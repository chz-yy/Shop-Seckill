package cn.wolfcode.mq.listener;

import cn.wolfcode.domain.IntegralRefundVo;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.service.IOrderInfoService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * @author xiaoliu
 * @date 2023/6/14
 */
@RocketMQTransactionListener(txProducerGroup = "integral_refund_tx_group")
@Component
@Slf4j
public class IntegralRefundTransactionMessageListener implements RocketMQLocalTransactionListener {

    @Autowired
    private IOrderInfoService orderInfoService;

    /**
     * 执行本地事务
     *
     * @param message 消息对象
     * @param arg     外部发送事务消息时的最后一个参数
     * @return 本地事务执行状态
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object arg) {
        try {
            log.info("[积分退款事务消息] 准备执行本地事务，修改订单状态：{}", arg);
            OrderInfo orderInfo = orderInfoService.findByOrderNo((String) arg);
            orderInfoService.changeRefundStatus(orderInfo, "积分支付事务消息退款");
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            log.info("[积分退款事务消息] 执行本地事务失败，回滚事务......");
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    /**
     * 检查本地事务状态，当执行本地事务没有返回 commit/rollback 的时候会执行本方法
     *
     * @param message 消息对象
     * @return 本地事务执行状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        log.info("[积分退款事务消息] 检查本地事务状态：{}", JSON.toJSONString(message));
        // 检查订单状态是否改成已退款
        String orderNo = (String) message.getHeaders().get("orderNo");
        OrderInfo orderInfo = orderInfoService.findByOrderNo(orderNo);
        if (OrderInfo.STATUS_REFUND.equals(orderInfo.getStatus())) {
            log.info("[积分退款事务消息] 已经退款成功，准备提交事务 orderNo={}, status={}", orderInfo.getOrderNo(), orderInfo.getStatus());
            return RocketMQLocalTransactionState.COMMIT;
        }

        log.info("[积分退款事务消息] 检查本地事务执行失败：{}", JSON.toJSONString(orderInfo));
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
