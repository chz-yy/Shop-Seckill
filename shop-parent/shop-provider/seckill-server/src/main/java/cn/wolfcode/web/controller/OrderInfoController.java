package cn.wolfcode.web.controller;

import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.common.web.resolver.RequestUser;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.common.util.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/order")
@Slf4j
public class OrderInfoController {

    /**
     * 本地库存售完标记缓存
     * key=秒杀id
     * value=是否已经售完
     */
    public static final Map<Long, Boolean> LOCAL_STOCK_COUNT_FLAG_CACHE = new ConcurrentHashMap<>();

    @Autowired
    private ISeckillProductService seckillProductService;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private IOrderInfoService orderInfoService;

    /**
     * 优化前：
     * 测试数据：500 个用户，100 线程，执行 50 次
     * 测试情况：330 QPS
     * 优化后：
     * 测试数据：500 个用户，100 线程，执行 50 次
     * 测试情况：850 QPS
     */
    @RequireLogin
    @PostMapping("/doSeckill")
    public Result<?> doSeckill(long seckillId, Integer time, @RequestUser UserInfo userInfo){
        SeckillProductVo sp = seckillProductService.selectByIdAndTime(seckillId, time);
        //商品信息是否存在
        AssertUtils.notNull(sp,"非法操作");
        //判断是否在秒杀时间内
        boolean range=this.betweenSeckillTime(sp);
        AssertUtils.isTrue(range,"不在时间范围内");
        //判断库存是否充足
        AssertUtils.isTrue(sp.getStockCount()>0,"库存不足");
        //用户是否下过单
        OrderInfo orderInfo = orderInfoService.selectByUserIdAndSeckillId(userInfo.getPhone(), seckillId, time);
        AssertUtils.isTrue(orderInfo==null,"不能重复下单");
        //下单
        String orderNo=orderInfoService.doSeckill(userInfo.getPhone(),sp);
        return Result.success(orderNo);
    }

    private boolean betweenSeckillTime(SeckillProductVo sp) {
        Calendar instance=Calendar.getInstance();
        instance.setTime(sp.getStartDate());
        //设置小时
        instance.set(Calendar.HOUR_OF_DAY,sp.getTime());
        //设置开始时间
        Date startDate=instance.getTime();
        instance.add(Calendar.HOUR_OF_DAY,2);
        Date endTime=instance.getTime();
        long now=System.currentTimeMillis();
        return startDate.getTime()<=now&&endTime.getTime()>now;
    }


    @RequireLogin
    @GetMapping("/find")
    public Result<OrderInfo> findById(String orderNo, @RequestUser UserInfo userInfo) {
        OrderInfo orderInfo = orderInfoService.findByOrderNo(orderNo, userInfo.getPhone());
        return Result.success(orderInfo);
    }


}
