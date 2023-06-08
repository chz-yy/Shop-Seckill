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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
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
    private RedissonClient redisson1;
    @Autowired
    private RedissonClient redisson2;
    @Autowired
    private RedissonClient redisson3;
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
        final String key = "seckill:product:lock:" + vo.getId();
        // 获取锁
        RLock lock1 = redisson1.getLock(key);
        RLock lock2 = redisson2.getLock(key);
        RLock lock3 = redisson3.getLock(key);
        RLock redLock = redisson1.getRedLock(lock1, lock2, lock3);
        try {
            // 加锁
            redLock.lock(5, TimeUnit.SECONDS);
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
            redLock.unlock();
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
