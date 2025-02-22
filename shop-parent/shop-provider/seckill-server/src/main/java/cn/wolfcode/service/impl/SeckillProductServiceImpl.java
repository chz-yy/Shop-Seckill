package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.util.AssertUtils;
import cn.wolfcode.common.web.CodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.feign.ProductFeignApi;
import cn.wolfcode.mapper.SeckillProductMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.ISeckillProductService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.convert.TransformReadField;
import org.aspectj.weaver.tools.Trace;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@CacheConfig(cacheNames = "SeckillProduct")
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignApi productFeignApi;
//    @Autowired
//    private RocketMQTemplate rocketMQTemplate;

    @Override
    public List<SeckillProductVo> selectTodayListByTime(Integer time) {
        // 1. 调用秒杀服务接口, 基于今天的时间, 查询今天的所有秒杀商品数据
        List<SeckillProduct> todayList = seckillProductMapper.queryCurrentlySeckillProduct(time);
        if (todayList.size() == 0) {
            return Collections.emptyList();
        }
        // 2. 遍历秒杀商品列表, 得到商品 id 列表
        List<Long> productIdList = todayList.stream() // Stream<SeckillProduct>
                .map(SeckillProduct::getProductId) // SeckillProduct => Long
                .distinct()
                .collect(Collectors.toList());
        // 3. 根据商品 id 列表, 调用商品服务查询接口, 得到商品列表
        Result<List<Product>> result = productFeignApi.selectByIdList(productIdList);
        /**
         * result 可能存在的几种情况:
         *  1. 远程接口正常返回, code == 200, data == 想要的数据
         *  2. 远程接口出现异常, code != 200
         *  3. 接口被熔断降级, data == null
         */
        if (result.hasError() || result.getData() == null) {
            throw new BusinessException(new CodeMsg(result.getCode(), result.getMsg()));
        }

        List<Product> products = result.getData();

        // 4. 遍历秒杀商品列表, 将商品对象与秒杀商品对象聚合到一起
        // List<SeckillProduct> => List<SeckillProductVo>
        List<SeckillProductVo> productVoList = todayList.stream()
                .map(sp -> {
                    SeckillProductVo vo = new SeckillProductVo();
                    BeanUtils.copyProperties(sp, vo);

                    // 遍历远程查询的商品列表，判断是否与当前的秒杀商品关联的商品对象一致
                    // 如果是一致的，将该对象返回并将属性拷贝到 vo 对象中
                    List<Product> list = products.stream().filter(p -> sp.getProductId().equals(p.getId())).collect(Collectors.toList());
                    if (list.size() > 0) {
                        Product product = list.get(0);
                        BeanUtils.copyProperties(product, vo);
                    }
                    vo.setId(sp.getId());

                    return vo;
                }) // Stream<SeckillProductVo>
                .collect(Collectors.toList());

        return productVoList;
    }

    @Override
    public List<SeckillProductVo> selectTodayListByTimeFromRedis(Integer time) {
        String key = SeckillRedisKey.SECKILL_PRODUCT_LIST.join(time + "");
        List<String> stringList = redisTemplate.opsForList().range(key, 0, -1); //查询全部

        if (stringList == null || stringList.size() == 0) {
            log.warn("[秒杀商品] 查询秒杀商品列表异常, Redis 中没有数据, 从 DB 中查询...");
            return this.selectTodayListByTime(time);
        }

        return stringList.stream().map(json -> JSON.parseObject(json, SeckillProductVo.class)).collect(Collectors.toList()); //json转java实体
    }

    @Override
    @Cacheable(key = "'selectByIdAndTime:'+ #seckillId")  //被坑了，这里查询的时候进行了缓存，多次测试的时候，一直从缓存拿的库存
    public SeckillProductVo selectByIdAndTime(Long seckillId, Integer time) {
        SeckillProduct seckillProduct = seckillProductMapper.selectByIdAndTime(seckillId, time);

        Result<List<Product>> result = productFeignApi.selectByIdList(Collections.singletonList(seckillProduct.getProductId()));
        if (result.hasError() || result.getData() == null || result.getData().size() == 0) {
            throw new BusinessException(new CodeMsg(result.getCode(), result.getMsg()));
        }

        Product product = result.getData().get(0);

        SeckillProductVo vo = new SeckillProductVo();
        // 先将商品的属性 copy 到 vo 对象中
        BeanUtils.copyProperties(product, vo);

        // 再将秒杀商品的属性 copy 到 vo 对象中, 并覆盖 id 属性
        BeanUtils.copyProperties(seckillProduct, vo);
        return vo;
    }

    /*
     * 当执行下方的 update 方法时，会自动拼接注解中的 key，将其从 redis 中删除
    @CacheEvict(key = "'selectByIdAndTime:' + #product.time + ':' + #product.id")
    public void update(SeckillProduct product) {
        // 更新操作
    }*/

    @CacheEvict(key = "'selectByIdAndTime:'+ #id")
    @Override
    public void decrStockCount(Long id, Integer time) {
        String key = "seckill:production:stockcount:" + time + ":" + id;
        try { //自旋锁
            int count = 0;
            boolean ret = true;
            do {
                ret = redisTemplate.opsForValue().setIfAbsent(key, "1");
                if (ret) {
                    break;
                }
                AssertUtils.isTrue((count++) < 5, "系统繁忙");
                Thread.sleep(20);
            } while (true);

            Long stockCount = seckillProductMapper.selectStockCountById(id);
            System.out.println(stockCount);
            AssertUtils.isTrue(stockCount > 0, "库存不足");

            // 扣减库存操作应在查询之后立即执行，并且不释放锁
            seckillProductMapper.decrStock(id);

            redisTemplate.delete(key); //不能放在finally里，系统繁忙异常会删除key，导致超卖
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public SeckillProduct findById(Long id) {
        return seckillProductMapper.selectById(id);
    }

    @Override
    public void incryStockCount(Long seckillId) {
        seckillProductMapper.incrStock(seckillId);
    }
}
