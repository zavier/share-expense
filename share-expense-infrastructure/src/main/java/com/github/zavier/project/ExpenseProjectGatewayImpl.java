package com.github.zavier.project;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.zavier.builder.ExpenseProjectBuilder;
import com.github.zavier.converter.ExpenseProjectConverter;
import com.github.zavier.converter.ExpenseRecordDoConverter;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.expense.ExpenseRecordConsumerDO;
import com.github.zavier.expense.ExpenseRecordConsumerMapper;
import com.github.zavier.expense.ExpenseRecordDO;
import com.github.zavier.expense.ExpenseRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class ExpenseProjectGatewayImpl implements ExpenseProjectGateway {

    @Resource
    private ExpenseProjectMapper expenseProjectMapper;
    @Resource
    private ExpenseProjectMemberMapper expenseProjectMemberMapper;
    @Resource
    private ExpenseRecordMapper expenseRecordMapper;
    @Resource
    private ExpenseRecordConsumerMapper expenseRecordConsumerMapper;

    @Override
    @Transactional
    public void save(ExpenseProject expenseProject) {
        final Integer projectId = saveProject(expenseProject);
        expenseProject.setId(projectId);

        saveProjectMembers(expenseProject);

        saveExpenseRecord(expenseProject);
    }

    @Override
    @Transactional
    public void delete(Integer projectId) {
        expenseProjectMapper.deleteByPrimaryKey(projectId);
        expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getProjectId, projectId)
                .delete();
        expenseRecordMapper.wrapper()
                .eq(ExpenseRecordDO::getProjectId, projectId)
                .delete();
        expenseRecordConsumerMapper.wrapper()
                .eq(ExpenseRecordConsumerDO::getProjectId, projectId)
                .delete();
    }

    @Override
    public Optional<ExpenseProject> getProjectById(@NotNull Integer expenseProjectId) {
        final Optional<ExpenseProjectDO> expenseProjectDO = expenseProjectMapper.selectByPrimaryKey(expenseProjectId);
        if (!expenseProjectDO.isPresent()) {
            return Optional.empty();
        }


        final List<ExpenseProjectMemberDO> expenseProjectMemberDOS = listProjectMembers(expenseProjectId);
        final List<ExpenseRecordDO> recordDOList = listRecord(expenseProjectId);
        final List<ExpenseRecordConsumerDO> recordConsumerDOList = listRecordConsumer(expenseProjectId);

        final ExpenseProject build = new ExpenseProjectBuilder()
                .setExpenseProjectDO(expenseProjectDO.get())
                .setMemberDOList(expenseProjectMemberDOS)
                .setRecordDOList(recordDOList)
                .setExpenseRecordConsumerDOList(recordConsumerDOList)
                .build();

        return Optional.ofNullable(build);
    }

    @Override
    public PageResponse<ExpenseProject> pageProject(ProjectListQry projectListQry) {
        // 查询出全部自己创建+自己加入的项目
        if (projectListQry.getOperatorId() != null) {
            return pageProjectByUser(projectListQry);
        }
        // 分页
        return pageAllProject(projectListQry);
    }

    private Integer saveProject(ExpenseProject expenseProject) {
        switch (expenseProject.getChangingStatus()) {
            case NEW:
                final ExpenseProjectDO projectDO = ExpenseProjectConverter.toInsertDO(expenseProject);
                expenseProjectMapper.insertSelective(projectDO);
                return projectDO.getId();
            case UPDATED:
                Assert.notNull(expenseProject.getId(), "费用项目ID不能为空");
                final ExpenseProjectDO updateProjectDo = ExpenseProjectConverter.toUpdateDO(expenseProject);
                final int update = expenseProjectMapper.wrapper()
                        .eq(ExpenseProjectDO::getId, expenseProject.getId())
                        .eq(ExpenseProjectDO::getVersion, expenseProject.getVersion())
                        .updateSelective(updateProjectDo);
                Assert.isTrue(update == 1, "当前项目已被其他人更新，请稍后重试");
                return expenseProject.getId();
            default:
                throw new BizException("不支持的操作");
        }
    }

    private void saveProjectMembers(ExpenseProject expenseProject) {
        if (expenseProject.getMemberChangingStatus() == ChangingStatus.UNCHANGED) {
            log.info("成员无变化，无需更新");
            return;
        }

        // 删除关联的人员
        expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getProjectId, expenseProject.getId())
                .delete();

        expenseProject.listAllMember().forEach(projectMember -> {
            final ExpenseProjectMemberDO expenseProjectMemberDO = new ExpenseProjectMemberDO();
            expenseProjectMemberDO.setProjectId(expenseProject.getId());
            expenseProjectMemberDO.setName(projectMember);
            expenseProjectMemberMapper.insertSelective(expenseProjectMemberDO);
        });
    }

    private void saveExpenseRecord(ExpenseProject project) {
        if (project.getRecordChangingStatus() == ChangingStatus.UNCHANGED) {
            log.info("费用记录无变化，无需更新");
            return;
        }

        // 删除关联的费用
        expenseRecordMapper.wrapper()
                .eq(ExpenseRecordDO::getProjectId, project.getId())
                .delete();
        // 删除关联的费用消费人员
        expenseRecordConsumerMapper.wrapper()
                .eq(ExpenseRecordConsumerDO::getProjectId, project.getId())
                .delete();

        final List<ExpenseRecord> expenseRecords = project.listAllExpenseRecord();
        expenseRecords.forEach(expenseRecord -> {
            final ExpenseRecordDO insertExpenseRecordDO = ExpenseRecordDoConverter.toInsertExpenseRecordDO(expenseRecord);
            expenseRecordMapper.insertSelective(insertExpenseRecordDO);

            saveRecordMember(project, expenseRecord, insertExpenseRecordDO);
        });
    }

    private void saveRecordMember(ExpenseProject project, ExpenseRecord expenseRecord, ExpenseRecordDO insertExpenseRecordDO) {
        expenseRecord.listAllConsumers().forEach(consumer -> {
            final ExpenseRecordConsumerDO consumerDO = new ExpenseRecordConsumerDO();
            consumerDO.setProjectId(project.getId());
            consumerDO.setRecordId(insertExpenseRecordDO.getId());
            consumerDO.setMember(consumer);
            consumerDO.setCreatedAt(new Date());
            consumerDO.setUpdatedAt(new Date());
            expenseRecordConsumerMapper.insertSelective(consumerDO);
        });
    }

    private List<ExpenseRecordConsumerDO> listRecordConsumer(@NotNull Integer expenseProjectId) {
        return expenseRecordConsumerMapper.wrapper()
                .eq(ExpenseRecordConsumerDO::getProjectId, expenseProjectId)
                .list();
    }

    private List<ExpenseRecordDO> listRecord(@NotNull Integer expenseProjectId) {
        return expenseRecordMapper.wrapper()
                .eq(ExpenseRecordDO::getProjectId, expenseProjectId)
                .list();
    }

    private PageResponse<ExpenseProject> pageAllProject(ProjectListQry projectListQry) {
        PageHelper.startPage(projectListQry.getPage(), projectListQry.getSize());
        final List<ExpenseProjectDO> list =  expenseProjectMapper.wrapper()
                .orderByDesc(ExpenseProjectDO::getId)
                .list();
        final Page<ExpenseProjectDO> page = (Page<ExpenseProjectDO>) list;

        final List<Integer> projectIdList = list.stream().map(ExpenseProjectDO::getId).collect(Collectors.toList());

        final List<ExpenseProject> projectList = listProjectByIds(projectIdList);

        return PageResponse.of(projectList, (int) page.getTotal(), page.getPageSize(), page.getPageNum());
    }


    private PageResponse<ExpenseProject> pageProjectByUser(ProjectListQry projectListQry) {

        PageHelper.startPage(projectListQry.getPage(), projectListQry.getSize(), "id desc");

        // 自己创建的，按照ID倒序
        final List<ExpenseProjectDO> projectList = expenseProjectMapper.wrapper()
                .eq(ExpenseProjectDO::getCreateUserId, projectListQry.getOperatorId())
                .list();

        if (CollectionUtils.isEmpty(projectList)) {
            return PageResponse.of(projectListQry.getPage(), projectListQry.getSize());
        }

        Page<ExpenseProjectDO> page = (Page<ExpenseProjectDO>) projectList;

        // 聚合member
        final List<Integer> projectIdList = projectList.stream().map(ExpenseProjectDO::getId).collect(Collectors.toList());

        final List<ExpenseProject> expenseProjectList = listProjectByIds(projectIdList);

        return PageResponse.of(expenseProjectList, (int) page.getTotal(), page.getPageSize(), page.getPageNum());
    }

    private List<ExpenseProject> listProjectByIds(List<Integer> projectIdList) {
        return projectIdList.stream()
                .map(this::getProjectById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<ExpenseProjectMemberDO> listProjectMembers(@NotNull Integer expenseProjectId) {
        return expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getProjectId, expenseProjectId)
                .list();
    }
}
