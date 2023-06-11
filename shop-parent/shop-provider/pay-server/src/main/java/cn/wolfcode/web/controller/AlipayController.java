package cn.wolfcode.web.controller;

import cn.wolfcode.common.web.CodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.config.AlipayProperties;
import cn.wolfcode.domain.PayVo;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/alipay")
public class AlipayController {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private AlipayProperties alipayProperties;

    @PostMapping("/doPay")
    public Result<String> doPay(@RequestBody PayVo vo) {
        try {
            AlipayTradePagePayRequest request = this.buildRequest(vo);
            //请求
            String body = alipayClient.pageExecute(request).getBody();
            // 请求到支付宝以后，预订单创建成功，会返回一个 HTML 片段，实现跳转到支付宝页面
            System.out.println(body);
            return Result.success(body);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(new CodeMsg(500401, e.getMessage()));
        }
    }

    @PostMapping("/checkRSASignature")
    public Result<Boolean> checkRSASignature(@RequestBody Map<String, String> params) throws AlipayApiException {
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayProperties.getAlipayPublicKey(), alipayProperties.getCharset(), alipayProperties.getSignType());
        return Result.success(signVerified);
    }

    private AlipayTradePagePayRequest buildRequest(PayVo vo) {
        // 设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(vo.getReturnUrl());
        alipayRequest.setNotifyUrl(vo.getNotifyUrl());

        JSONObject json = new JSONObject();
        json.put("out_trade_no", vo.getOutTradeNo());
        json.put("total_amount", vo.getTotalAmount());
        json.put("subject", vo.getSubject());
        json.put("body", vo.getBody());
        json.put("product_code", "FAST_INSTANT_TRADE_PAY");

        alipayRequest.setBizContent(json.toJSONString());
        return alipayRequest;
    }
}
