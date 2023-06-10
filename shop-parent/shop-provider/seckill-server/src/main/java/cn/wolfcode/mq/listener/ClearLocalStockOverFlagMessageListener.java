package cn.wolfcode.mq.listener;

import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.web.controller.OrderInfoController;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author xiaoliu
 * @date 2023/6/10
 */
@RocketMQMessageListener(
        consumerGroup = MQConstant.CANCEL_SECKILL_OVER_SIGE_GROUP,
        topic = MQConstant.CANCEL_SECKILL_OVER_SIGE_TOPIC,
        /* 消息模式修改为广播消息 */
        messageModel = MessageModel.BROADCASTING
)
@Component
@Slf4j
public class ClearLocalStockOverFlagMessageListener implements RocketMQListener<Long> {

    @Override
    public void onMessage(Long seckillId) {
        log.info("[清除本地标识] 收到清除本地标识请求，清除本地售完标识：{}", seckillId);
        OrderInfoController.LOCAL_STOCK_COUNT_FLAG_CACHE.put(seckillId, false);
    }
}
