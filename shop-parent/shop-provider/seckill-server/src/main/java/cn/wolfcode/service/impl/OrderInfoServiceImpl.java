package cn.wolfcode.service.impl;

import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderTimeoutMessage;
import cn.wolfcode.mq.callback.DefaultMQMessageCallback;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by wolfcode
 */
@Service
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
}
