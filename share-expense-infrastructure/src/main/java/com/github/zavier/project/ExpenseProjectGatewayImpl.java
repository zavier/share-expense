package com.github.zavier.project;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.zavier.converter.ExpenseProjectConverter;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectListQry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ExpenseProjectGatewayImpl implements ExpenseProjectGateway {

    @Resource
    private ExpenseProjectMapper expenseProjectMapper;
    @Resource
    private ExpenseProjectMemberMapper expenseProjectMemberMapper;

    @Override
    @Transactional
    public void save(ExpenseProject expenseProject) {
        final Integer projectId = saveProject(expenseProject);
        expenseProject.setId(projectId);

        saveProjectMembers(expenseProject);
    }

    private void saveProjectMembers(ExpenseProject expenseProject) {
        // 删除关联的人员
        expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getProjectId, expenseProject.getId())
                .delete();

        expenseProject.listMember().forEach(projectMember -> {
            final ExpenseProjectMemberDO expenseProjectMemberDO = new ExpenseProjectMemberDO();
            expenseProjectMemberDO.setProjectId(expenseProject.getId());
            expenseProjectMemberDO.setUserId(projectMember.getUserId());
            expenseProjectMemberDO.setUserName(projectMember.getUserName());
            expenseProjectMemberDO.setWeight(projectMember.getWeight());
            expenseProjectMemberMapper.insertSelective(expenseProjectMemberDO);
        });
    }

    @Override
    @Transactional
    public void delete(Integer projectId) {
        expenseProjectMapper.deleteByPrimaryKey(projectId);
        expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getProjectId, projectId)
                .delete();
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

    @Override
    public Optional<ExpenseProject> getProjectById(@NotNull Integer expenseProjectId) {
        final List<ExpenseProjectMemberDO> expenseProjectMemberDOS = listProjectMembers(expenseProjectId);

        return expenseProjectMapper.selectByPrimaryKey(expenseProjectId)
                .map(it -> ExpenseProjectConverter.toEntity(it, expenseProjectMemberDOS));
    }

    @Override
    public PageResponse<ExpenseProject> pageProject(ProjectListQry projectListQry) {
        // 查询出全部自己创建+自己加入的项目
        if (projectListQry.getUserId() != null) {
            return pageProjectByUser(projectListQry);
        }
        // 分页
        return pageAllProject(projectListQry);

    }

    @NotNull
    private PageResponse<ExpenseProject> pageAllProject(ProjectListQry projectListQry) {
        PageHelper.startPage(projectListQry.getPage(), projectListQry.getSize());
        final List<ExpenseProjectDO> list =  expenseProjectMapper.wrapper()
                .orderByDesc(ExpenseProjectDO::getId)
                .list();
        final Page<ExpenseProjectDO> page = (Page<ExpenseProjectDO>) list;

        final List<Integer> projectIdList = list.stream().map(ExpenseProjectDO::getId).collect(Collectors.toList());
        final Map<Integer, List<ExpenseProjectMemberDO>> projectIdMap = listProjectMembers(projectIdList);

        final List<ExpenseProject> projectList = list.stream()
                .map(it -> ExpenseProjectConverter.toEntity(it, projectIdMap.get(it.getId()))).collect(Collectors.toList());
        return PageResponse.of(projectList, (int) page.getTotal(), page.getPageSize(), page.getPageNum());
    }


    private PageResponse<ExpenseProject> pageProjectByUser(ProjectListQry projectListQry) {
        List<ExpenseProjectDO> resultList = new ArrayList<>();

        List<ExpenseProjectDO> repeatbleList = new ArrayList<>();
        // 创建的
        final List<ExpenseProjectDO> createdList = expenseProjectMapper.wrapper()
                .eq(ExpenseProjectDO::getCreateUserId, projectListQry.getUserId())
                .list();
        repeatbleList.addAll(createdList);

        // 加入的
        final List<ExpenseProjectMemberDO> memberDOList = expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getUserId, projectListQry.getUserId())
                .list();
        if (CollectionUtils.isNotEmpty(memberDOList)) {
            final List<Integer> projectIdList = memberDOList.stream().map(ExpenseProjectMemberDO::getProjectId).distinct()
                    .collect(Collectors.toList());
            final List<ExpenseProjectDO> joinedList = expenseProjectMapper.wrapper()
                    .in(ExpenseProjectDO::getId, projectIdList)
                    .list();
            repeatbleList.addAll(joinedList);
        }

        // 去重复
        Set<Integer> projectIdSet = new HashSet<>();
        repeatbleList.forEach(it -> {
            if (projectIdSet.add(it.getId())) {
                resultList.add(it);
            }
        });


        // 排序
        resultList.sort(Comparator.comparing(ExpenseProjectDO::getId).reversed());

        if (CollectionUtils.isEmpty(resultList)) {
            return PageResponse.of(projectListQry.getPage(), projectListQry.getSize());
        }

        // 转换结果
        int total = resultList.size();
        int offset = (projectListQry.getPage() - 1) * projectListQry.getSize();
        final List<ExpenseProjectDO> collect = resultList.stream()
                .skip(offset)
                .limit(projectListQry.getSize())
                .collect(Collectors.toList());

        // 聚合member
        final List<Integer> projectIdList = collect.stream().map(ExpenseProjectDO::getId).collect(Collectors.toList());
        final Map<Integer, List<ExpenseProjectMemberDO>> projectIdMap = listProjectMembers(projectIdList);

        final List<ExpenseProject> projectList = collect.stream()
                .map(it -> ExpenseProjectConverter.toEntity(it, projectIdMap.get(it.getId()))).collect(Collectors.toList());

        return PageResponse.of(projectList, total, projectListQry.getSize(), projectListQry.getPage());
    }

    @NotNull
    private List<ExpenseProjectMemberDO> listProjectMembers(@NotNull Integer expenseProjectId) {
        return expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getProjectId, expenseProjectId)
                .list();
    }

    private Map<Integer, List<ExpenseProjectMemberDO>> listProjectMembers(List<Integer> projectIdList) {
        if (CollectionUtils.isEmpty(projectIdList)) {
            return Collections.emptyMap();
        }
        final List<ExpenseProjectMemberDO> list = expenseProjectMemberMapper.wrapper()
                .in(ExpenseProjectMemberDO::getProjectId, projectIdList)
                .list();
        return list.stream()
                .collect(Collectors.groupingBy(ExpenseProjectMemberDO::getProjectId));

    }
}
