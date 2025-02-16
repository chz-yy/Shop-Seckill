package cn.wolfcode.web.controller;

import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.common.web.resolver.RequestUser;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.mq.callback.DefaultMQMessageCallback;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.common.util.AssertUtils;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.fasterxml.jackson.databind.jsontype.impl.AsExistingPropertyTypeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/order")
@Slf4j
public class OrderInfoController {



    @Autowired
    private ISeckillProductService seckillProductService;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private IOrderInfoService orderInfoService;
    /**
     * 本地库存售完标记缓存
     * key=秒杀id
     * value=是否已经售完
     */
    private static final ConcurrentHashMap<Long,Boolean> overMap=new ConcurrentHashMap<>();

    public static void clearStockFlag(Long key){
        overMap.put(key,false);
    }

    /**
     * 优化前：
     * 测试数据：500 个用户，100 线程，执行 500 次  i5-1135G7
     * 测试情况：230 QPS
     * 方案一：jvm
     * 1.doSeckill方法加锁  73 QPS 平均响应时间 1.223 s
     * 2.扣减库存前查询库存，判断库存是否够，加锁 135QPS 平均响应时间 0.658 s
     * 方案二：分布式锁
     *  自旋锁 485QPS 0.17 s 刚开始660qps,运行时间越长，越低
     *  乐观锁+预存库存+jvm缓存，165qps 10g堆内存 187qps  15g 200qps
     */
    @RequireLogin
    @PostMapping("/doSeckill")
    public Result<?> doSeckill(long seckillId, Integer time, @RequestUser UserInfo userInfo,@RequestHeader("token") String token){
        Boolean over = overMap.get(seckillId);
        if(over!=null&&over){
            return Result.error(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        SeckillProductVo sp = seckillProductService.selectByIdAndTime(seckillId, time);
        //商品信息是否存在
//        AssertUtils.notNull(sp,"非法操作");
        if(sp==null){
            return Result.error(SeckillCodeMsg.REMOTE_DATA_ERROR);
        }
        //判断是否在秒杀时间内
        boolean range=this.betweenSeckillTime(sp);
//        AssertUtils.isTrue(range,"不在时间范围内");
        if(!range){
            return Result.error(SeckillCodeMsg.OUT_OF_SECKILL_TIME_ERROR);
        }
        //用户是否下过单  redis缓存记录下单次数，但是有问题就是，没有下单成功，多次点击，提示信息是错的
//        OrderInfo orderInfo = orderInfoService.selectByUserIdAndSeckillId(userInfo.getPhone(), seckillId, time);
        String orderKey = SeckillRedisKey.SECKILL_ORDER_HASH.join(seckillId+"");
        Boolean ordered = redisTemplate.opsForHash().putIfAbsent(orderKey, userInfo.getPhone()+"", "1");  //同一商品同一用户只能下单一次
//        AssertUtils.isTrue(orderCount<=1,"不能重复下单");  已经在controller层就没必要抛出异常，耗时
        if(!ordered){
            return Result.error(SeckillCodeMsg.REPEAT_SECKILL);
        }
        try{
            //判断库存是否充足 使用redis预存库存策略
            String countKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.join(time + "");
            Long count = redisTemplate.opsForHash().increment(countKey, seckillId+"", -1);  //同一场次同一商品的库存
            AssertUtils.isTrue(count>=0,"库存不足"); //这里需要写成抛出异常，要不然，下单标识和overMap不会被执行
            //下单
//            String orderNo=orderInfoService.doSeckill(userInfo.getPhone(),sp);
            //rocketmq异步创建订单
            rocketMQTemplate.asyncSend(MQConstant.ORDER_PENDING_TOPIC,new OrderMessage(time,seckillId,token,userInfo.getPhone()),new DefaultMQMessageCallback());
            return Result.success("订单创建中");
        }catch (BusinessException e){
            redisTemplate.opsForHash().delete(orderKey,userInfo.getPhone()+"");
            overMap.put(seckillId,true);
            return Result.error(e.getCodeMsg());
        }
    }

    private boolean betweenSeckillTime(SeckillProductVo sp) {
        Calendar instance=Calendar.getInstance();
        instance.setTime(sp.getStartDate());
        //设置小时
        instance.set(Calendar.HOUR_OF_DAY,sp.getTime());
        //设置开始时间
        Date startDate=instance.getTime();
        instance.add(Calendar.HOUR_OF_DAY,23);
        Date endTime=instance.getTime();
        long now=System.currentTimeMillis();
        return startDate.getTime()<=now&&endTime.getTime()>now;
    }


    @RequireLogin
    @GetMapping("/find")
    public Result<OrderInfo> findById(String orderNo, @RequestUser UserInfo userInfo) {
        OrderInfo orderInfo = orderInfoService.findByOrderNo(orderNo, userInfo.getPhone());
        if(!userInfo.getPhone().equals(orderInfo.getUserId())){
            return Result.error(new CodeMsg(40003,"不允许访问"));
        }
        return Result.success(orderInfo);
    }

    @Autowired
    private StringRedisTemplate redisTemplate;
    private static int num=10;
    @RequireLogin
    @GetMapping("/test")
    public Result<Integer> test() {
        String key = "abd";
        try {
            Boolean ret;
            int count=0;
            do{
                ret = redisTemplate.opsForValue().setIfAbsent(key,"1");
                if (ret!=null&&ret) {
                    break;
                }
                count++;
                AssertUtils.isTrue(count<5,"+++++++++++++++++++++++++++"+num+Thread.currentThread().getName());
                Thread.sleep(20);
            }while (true);
            int n=num;
            System.out.println(n+Thread.currentThread().getName());
            Thread.sleep(500);
            AssertUtils.isTrue(n>0,"库存不足"+n+Thread.currentThread().getName());
            num--;
            // 扣减库存操作应在查询之后立即执行，并且不释放锁
        }catch (InterruptedException e){
            System.out.println(e);
        }finally {
            System.out.println("-----------------------------------------"+num+Thread.currentThread().getName());  //业务异常会执行finally，删除key
            redisTemplate.delete(key);
        }

        return Result.success(num);

    }


}
