package com.github.zavier.project;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.alibaba.fastjson2.JSON;
import com.github.zavier.builder.ExpenseProjectBuilder;
import com.github.zavier.converter.ExpenseProjectConverter;
import com.github.zavier.converter.ExpenseRecordDoConverter;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.expense.ExpenseRecordConsumerDO;
import com.github.zavier.expense.ExpenseRecordConsumerRepository;
import com.github.zavier.expense.ExpenseRecordDO;
import com.github.zavier.expense.ExpenseRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class ExpenseProjectGatewayImpl implements ExpenseProjectGateway {

    @Resource
    private ExpenseProjectRepository expenseProjectRepository;
    @Resource
    private ExpenseProjectMemberRepository expenseProjectMemberRepository;
    @Resource
    private ExpenseRecordRepository expenseRecordRepository;
    @Resource
    private ExpenseRecordConsumerRepository expenseRecordConsumerRepository;

    @Override
    @Transactional
    public void save(ExpenseProject expenseProject) {
        log.info("save project:{}", JSON.toJSONString(expenseProject));

        saveProject(expenseProject);

        saveProjectMembers(expenseProject);

        saveExpenseRecord(expenseProject);
    }

    @Override
    @Transactional
    public void delete(Integer projectId) {
        expenseProjectRepository.deleteById(projectId);
        expenseProjectMemberRepository.deleteByProjectId(projectId);
        expenseRecordRepository.deleteByProjectId(projectId);
        expenseRecordConsumerRepository.deleteByProjectId(projectId);
    }

    @Override
    public Optional<ExpenseProject> getProjectById(@NotNull Integer expenseProjectId) {
        final Optional<ExpenseProjectDO> expenseProjectDO = expenseProjectRepository.findById(expenseProjectId);
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
        return Optional.of(build);
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
                final ExpenseProjectDO saved = expenseProjectRepository.save(projectDO);
                // Sync id and version back to expenseProject
                expenseProject.setId(saved.getId());
                expenseProject.setVersion(saved.getVersion());
                return saved.getId();
            case UPDATED:
                Assert.notNull(expenseProject.getId(), "费用项目ID不能为空");
                // Manual version control and optimistic locking
                final Optional<ExpenseProjectDO> existing = expenseProjectRepository.findById(expenseProject.getId());
                if (existing.isPresent()) {
                    final ExpenseProjectDO existingDO = existing.get();
                    if (!existingDO.getVersion().equals(expenseProject.getVersion())) {
                        throw new BizException("当前项目已被其他人更新，请稍后重试");
                    }
                    final ExpenseProjectDO updateProjectDo = ExpenseProjectConverter.toUpdateDO(expenseProject);
                    updateProjectDo.setVersion(existingDO.getVersion() + 1);
                    final ExpenseProjectDO updated = expenseProjectRepository.save(updateProjectDo);
                    expenseProject.setVersion(updated.getVersion());
                    return expenseProject.getId();
                } else {
                    throw new BizException("项目不存在");
                }
            case UNCHANGED:
                Assert.notNull(expenseProject.getId(), "费用项目ID不能为空");
                log.info("项目无变化，无需更新");
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
        expenseProjectMemberRepository.deleteByProjectId(expenseProject.getId());

        expenseProject.listAllMember().forEach(projectMember -> {
            final ExpenseProjectMemberDO expenseProjectMemberDO = new ExpenseProjectMemberDO();
            expenseProjectMemberDO.setProjectId(expenseProject.getId());
            expenseProjectMemberDO.setName(projectMember);
            expenseProjectMemberRepository.save(expenseProjectMemberDO);
        });
    }

    private void saveExpenseRecord(ExpenseProject project) {
        if (project.getRecordChangingStatus() == ChangingStatus.UNCHANGED) {
            log.info("费用记录无变化，无需更新");
            return;
        }

        // 删除关联的费用
        expenseRecordRepository.deleteByProjectId(project.getId());
        // 删除关联的费用消费人员
        expenseRecordConsumerRepository.deleteByProjectId(project.getId());

        final List<ExpenseRecord> expenseRecords = project.listAllExpenseRecord();
        expenseRecords.forEach(expenseRecord -> {
            // Ensure projectId is set
            if (expenseRecord.getProjectId() == null) {
                expenseRecord.setProjectId(project.getId());
            }
            final ExpenseRecordDO insertExpenseRecordDO = ExpenseRecordDoConverter.toInsertExpenseRecordDO(expenseRecord);
            final ExpenseRecordDO savedRecord = expenseRecordRepository.save(insertExpenseRecordDO);

            // Sync the generated ID back to the domain object
            expenseRecord.setId(savedRecord.getId());

            saveRecordMember(project, expenseRecord, savedRecord);
        });
    }

    private void saveRecordMember(ExpenseProject project, ExpenseRecord expenseRecord, ExpenseRecordDO savedRecord) {
        expenseRecord.listAllConsumers().forEach(consumer -> {
            final ExpenseRecordConsumerDO consumerDO = new ExpenseRecordConsumerDO();
            consumerDO.setProjectId(project.getId());
            consumerDO.setRecordId(savedRecord.getId());
            consumerDO.setMember(consumer);
            consumerDO.setCreatedAt(new Date());
            consumerDO.setUpdatedAt(new Date());
            expenseRecordConsumerRepository.save(consumerDO);
        });
    }

    private List<ExpenseRecordConsumerDO> listRecordConsumer(@NotNull Integer expenseProjectId) {
        return expenseRecordConsumerRepository.findByProjectId(expenseProjectId);
    }

    private List<ExpenseRecordDO> listRecord(@NotNull Integer expenseProjectId) {
        return expenseRecordRepository.findByProjectIdOrderByPayDateAsc(expenseProjectId);
    }

    private PageResponse<ExpenseProject> pageAllProject(ProjectListQry projectListQry) {
        // Build specification
        Specification<ExpenseProjectDO> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(projectListQry.getName())) {
                predicates.add(cb.like(root.get("name"), projectListQry.getName() + "%"));
            }
            if (projectListQry.getId() != null) {
                predicates.add(cb.equal(root.get("id"), projectListQry.getId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Create pageable
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(projectListQry.getPage() - 1, projectListQry.getSize(), sort);

        // Execute query
        Page<ExpenseProjectDO> page = expenseProjectRepository.findAll(spec, pageable);

        final List<Integer> projectIdList = page.stream()
                .map(ExpenseProjectDO::getId)
                .collect(Collectors.toList());

        final List<ExpenseProject> projectList = listProjectByIds(projectIdList);

        return PageResponse.of(projectList, (int) page.getTotalElements(), page.getSize(), projectListQry.getPage());
    }


    private PageResponse<ExpenseProject> pageProjectByUser(ProjectListQry projectListQry) {
        // Build specification
        Specification<ExpenseProjectDO> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("createUserId"), projectListQry.getOperatorId()));

            if (StringUtils.isNotBlank(projectListQry.getName())) {
                predicates.add(cb.like(root.get("name"), projectListQry.getName() + "%"));
            }
            if (projectListQry.getId() != null) {
                predicates.add(cb.equal(root.get("id"), projectListQry.getId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Create pageable
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(projectListQry.getPage() - 1, projectListQry.getSize(), sort);

        // Execute query
        Page<ExpenseProjectDO> page = expenseProjectRepository.findAll(spec, pageable);

        if (CollectionUtils.isEmpty(page.getContent())) {
            return PageResponse.of(projectListQry.getPage(), projectListQry.getSize());
        }

        // 聚合member
        final List<Integer> projectIdList = page.stream()
                .map(ExpenseProjectDO::getId)
                .collect(Collectors.toList());

        final List<ExpenseProject> expenseProjectList = listProjectByIds(projectIdList);

        return PageResponse.of(expenseProjectList, (int) page.getTotalElements(), page.getSize(), projectListQry.getPage());
    }

    private List<ExpenseProject> listProjectByIds(List<Integer> projectIdList) {
        return projectIdList.stream()
                .map(this::getProjectById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<ExpenseProjectMemberDO> listProjectMembers(@NotNull Integer expenseProjectId) {
        return expenseProjectMemberRepository.findByProjectId(expenseProjectId);
    }
}
