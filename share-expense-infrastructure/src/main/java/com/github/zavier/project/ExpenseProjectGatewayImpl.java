package com.github.zavier.project;

import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.converter.ExpenseProjectConverter;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Repository
public class ExpenseProjectGatewayImpl implements ExpenseProjectGateway {

    @Resource
    private ExpenseProjectMapper expenseProjectMapper;
    @Resource
    private ExpenseProjectMemberMapper expenseProjectMemberMapper;

    @Override
    @Transactional
    public void save(ExpenseProject expenseProject) {
        final List<Integer> memberIds = expenseProject.getMemberIds();

        final Integer projectId = saveProject(expenseProject, memberIds);

        if (CollectionUtils.isNotEmpty(memberIds)) {
            for (Integer memberId : memberIds) {
                final ExpenseProjectMemberDO expenseProjectMemberDO = new ExpenseProjectMemberDO();
                expenseProjectMemberDO.setExpenseProjectId(projectId);
                expenseProjectMemberDO.setUserId(memberId);
                expenseProjectMemberMapper.insertSelective(expenseProjectMemberDO);
            }
        }
    }

    @Override
    @Transactional
    public void delete(Integer projectId) {
        expenseProjectMapper.deleteByPrimaryKey(projectId);
        expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getExpenseProjectId, projectId)
                .delete();
    }

    private Integer saveProject(ExpenseProject expenseProject, List<Integer> memberIds) {
        switch (expenseProject.getChangingStatus()) {
            case NEW:
                final ExpenseProjectDO projectDO = ExpenseProjectConverter.toInsertDO(expenseProject);
                expenseProjectMapper.insertSelective(projectDO);
                return projectDO.getId();
            case UPDATED:
                Assert.notNull(expenseProject.getExpenseProjectId(), "费用项目ID不能为空");
                final ExpenseProjectDO updateProjectDo = ExpenseProjectConverter.toUpdateDO(expenseProject);
                final int update = expenseProjectMapper.wrapper()
                        .eq(ExpenseProjectDO::getId, expenseProject.getExpenseProjectId())
                        .eq(ExpenseProjectDO::getVersion, expenseProject.getVersion())
                        .updateSelective(updateProjectDo);
                Assert.isTrue(update == 1, "当前项目已被其他人更新，请稍后重试");

                // 删除关联的人员
                if (CollectionUtils.isNotEmpty(memberIds)) {
                    expenseProjectMemberMapper.wrapper()
                            .eq(ExpenseProjectMemberDO::getExpenseProjectId, expenseProject.getExpenseProjectId())
                            .delete();
                }
                return expenseProject.getExpenseProjectId();
            default:
                throw new BizException("不支持的操作");
        }
    }

    @Override
    public Optional<ExpenseProject> getProjectById(@NotNull Integer expenseProjectId) {
        final List<ExpenseProjectMemberDO> expenseProjectMemberDOS = listProjectMembers(expenseProjectId);

        return expenseProjectMapper.selectByPrimaryKey(expenseProjectId)
                .map(it -> ExpenseProjectConverter.toEntity(it, expenseProjectMemberDOS));
    }

    @NotNull
    private List<ExpenseProjectMemberDO> listProjectMembers(@NotNull Integer expenseProjectId) {
        return expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getExpenseProjectId, expenseProjectId)
                .list();
    }
}
