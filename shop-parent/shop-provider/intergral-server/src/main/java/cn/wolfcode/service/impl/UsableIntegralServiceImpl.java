package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.AccountLog;
import cn.wolfcode.domain.IntegralRefundVo;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.mapper.AccountLogMapper;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.msg.IntergralCodeMsg;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


@Slf4j
@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Autowired
    private UsableIntegralMapper usableIntegralMapper;
    @Autowired
    private AccountTransactionMapper accountTransactionMapper;
    @Autowired
    private AccountLogMapper accountLogMapper;

    @Override
    public String tryIncrIntegral(OperateIntergralVo vo, BusinessActionContext context) {
        log.info("[积分支付] 执行一阶段 TRY 方法，准备冻结金额：xid={}, branchId={}, params={}", context.getXid(), context.getBranchId(), JSON.toJSONString(vo));
        // 1. 查询金额（totalAmount-freezedAmount），判断是否足够
        // 2. 增加冻结金额
        int row = usableIntegralMapper.freezeIntergral(vo.getUserId(), vo.getValue());
        if (row == 0) {
            // 余额不足
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
        // 3. 保存账户变动日志，设置状态为 TRY
        AccountLog accountLog = this.buildAccountLog(vo.getPk(), vo.getValue(), vo.getInfo(), AccountLog.TYPE_DECR);
        accountLogMapper.insert(accountLog);
        return accountLog.getTradeNo();
    }

    @Override
    public String commitIncrIntegral(BusinessActionContext context) {
        JSONObject json = (JSONObject) context.getActionContext("integralVo");
        log.info("[积分支付] 执行二阶段 CONFIRM 方法，提交积分变动操作：xid={}, branchId={}, params={}", context.getXid(), context.getBranchId(), json);
        // 1. 先查询之前 TRY 阶段执行的记录是否存在
        AccountLog accountLog = accountLogMapper.selectByPkAndStatus(json.getString("pk"), AccountLog.ACCOUNT_LOG_STATUS_TRY);
        if (accountLog == null) {
            log.warn("[积分支付] 执行积分支付的二阶段 COMMIT 操作失败，查询不到前置 TRY 操作日志..");
            return null;
        }
        // 2. 执行扣除总金额、冻结金额
        usableIntegralMapper.commitChange(json.getLong("userId"), json.getLong("value"));

        // 3. 更新账户日志变动的状态为 CONFIRM
        accountLogMapper.changeStatus(accountLog.getTradeNo(), AccountLog.ACCOUNT_LOG_STATUS_CONFIRM);
        return accountLog.getTradeNo();
    }

    @Override
    public void rollbackIncrIntegral(BusinessActionContext context) {
        JSONObject json = (JSONObject) context.getActionContext("integralVo");
        log.info("[积分支付] 执行二阶段 ROLLBACK 方法，提交积分变动操作：xid={}, branchId={}, params={}", context.getXid(), context.getBranchId(), json);
        // 1. 先查询之前 TRY 阶段执行的记录是否存在
        AccountLog accountLog = accountLogMapper.selectByPkAndStatus(json.getString("pk"), AccountLog.ACCOUNT_LOG_STATUS_TRY);
        if (accountLog == null) {
            log.warn("[积分支付] 执行积分支付的二阶段 ROLLBACK 操作失败，查询不到前置 TRY 操作日志..");
            return;
        }
        // 2. 取消冻结
        usableIntegralMapper.unFreezeIntergral(json.getLong("userId"),
                json.getLong("value"));
        // 3. 将操作日志状态更新为回滚
        accountLogMapper.changeStatus(accountLog.getTradeNo(), AccountLog.ACCOUNT_LOG_STATUS_CANCEL);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String doPay(OperateIntergralVo vo) {
        // 1. 获取当前用户的积分账户，判断账户余额是否足够
        // 2. 扣除积分，更新时间
        int row = usableIntegralMapper.decrIntegral(vo.getUserId(), vo.getValue());
        if (row <= 0) {
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
        // 3. 记录账户变动日志
        AccountLog log = null;
        try {
            log = this.buildAccountLog(vo.getPk(), vo.getValue(), vo.getInfo(), AccountLog.TYPE_DECR);
            accountLogMapper.insert(log);
        } catch (Exception e) {
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_PAY_REPEATE);
        }
        return log.getTradeNo();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean refund(IntegralRefundVo vo) {
        // 1. 基于订单 id 查询之前支付的记录，判断是否存在
        AccountLog log = accountLogMapper.selectByPkAndType(vo.getOrderNo(), AccountLog.TYPE_DECR);
        if (log == null) {
            throw new BusinessException(IntergralCodeMsg.OP_INTERGRAL_ERROR);
        }
        // 2. 之前是否已经退款过了
        AccountLog incrLog = accountLogMapper.selectByPkAndType(vo.getOrderNo(), AccountLog.TYPE_INCR);
        if (incrLog != null) {
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_REFUND_REPEATE);
        }
        // 3. 判断本次退款金额是否 <= 支付金额
        if (vo.getRefundAmount() > log.getAmount()) {
            throw new BusinessException(IntergralCodeMsg.AMOUNT_ERROR);
        }
        // 4. 增加积分，更新时间
        usableIntegralMapper.addIntergral(vo.getUserId(), vo.getRefundAmount());
        // 5. 记录账户变动日志
        incrLog = this.buildAccountLog(vo.getOrderNo(), vo.getRefundAmount(), vo.getRefundReason(), AccountLog.TYPE_INCR);
        accountLogMapper.insert(incrLog);
        return true;
    }

    private AccountLog buildAccountLog(String pkValue, Long amount, String info, Integer type) {
        AccountLog log = new AccountLog();
        log.setAmount(amount);
        log.setInfo(info);
        log.setPkValue(pkValue);

        log.setGmtTime(new Date());
        log.setType(type);
        log.setTradeNo(IdGenerateUtil.get().nextId() + "");
        log.setStatus(AccountLog.ACCOUNT_LOG_STATUS_TRY);
        return log;
    }
}
