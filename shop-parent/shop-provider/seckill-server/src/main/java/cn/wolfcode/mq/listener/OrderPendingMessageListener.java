package cn.wolfcode.mq.listener;

import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Override
    public void onMessage(OrderMessage orderMessage) {
        log.info("[创建订单消息] 收到创建订单消息，准备开始创建订单 {}", JSON.toJSONString(orderMessage));

        // 调用秒杀接口进行秒杀
        UserInfo userInfo = new UserInfo();
        userInfo.setPhone(orderMessage.getUserPhone());
        orderInfoService.doSeckill(userInfo, seckillProductService.selectByIdAndTime(orderMessage.getSeckillId(), orderMessage.getTime()));
    }
}
