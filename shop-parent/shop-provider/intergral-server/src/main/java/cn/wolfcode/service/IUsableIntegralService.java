package cn.wolfcode.service;

import cn.wolfcode.domain.OperateIntergralVo;


public interface IUsableIntegralService {
    /**
     * 积分支付接口
     *
     * @param vo 积分支付 vo
     * @return 支付流水号
     */
    String doPay(OperateIntergralVo vo);
}
