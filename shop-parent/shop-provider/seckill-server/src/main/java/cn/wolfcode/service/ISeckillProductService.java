package cn.wolfcode.service;

import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;

import java.util.List;


public interface ISeckillProductService {

    void failedRollback(Long seckillId, Integer time, Long userPhone);

    List<SeckillProductVo> selectTodayListByTime(Integer time);

    List<SeckillProductVo> selectTodayListByTimeFromRedis(Integer time);

    SeckillProductVo selectByIdAndTime(Long seckillId, Integer time);

    void decrStockCount(Long id);

    void decrStockCount(Long id, Integer time);

    SeckillProduct findById(Long id);


    Long selectStockCountById(Long seckillId);

    void incrStockCount(Long seckillId);
}
