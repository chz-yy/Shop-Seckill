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

    /**
     * 发起支付宝支付接口
     *
     * @param orderNo 订单编号
     * @return 重定向到支付宝的 HTML 脚本
     */
    String alipay(String orderNo);
}
