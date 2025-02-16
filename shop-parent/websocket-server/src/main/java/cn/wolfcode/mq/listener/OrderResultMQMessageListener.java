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
import java.io.IOException;
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
    public void onMessage(OrderMQResult message) {
        String result = JSON.toJSONString(message);
        log.info("收到订单信息，订单结果信息："+result);
        try{
            int count=0;
            do{
                Session session = WebSocketServer.SESSION_MAP.get(message.getToken());
                if(session!=null){
                    session.getBasicRemote().sendText(result);
                    log.info("session连接建立，发送消息成功");
                    return;
                }
                count++;
                Thread.sleep(400);
            }while (count<4);
        }catch (Exception e){
            log.info(e.toString());
        }
    }
}
