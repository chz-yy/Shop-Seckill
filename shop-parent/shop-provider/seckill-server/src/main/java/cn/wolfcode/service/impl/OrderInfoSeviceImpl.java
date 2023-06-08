package cn.wolfcode.service.impl;

import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by wolfcode
 */
@Service
public class OrderInfoSeviceImpl implements IOrderInfoService {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PayLogMapper payLogMapper;
    @Autowired
    private RefundLogMapper refundLogMapper;

    @Override
    public OrderInfo selectByUserIdAndSeckillId(Long userId, Long seckillId, Integer time) {
        return orderInfoMapper.selectByUserIdAndSeckillId(userId, seckillId, time);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String doSeckill(UserInfo userInfo, SeckillProductVo vo) {
        // 再次判断库存是否足够
        // setIfAbsent == Redis 中的 SETNX 方法
        // SETNX：如果设置的 key 在 redis 中已经存在，就返回 false，如果不存在就返回 true
        // 如果当前线程执行结果为 true，我就认为它抢到了锁，如果为 false 我就认为争抢锁失败，直接提示网络繁忙稍后再试
        final String key = "seckill:product:lock:" + vo.getId();
        try {
            // 加锁
            // 设置了超时时间，避免获取到锁后，进程被关闭，无法释放锁导致死锁问题
            // TODO: Spring Data 所提供的 setIfAbsent(key, value, timeout, unit) 存在并发问题
            // TODO: 原因是该方法是通过先后调用 SET + EXPIRE 指令实现的如果 key 不存在就设置，并设置超时时间的功能
            // TODO: 因为是有多个指令组成的，此时如果其中一个指令执行成功，另一个失败则还是可能出现死锁问题
            // TODO: 真正通过 SETNX + EXPIRE 实现的方法需要通过管道命令 或 LUA 脚本实现批处理命令才可以避免加锁成功但是设置超时时间失败的问题
            Boolean ret = redisTemplate.opsForValue().setIfAbsent(key, "wolfcode", 5, TimeUnit.SECONDS);
            if (ret == null || !ret) {
                System.err.println(Thread.currentThread().getName() + "------------------加锁失败------------------");
                // 如果加锁失败，就抛出异常
                throw new BusinessException(SeckillCodeMsg.SECKILL_BUSY);
            }

            // 如果加锁成功，就扣减库存
            SeckillProduct sp = seckillProductService.findById(vo.getId());
            if (sp.getStockCount() <= 0) {
                // 库存不足
                throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
            }
            // 1. 扣除秒杀商品库存
            seckillProductService.decrStockCount(vo.getId(), vo.getTime());
        } finally {
            // 释放锁
            redisTemplate.delete(key);
        }
        // 2. 创建秒杀订单并保存
        OrderInfo orderInfo = this.buildOrderInfo(userInfo, vo);
        orderInfoMapper.insert(orderInfo);
        // 3. 返回订单编号
        return orderInfo.getOrderNo();
    }

    private OrderInfo buildOrderInfo(UserInfo userInfo, SeckillProductVo vo) {
        Date now = new Date();
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(now);
        orderInfo.setDeliveryAddrId(1L);
        orderInfo.setIntergral(vo.getIntergral());
        orderInfo.setOrderNo(IdGenerateUtil.get().nextId() + "");
        orderInfo.setPayType(OrderInfo.PAY_TYPE_ONLINE);
        orderInfo.setProductCount(1);
        orderInfo.setProductId(vo.getProductId());
        orderInfo.setProductImg(vo.getProductImg());
        orderInfo.setProductName(vo.getProductName());
        orderInfo.setProductPrice(vo.getProductPrice());
        orderInfo.setSeckillDate(now);
        orderInfo.setSeckillId(vo.getId());
        orderInfo.setSeckillPrice(vo.getSeckillPrice());
        orderInfo.setSeckillTime(vo.getTime());
        orderInfo.setStatus(OrderInfo.STATUS_ARREARAGE);
        orderInfo.setUserId(userInfo.getPhone());
        return orderInfo;
    }
}
