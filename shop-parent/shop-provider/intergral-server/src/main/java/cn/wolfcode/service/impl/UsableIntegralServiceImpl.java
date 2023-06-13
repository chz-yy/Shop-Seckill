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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Autowired
    private UsableIntegralMapper usableIntegralMapper;
    @Autowired
    private AccountTransactionMapper accountTransactionMapper;
    @Autowired
    private AccountLogMapper accountLogMapper;

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
        return log;
    }
}
