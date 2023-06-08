package cn.wolfcode.service;

import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;

import java.util.List;


public interface ISeckillProductService {

    List<SeckillProductVo> selectTodayListByTime(Integer time);

    List<SeckillProductVo> selectTodayListByTimeFromRedis(Integer time);

    SeckillProductVo selectByIdAndTime(Long seckillId, Integer time);

    int decrStockCount(Long id, Integer time);

    SeckillProduct findById(Long id);
}
