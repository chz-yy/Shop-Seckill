package cn.wolfcode.web.controller;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.service.IOrderInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/orderPay")
public class OrderPayController {
    @Autowired
    private IOrderInfoService orderInfoService;

    @GetMapping("/alipay")
    public Result<String> alipay(String orderNo) {
        return Result.success(orderInfoService.alipay(orderNo));
    }
}
