package cn.wolfcode.web.controller;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.feign.AlipayFeignApi;
import cn.wolfcode.service.IOrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


@RestController
@RequestMapping("/orderPay")
@RefreshScope
@Slf4j
public class OrderPayController {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private AlipayFeignApi alipayFeignApi;
    @Value("${pay.frontEndPayUrl}")
    private String frontEndPayUrl;

    @GetMapping("/alipay")
    public Result<String> alipay(String orderNo) {
        return Result.success(orderInfoService.alipay(orderNo));
    }

    @GetMapping("/return_url")
    public void returnUrl(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        // 获取支付宝GET过来反馈信息
        Map<String, String> params = new HashMap<String, String>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            // 乱码解决，这段代码在出现乱码时使用
            // valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }

        log.info("[订单支付] 收到同步回调请求，请求参数：{}", params);

        // 远程调用支付服务进行签名验证
        Result<Boolean> result = alipayFeignApi.checkRSASignature(params);
        if (result == null || result.hasError() || !result.getData()) {
            // 重定向到签名失败页面
            resp.getWriter().write("<h1>支付宝同步回调签名验证失败</h1>");
            return;
        }

        // 从传回来的参数中获取订单编号
        String orderNo = request.getParameter("out_trade_no");

        // 如果签名验证通过，重定向到订单详情页
        resp.sendRedirect(frontEndPayUrl + orderNo);
    }

    @PostMapping("/notify_url")
    public String notifyUrl(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        // 获取支付宝POST过来反馈信息
        Map<String, String> params = new HashMap<String, String>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            // 乱码解决，这段代码在出现乱码时使用
            // valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }

        log.info("[订单支付] 收到异步回调请求，请求参数：{}", params);

        // 远程调用支付服务进行签名验证
        Result<Boolean> result = alipayFeignApi.checkRSASignature(params);
        if (result == null || result.hasError() || !result.getData()) {
            return "error";
        }

        //商户订单号
        String orderNo = request.getParameter("out_trade_no");
        //支付宝交易号
        String tradeNo = request.getParameter("trade_no");
        //交易状态
        String trade_status = request.getParameter("trade_status");
        //支付金额
        String totalAmount = request.getParameter("total_amount");

        if ("TRADE_FINISHED".equals(trade_status)) {
            //判断该笔订单是否在商户网站中已经做过处理
            //如果没有做过处理，根据订单号（out_trade_no）在商户网站的订单系统中查到该笔订单的详细，并执行商户的业务程序
            //如果有做过处理，不执行商户的业务程序

            log.info("[订单支付] 订单已过退款时间，标记订单为完成状态，不可再退款");
            //注意：
            //退款日期超过可退款期限后（如三个月可退款），支付宝系统发送该交易状态通知
        } else if ("TRADE_SUCCESS".equals(trade_status)) {
            //判断该笔订单是否在商户网站中已经做过处理
            //如果没有做过处理，根据订单号（out_trade_no）在商户网站的订单系统中查到该笔订单的详细，并执行商户的业务程序
            //如果有做过处理，不执行商户的业务程序

            //注意：
            //付款完成后，支付宝系统发送该交易状态通知
            orderInfoService.alipaySuccess(orderNo, tradeNo, totalAmount);
        }

        return "success";
    }
}
