package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.AccountLog;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.mapper.AccountLogMapper;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.msg.IntergralCodeMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Autowired
    private UsableIntegralMapper usableIntegralMapper;
    @Autowired
    private AccountTransactionMapper accountTransactionMapper;
    @Autowired
    private AccountLogMapper accountLogMapper;

    @Override
    public String doPay(OperateIntergralVo vo) {
        // 1. 获取当前用户的积分账户，判断账户余额是否足够
        // 2. 扣除积分，更新时间
        int row = usableIntegralMapper.decrIntegral(vo.getUserId(), vo.getValue());
        if (row <= 0) {
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
        // 3. 记录账户变动日志
        AccountLog log = this.buildAccountLog(vo, AccountLog.TYPE_DECR);
        accountLogMapper.insert(log);
        return log.getTradeNo();
    }

    private AccountLog buildAccountLog(OperateIntergralVo vo, Integer type) {
        AccountLog log = new AccountLog();
        log.setAmount(vo.getValue());
        log.setGmtTime(new Date());
        log.setInfo(vo.getInfo());
        log.setPkValue(vo.getPk());
        log.setType(type);
        log.setTradeNo(IdGenerateUtil.get().nextId() + "");
        return log;
    }
}
