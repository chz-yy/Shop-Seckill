package cn.wolfcode.mq.listener;

import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMQResult;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.mq.OrderTimeoutMessage;
import cn.wolfcode.mq.callback.DefaultMQMessageCallback;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
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
@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = MQConstant.ORDER_PENDING_CONSUMER_GROUP,
        topic = MQConstant.ORDER_PENDING_TOPIC
)
public class OrderPendingMessageListener implements RocketMQListener<OrderMessage> {

    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(OrderMessage orderMessage) {
        log.info("[创建订单消息] 收到创建订单消息，准备开始创建订单 {}", JSON.toJSONString(orderMessage));

        OrderMQResult result = new OrderMQResult();
        try {
            result.setTime(orderMessage.getTime());
            result.setSeckillId(orderMessage.getSeckillId());
            result.setToken(orderMessage.getToken());

            // 调用秒杀接口进行秒杀
            UserInfo userInfo = new UserInfo();
            userInfo.setPhone(orderMessage.getUserPhone());
            String orderNo = orderInfoService.doSeckill(userInfo, seckillProductService.selectByIdAndTime(orderMessage.getSeckillId(), orderMessage.getTime()));

            result.setOrderNo(orderNo);
            // 创建订单成功，发送延迟消息检查超时未支付订单
            // 延迟等级：1s、5s、10s、30s、1m、2m、3m、4m、5m、6m、7m、8m、9m、10m、20m、30m、1h、2h
            Message<OrderTimeoutMessage> message = MessageBuilder.withPayload(new OrderTimeoutMessage(orderNo, orderMessage.getSeckillId())).build();
            rocketMQTemplate.asyncSend(MQConstant.ORDER_PAY_TIMEOUT_TOPIC, message, new DefaultMQMessageCallback(), 2000, MQConstant.ORDER_PAY_TIMEOUT_DELAY_LEVEL);
        } catch (Exception e) {
            e.printStackTrace();
            // 订单创建失败
            SeckillCodeMsg codeMsg = SeckillCodeMsg.SECKILL_ERROR;
            result.setCode(codeMsg.getCode());
            result.setMsg(codeMsg.getMsg());
        }

        // 发送订单创建结果消息
        rocketMQTemplate.asyncSend(MQConstant.ORDER_RESULT_TOPIC, result, new DefaultMQMessageCallback());
    }
}
