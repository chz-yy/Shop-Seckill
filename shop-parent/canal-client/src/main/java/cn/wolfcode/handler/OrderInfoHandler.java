package cn.wolfcode.handler;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.redis.SeckillRedisKey;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

@Slf4j
@Component
@CanalTable(value = "t_order_info")
public class OrderInfoHandler implements EntryHandler<OrderInfo> {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void insert(OrderInfo orderInfo) {
        log.info("[订单新增] 收到订单新增数据：{}", JSON.toJSONString(orderInfo));
        // 将订单存入 redis
        String orderKey = SeckillRedisKey.SECKILL_ORDER_HASH.join(orderInfo.getUserId() + "");
        redisTemplate.opsForHash().put(orderKey, orderInfo.getOrderNo(), JSON.toJSONString(orderInfo));
    }

    @Override
    public void update(OrderInfo before, OrderInfo after) {
        log.info("[订单新增] 收到订单修改数据-修改后：{}", JSON.toJSONString(after));
        // 覆盖 redis 中的订单
        String orderKey = SeckillRedisKey.SECKILL_ORDER_HASH.join(after.getUserId() + "");
        redisTemplate.opsForHash().put(orderKey, after.getOrderNo(), JSON.toJSONString(after));
    }

    @Override
    public void delete(OrderInfo orderInfo) {
        log.info("[订单数据] 订单：{} 被删除了...", orderInfo.getOrderNo());
        // 删除 redis 中的订单
        String orderKey = SeckillRedisKey.SECKILL_ORDER_HASH.join(orderInfo.getUserId() + "");
        redisTemplate.opsForHash().delete(orderKey, orderInfo.getOrderNo());
    }
}