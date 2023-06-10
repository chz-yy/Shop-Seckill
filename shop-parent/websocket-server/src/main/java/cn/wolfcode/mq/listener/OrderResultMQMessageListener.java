package cn.wolfcode.mq.listener;

import cn.wolfcode.core.WebSocketServer;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMQResult;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaoliu
 * @date 2023/6/10
 */
@RocketMQMessageListener(
        consumerGroup = MQConstant.ORDER_RESULT_CONSUMER_GROUP,
        topic = MQConstant.ORDER_RESULT_TOPIC
)
@Component
@Slf4j
public class OrderResultMQMessageListener implements RocketMQListener<OrderMQResult> {

    @Override
    public void onMessage(OrderMQResult result) {
        log.info("[订单结果] 收到订单结果消息：{}", JSON.toJSONString(result));
        try {
            int count = 3;
            Session session = null;
            do {
                // 将收到的消息发送给客户端
                session = WebSocketServer.SESSION_MAP.get(result.getToken());
                if (session != null) {
                    session.getBasicRemote().sendText(JSON.toJSONString(result));
                    log.info("[订单结果] 消息成功通知到前端用户：{}", result.getToken());
                    break;
                }

                log.warn("[订单结果] 无法获取用户连接信息：{}，count：{}", result.getToken(), count);
                // 拿不到睡 500ms
                TimeUnit.MILLISECONDS.sleep(1000);
                count--;
            } while (count > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
