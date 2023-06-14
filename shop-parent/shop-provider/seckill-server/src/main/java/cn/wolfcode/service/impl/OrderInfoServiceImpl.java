package cn.wolfcode.service.impl;

import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.*;
import cn.wolfcode.feign.AlipayFeignApi;
import cn.wolfcode.feign.IntegralFeignApi;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderTimeoutMessage;
import cn.wolfcode.mq.callback.DefaultMQMessageCallback;
import cn.wolfcode.redis.CommonRedisKey;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;

/**
 * Created by wolfcode
 * RefreshScope: Nacos Config 的动态刷新配置
 */
@RefreshScope
@Service
@Slf4j
public class OrderInfoServiceImpl implements IOrderInfoService {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private PayLogMapper payLogMapper;
    @Autowired
    private RefundLogMapper refundLogMapper;
    @Autowired
    private AlipayFeignApi alipayFeignApi;
    @Autowired
    private IntegralFeignApi integralFeignApi;

    @Value("${pay.returnUrl}")
    private String returnUrl;
    @Value("${pay.notifyUrl}")
    private String notifyUrl;

    @Override
    public OrderInfo selectByUserIdAndSeckillId(Long userId, Long seckillId, Integer time) {
        return orderInfoMapper.selectByUserIdAndSeckillId(userId, seckillId, time);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String doSeckill(UserInfo userInfo, SeckillProductVo vo) {
        // 1. 扣除秒杀商品库存
        int row = seckillProductService.decrStockCount(vo.getId(), vo.getTime());
        if (row <= 0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        // 2. 创建秒杀订单并保存
        OrderInfo orderInfo = this.buildOrderInfo(userInfo, vo);
        orderInfoMapper.insert(orderInfo);
        // 3. 返回订单编号
        return orderInfo.getOrderNo();
    }

    @Override
    public OrderInfo findByOrderNo(String orderNo) {
        return orderInfoMapper.find(orderNo);
    }

    @Transactional
    @Override
    public void checkPyTimeout(OrderTimeoutMessage message) {
        // 1. 根据订单编号查询订单是否已支付，如果支付则直接结束
        // 2. 如果未支付，更新订单状态为超时取消
        int row = orderInfoMapper.updateCancelStatus(message.getOrderNo(), OrderInfo.STATUS_TIMEOUT);
        if (row > 0) {
            // 3. MySQL 库存+1
            seckillProductService.incryStockCount(message.getSeckillId());

            this.syncStock(message.getSeckillId(), message.getUserPhone());
        }
    }

    @Override
    public void syncStock(Long seckillId, Long userPhone) {
        // 1. 删除用户重复下单标识
        redisTemplate.opsForHash().delete(SeckillRedisKey.SECKILL_ORDER_HASH.join(seckillId + ""), userPhone + "");

        // 2. 还原库存
        SeckillProduct sp = seckillProductService.findById(seckillId);
        String key = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.join(sp.getTime() + "");
        redisTemplate.opsForHash().put(key, sp.getId() + "", sp.getStockCount() + "");

        // 3. 清除本地标识
        rocketMQTemplate.asyncSend(MQConstant.CANCEL_SECKILL_OVER_SIGE_TOPIC, seckillId, new DefaultMQMessageCallback());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String alipay(String orderNo, String token) {
        // 1. 查询订单信息
        OrderInfo orderInfo = this.findByOrderNo(orderNo);
        // 保证幂等性，同一个订单不能支付两次
        if (!orderInfo.getStatus().equals(OrderInfo.STATUS_ARREARAGE)) {
            throw new BusinessException(SeckillCodeMsg.ORDER_STATUS_ERROR);
        }

        // 检查是否是当前用户在发起支付
        this.checkOrderUser(token, orderInfo);

        // 2. 构建支付 vo 对象，远程调用支付服务发起支付
        PayVo payVo = this.buildPayVo(orderInfo);
        Result<String> result = alipayFeignApi.doPay(payVo);
        // 3. 判断是否发起支付成功
        if (result == null || result.hasError()) {
            throw new BusinessException(SeckillCodeMsg.PAY_SERVER_ERROR);
        }
        // 6. 返回结果
        return result.getData();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void alipaySuccess(String orderNo, String tradeNo, String totalAmount) {
        // 1. 基于订单 id 查询订单对象，判断订单是否存在
        OrderInfo orderInfo = this.findByOrderNo(orderNo);
        if (orderInfo == null) {
            throw new BusinessException(SeckillCodeMsg.REMOTE_DATA_ERROR);
        }
        // 2. 判断订单状态是否正确
        if (!orderInfo.getStatus().equals(OrderInfo.STATUS_ARREARAGE)) {
            throw new BusinessException(SeckillCodeMsg.ORDER_STATUS_ERROR);
        }
        // 3. 判断订单支付金额是否正确
        if (!orderInfo.getSeckillPrice().equals(new BigDecimal(totalAmount))) {
            throw new BusinessException(SeckillCodeMsg.PAY_AMOUNT_ERROR);
        }
        try {
            // 4. 构建支付日志对象，保存支付日志
            PayLog log = this.buildPayLog(tradeNo, orderInfo, PayLog.PAY_TYPE_ONLINE);
            payLogMapper.insert(log);

            // 5. 更新订单支付成功状态
            int row = orderInfoMapper.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAY_TYPE_ONLINE);
            if (row == 0) {
                throw new BusinessException(SeckillCodeMsg.ORDER_STATUS_ERROR);
            }
        } catch (SQLException e) {
            throw new BusinessException(SeckillCodeMsg.REPEAT_PAY_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void refund(String orderNo, String token) {
        log.info("[退款请求] 收到退款请求 orderNo={}, token={}", orderNo, token);
        // 1. 基于订单编号查询订单对象，判断对象是否存在
        OrderInfo orderInfo = this.findByOrderNo(orderNo);
        if (orderInfo == null) {
            throw new BusinessException(SeckillCodeMsg.REMOTE_DATA_ERROR);
        }

        // 判断用户是否是订单用户
        this.checkOrderUser(token, orderInfo);

        // 2. 判断订单状态是否为已支付
        if (!OrderInfo.STATUS_ACCOUNT_PAID.equals(orderInfo.getStatus())) {
            throw new BusinessException(SeckillCodeMsg.ORDER_STATUS_ERROR);
        }

        // 3. 封装退款 vo 对象
        if (orderInfo.getPayType() == OrderInfo.PAY_TYPE_ONLINE) {
            RefundVo vo = this.buildRefundVo(orderInfo);
            // 4. 远程调用支付服务，发起退款操作
            Result<Boolean> result = alipayFeignApi.refund(vo);
            // 5. 判断退款结果是否成功
            if (result == null || result.hasError() || !result.getData()) {
                log.warn("[退款操作] 支付宝退款失败：{}", JSON.toJSONString(result));
                throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
            }

            // 改变订单状态
            this.changeRefundStatus(orderInfo, vo.getRefundReason());
            return;
        }

        IntegralRefundVo refundVo = new IntegralRefundVo(orderInfo.getUserId(), orderNo, orderInfo.getIntergral(),
                "积分退款：" + orderInfo.getProductName());

        log.info("[退款请求] 进行积分退款操作，发送事务消息：{}", JSON.toJSONString(refundVo));
        // 利用 RocketMQ 发送事务消息
        // 构建消息对象
        Message<IntegralRefundVo> message = MessageBuilder.withPayload(refundVo).setHeader("orderNo", orderNo).build();
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction("integral_refund_tx_group",
                "integral_refund", message, orderNo);
        log.info("[退款请求] 发送事务消息结果：SendState={}, txState={}", result.getSendStatus(), result.getLocalTransactionState());

        // 如果本地事务执行状态返回了 ROLLBACK，就认为退款失败
        if (result.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
    }

    @Override
    public void changeRefundStatus(OrderInfo orderInfo, String reason) {
        // int i = 1 / 0;
        // 6. 如果退款成功，修改订单状态
        int row = orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(), OrderInfo.STATUS_REFUND);
        if (row == 0) {
            throw new BusinessException(SeckillCodeMsg.ORDER_STATUS_ERROR);
        }
        // 7. 记录退款日志
        RefundLog log = this.buildRefundLog(orderInfo, reason);
        refundLogMapper.insert(log);
    }

    @GlobalTransactional(rollbackFor = Exception.class)
    @Override
    public void integralPay(String orderNo, String token) {
        // 1. 先基于订单编号查询订单对象，判断订单是否存在
        OrderInfo orderInfo = this.findByOrderNo(orderNo);
        if (orderInfo == null) {
            throw new BusinessException(SeckillCodeMsg.REMOTE_DATA_ERROR);
        }

        // 检查是否是当前用户在发起支付
        this.checkOrderUser(token, orderInfo);

        // 2. 判断订单状态是否为未支付
        if (!OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            throw new BusinessException(SeckillCodeMsg.ORDER_STATUS_ERROR);
        }
        // 3. 封装请求积分支付 vo 对象
        OperateIntergralVo vo = this.buildIntegralVo(orderInfo);
        // 4. 远程请求积分支付接口
        Result<String> result = integralFeignApi.doPay(vo);
        // 5. 判断远程是否支付成功
        if (result.hasError()) {
            throw new BusinessException(new CodeMsg(result.getCode(), result.getMsg()));
        }
        // 6. 更新订单状态为支付成功
        int row = orderInfoMapper.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAY_TYPE_INTERGRAL);
        if (row == 0) {
            throw new BusinessException(SeckillCodeMsg.ORDER_STATUS_ERROR);
        }
        // 7. 记录支付日志
        try {
            PayLog log = this.buildPayLog(result.getData(), orderInfo, PayLog.PAY_TYPE_INTERGRAL);
            payLogMapper.insert(log);
        } catch (SQLException e) {
            throw new BusinessException(SeckillCodeMsg.REPEAT_PAY_ERROR);
        }
    }

    private void checkOrderUser(String token, OrderInfo orderInfo) {
        // 判断当前用户是否是创建该订单的用户
        UserInfo userInfo = getUserByToken(token);
        if (userInfo == null) {
            throw new BusinessException(new CodeMsg(500103, "用户未认证"));
        }
        if (!userInfo.getPhone().equals(orderInfo.getUserId())) {
            throw new BusinessException(SeckillCodeMsg.ILLEGAL_OPERATION);
        }
    }

    private OperateIntergralVo buildIntegralVo(OrderInfo orderInfo) {
        OperateIntergralVo vo = new OperateIntergralVo();
        vo.setInfo("积分支付：" + orderInfo.getProductName());
        vo.setPk(orderInfo.getOrderNo());
        vo.setValue(orderInfo.getIntergral());
        vo.setUserId(orderInfo.getUserId());
        return vo;
    }

    private RefundLog buildRefundLog(OrderInfo orderInfo, String reason) {
        RefundLog log = new RefundLog();
        log.setOutTradeNo(orderInfo.getOrderNo());
        if (orderInfo.getPayType() == OrderInfo.PAY_TYPE_INTERGRAL) {
            log.setRefundAmount(orderInfo.getIntergral().toString());
        } else {
            log.setRefundAmount(orderInfo.getSeckillPrice().toString());
        }
        log.setRefundReason(reason);
        log.setRefundTime(new Date());
        log.setRefundType(orderInfo.getPayType());
        return log;
    }

    private RefundVo buildRefundVo(OrderInfo orderInfo) {
        RefundVo vo = new RefundVo();
        vo.setOutTradeNo(orderInfo.getOrderNo());
        vo.setRefundAmount(orderInfo.getSeckillPrice().toString());
        String type = OrderInfo.PAY_TYPE_ONLINE == orderInfo.getPayType() ? "支付宝" : "积分";
        vo.setRefundReason(type + "退款");
        return vo;
    }

    private PayLog buildPayLog(String tradeNo, OrderInfo orderInfo, int payType) {
        PayLog log = new PayLog();
        log.setTradeNo(tradeNo);
        log.setOrderNo(orderInfo.getOrderNo());
        log.setPayType(payType);
        log.setStatus(PayLog.PAY_STATUS_SUCCESS);
        log.setNotifyTime(System.currentTimeMillis());
        if (payType == PayLog.PAY_TYPE_ONLINE) {
            log.setTotalAmount(orderInfo.getSeckillPrice().toString());
        } else {
            log.setTotalAmount(orderInfo.getIntergral() + "");
        }
        return log;
    }

    private PayVo buildPayVo(OrderInfo orderInfo) {
        PayVo vo = new PayVo();
        vo.setOutTradeNo(orderInfo.getOrderNo());
        vo.setSubject("叩丁狼-限时抢购");
        vo.setBody(orderInfo.getProductName());
        vo.setTotalAmount(orderInfo.getSeckillPrice().toString());
        vo.setReturnUrl(returnUrl);
        vo.setNotifyUrl(notifyUrl);
        return vo;
    }

    private OrderInfo buildOrderInfo(UserInfo userInfo, SeckillProductVo vo) {
        Date now = new Date();
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(now);
        orderInfo.setDeliveryAddrId(1L);
        orderInfo.setIntergral(vo.getIntergral());
        orderInfo.setOrderNo(IdGenerateUtil.get().nextId() + "");
        orderInfo.setPayType(OrderInfo.PAY_TYPE_ONLINE);
        orderInfo.setProductCount(1);
        orderInfo.setProductId(vo.getProductId());
        orderInfo.setProductImg(vo.getProductImg());
        orderInfo.setProductName(vo.getProductName());
        orderInfo.setProductPrice(vo.getProductPrice());
        orderInfo.setSeckillDate(now);
        orderInfo.setSeckillId(vo.getId());
        orderInfo.setSeckillPrice(vo.getSeckillPrice());
        orderInfo.setSeckillTime(vo.getTime());
        orderInfo.setStatus(OrderInfo.STATUS_ARREARAGE);
        orderInfo.setUserId(userInfo.getPhone());
        return orderInfo;
    }

    private UserInfo getUserByToken(String token) {
        return JSON.parseObject(redisTemplate.opsForValue().get(CommonRedisKey.USER_TOKEN.getRealKey(token)), UserInfo.class);
    }
}
