package cn.wolfcode.web.controller;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.IntegralRefundVo;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.service.IUsableIntegralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/intergral")
public class IntegralController {
    @Autowired
    private IUsableIntegralService usableIntegralService;

    @PostMapping("/pay")
    public Result<String> doPay(@RequestBody OperateIntergralVo vo) {
        String tradeNo = usableIntegralService.tryIncrIntegral(vo, null);
        return Result.success(tradeNo);
    }

    @PostMapping("/refund")
    public Result<Boolean> refund(@RequestBody IntegralRefundVo vo) {
        boolean ret = usableIntegralService.refund(vo);
        return Result.success(ret);
    }
}
