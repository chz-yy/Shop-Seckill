package cn.wolfcode.mapper;

import cn.wolfcode.domain.PayLog;

import java.sql.SQLException;

/**
 * Created by wolfcode
 */
public interface PayLogMapper {
    /**
     * 插入支付日志，用于幂等性控制
     *
     * @param payLog
     * @return
     */
    int insert(PayLog payLog) throws SQLException;
}
