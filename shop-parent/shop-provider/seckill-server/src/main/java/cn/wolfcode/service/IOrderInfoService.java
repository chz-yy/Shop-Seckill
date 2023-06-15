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

    OrderInfo findByOrderNo(String orderNo, Long userId);

    void checkPyTimeout(OrderTimeoutMessage message);

    void syncStock(Long seckillId, Long userPhone);

    /**
     * 发起支付宝支付接口
     *
     * @param orderNo 订单编号
     * @param token
     * @return 重定向到支付宝的 HTML 脚本
     */
    String alipay(String orderNo, String token);

    /**
     * 支付宝异步回调支付成功
     *
     * @param orderNo     订单 id
     * @param tradeNo     支付宝交易流水号
     * @param totalAmount 支付金额
     */
    void alipaySuccess(String orderNo, String tradeNo, String totalAmount);

    void refund(String orderNo, String token);

    void changeRefundStatus(OrderInfo orderInfo, String reason);

    void integralPay(String orderNo, String token);
}
