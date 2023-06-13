package cn.wolfcode.service;

import cn.wolfcode.domain.IntegralRefundVo;
import cn.wolfcode.domain.OperateIntergralVo;


public interface IUsableIntegralService {
    /**
     * 积分支付接口
     *
     * @param vo 积分支付 vo
     * @return 支付流水号
     */
    String doPay(OperateIntergralVo vo);

    /**
     * 积分退款
     *
     * @param vo 积分退款 vo
     * @return 是否退款成功
     */
    boolean refund(IntegralRefundVo vo);
}
