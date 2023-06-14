package cn.wolfcode.mq.listener;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.IntegralRefundVo;
import cn.wolfcode.service.IUsableIntegralService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author xiaoliu
 * @date 2023/6/14
 */
@RocketMQMessageListener(
        consumerGroup = "integral_refund_consumer_group",
        topic = "integral_refund"
)
@Component
@Slf4j
public class IntegralRefundMessageListener implements RocketMQListener<IntegralRefundVo> {

    @Autowired
    private IUsableIntegralService usableIntegralService;

    @Override
    public void onMessage(IntegralRefundVo refundVo) {
        try {
            log.info("[积分退款] 收到积分退款消息：{}", JSON.toJSONString(refundVo));
            usableIntegralService.refund(refundVo);
        } catch (BusinessException e) {
            log.warn("[积分退款] 积分退款失败", e);
        }
    }
}
