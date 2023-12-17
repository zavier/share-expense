package com.github.zavier.project.executor;

import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordValidator;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.ExpenseRecordSharingAddCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ExpenseRecordSharingAddCmdExe {

    private final ExpenseRecordGateway expenseRecordGateway;
    private final ExpenseRecordValidator expenseRecordValidator;
    private final UserGateway userGateway;

    public ExpenseRecordSharingAddCmdExe(ExpenseRecordGateway expenseRecordGateway, ExpenseRecordValidator expenseRecordValidator, UserGateway userGateway) {
        this.expenseRecordGateway = expenseRecordGateway;
        this.expenseRecordValidator = expenseRecordValidator;
        this.userGateway = userGateway;
    }

    public void execute(ExpenseRecordSharingAddCmd sharingAddCmd) {
        expenseRecordValidator.valid(sharingAddCmd);

        // 设置用户名
        final Optional<User> userOpt = userGateway.getUserById(sharingAddCmd.getUserId());
        Assert.isTrue(userOpt.isPresent(), "用户不存在");

        final Optional<ExpenseRecord> expenseRecordOptional = expenseRecordGateway.getRecordById(sharingAddCmd.getRecordId());
        final ExpenseRecord expenseRecord = expenseRecordOptional.orElseThrow(() -> new BizException("用户不存在"));
        expenseRecord.addUserSharing(sharingAddCmd.getUserId(), userOpt.get().getUserName(), sharingAddCmd.getWeight());

        expenseRecord.setChangingStatus(ChangingStatus.UPDATED);
        expenseRecordGateway.save(expenseRecord);
    }
}
