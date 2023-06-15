package cn.wolfcode.mapper;

import cn.wolfcode.domain.AccountLog;
import org.apache.ibatis.annotations.Param;


public interface AccountLogMapper {
    /**
     * 插入日志
     *
     * @param accountLog
     */
    void insert(AccountLog accountLog);

    /**
     * 按照 pk 和 type 查询日志对象
     *
     * @param pkValue 订单编号
     * @param type    操作类型
     * @return 日志对象
     */
    AccountLog selectByPkAndType(@Param("pkValue") String pkValue, @Param("type") int type);

    AccountLog selectByPkAndStatus(@Param("pk") String pk, @Param("status") int status);

    AccountLog selectByTxId(String txId);

    void changeStatus(String tradeNo, int status);
}
