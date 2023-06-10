package cn.wolfcode.mq.listener;

import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderTimeoutMessage;
import cn.wolfcode.service.IOrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author xiaoliu
 * @date 2023/6/10
 */
@RocketMQMessageListener(
        consumerGroup = MQConstant.ORDER_PAY_TIMEOUT_GROUP,
        topic = MQConstant.ORDER_PAY_TIMEOUT_TOPIC
)
@Component
@Slf4j
public class OrderPayTimeoutCheckMessageListener implements RocketMQListener<OrderTimeoutMessage> {

    @Autowired
    private IOrderInfoService orderInfoService;

    @Override
    public void onMessage(OrderTimeoutMessage message) {
        log.info("[超时未支付检查] 收到超时未支付检查消息：{}", message);

        // 超时未支付检查
        orderInfoService.checkPyTimeout(message);
    }
}
