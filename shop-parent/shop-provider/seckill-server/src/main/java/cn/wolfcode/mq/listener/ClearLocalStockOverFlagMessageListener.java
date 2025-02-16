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

@RocketMQMessageListener(consumerGroup = MQConstant.CANCEL_SECKILL_OVER_SIGE_GROUP,
topic = MQConstant.CANCEL_SECKILL_OVER_SIGE_TOPIC,
messageModel = MessageModel.BROADCASTING)  //广播模式
@Component
@Slf4j
public class ClearLocalStockOverFlagMessageListener implements RocketMQListener<String> {


    @Override
    public void onMessage(String message) {
        log.info("收到清除库存标识信息:"+message);
        Long id = Long.parseLong(message);
        OrderInfoController.clearStockFlag(id);
    }
}
