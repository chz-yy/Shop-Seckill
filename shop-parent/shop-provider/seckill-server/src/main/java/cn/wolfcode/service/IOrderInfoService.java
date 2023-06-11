package cn.wolfcode.service;


import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mq.OrderTimeoutMessage;

/**
 * Created by wolfcode
 */
public interface IOrderInfoService {

    OrderInfo selectByUserIdAndSeckillId(Long phone, Long seckillId, Integer time);

    String doSeckill(UserInfo userInfo, SeckillProductVo vo);

    OrderInfo findByOrderNo(String orderNo);

    void checkPyTimeout(OrderTimeoutMessage message);

    void syncStock(Long seckillId, Long userPhone);
}
