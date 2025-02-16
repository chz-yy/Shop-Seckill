package cn.wolfcode.mq.listener;

import cn.wolfcode.SeckillApplication;
import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMQResult;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.mq.OrderTimeoutMessage;
import cn.wolfcode.mq.callback.DefaultMQMessageCallback;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.service.impl.OrderInfoServiceImpl;
import cn.wolfcode.service.impl.SeckillProductServiceImpl;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @author xiaoliu
 * @date 2023/6/10
 */
@RocketMQMessageListener(consumerGroup = MQConstant.ORDER_PENDING_CONSUMER_GROUP,topic = MQConstant.ORDER_PENDING_TOPIC) //消费者分组和监听的topic
@Component
@Slf4j
public class OrderPendingMessageListener implements RocketMQListener<OrderMessage> {
    @Autowired
    OrderInfoServiceImpl orderInfoService;
    @Autowired
    SeckillProductServiceImpl seckillProductService;
    @Autowired
    RocketMQTemplate rocketMQTemplate;
    @Override
    public void onMessage(OrderMessage message) {
        log.info("创建订单");
        OrderMQResult result=new OrderMQResult();
        result.setTime(message.getTime());
        result.setToken(message.getToken());
        result.setSeckillId(message.getSeckillId());
        try{
            String orderNo = orderInfoService.doSeckill(message.getUserPhone(), message.getSeckillId(), message.getTime());
            result.setOrderNo(orderNo);
            result.setCode(Result.SUCCESS_CODE);
            result.setMsg("下单成功");
            //订单超时消息
            OrderTimeoutMessage orderTimeoutMessage=new OrderTimeoutMessage();
            orderTimeoutMessage.setOrderNo(orderNo);
            orderTimeoutMessage.setSeckillId(message.getSeckillId());
            orderTimeoutMessage.setUserPhone(message.getUserPhone());
            orderTimeoutMessage.setTime(message.getTime());
            //build消息
            Message<OrderTimeoutMessage> build = MessageBuilder.withPayload(orderTimeoutMessage).build();
            rocketMQTemplate.asyncSend(MQConstant.ORDER_PAY_TIMEOUT_TOPIC,build ,new DefaultMQMessageCallback(),2000,3);
        }catch (Exception e){
            result.setCode(SeckillCodeMsg.SECKILL_ERROR.getCode());
            result.setMsg(SeckillCodeMsg.SECKILL_ERROR.getMsg());
            seckillProductService.failedRollback(message.getSeckillId(),message.getTime(),message.getUserPhone());
        }
        rocketMQTemplate.asyncSend(MQConstant.ORDER_RESULT_TOPIC,result,new DefaultMQMessageCallback());
    }
}
