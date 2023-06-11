package cn.wolfcode.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.PayVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("pay-service")
public interface AlipayFeignApi {

    @PostMapping("/alipay/doPay")
    Result<String> doPay(@RequestBody PayVo vo);
}
