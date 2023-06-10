package cn.wolfcode.mq.callback;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;

/**
 * @author xiaoliu
 * @date 2023/6/10
 */
@Slf4j
public class DefaultMQMessageCallback implements SendCallback {

    @Override
    public void onSuccess(SendResult result) {
        log.info("[异步消息回调] 发送消息成功：{}", result);
    }

    @Override
    public void onException(Throwable throwable) {
        log.error("[异步消息回调] 消息发送失败，准备重新投递消息", throwable);
    }
}
