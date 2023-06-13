package cn.wolfcode.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.IntegralRefundVo;
import cn.wolfcode.domain.OperateIntergralVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("intergral-service")
public interface IntegralFeignApi {

    @PostMapping("/intergral/pay")
    Result<String> doPay(@RequestBody OperateIntergralVo vo);

    @PostMapping("/intergral/refund")
    Result<Boolean> refund(@RequestBody IntegralRefundVo refundVo);
}
