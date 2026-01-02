package com.github.zavier.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseProjectMemberRepository extends JpaRepository<ExpenseProjectMemberDO, Integer> {

    List<ExpenseProjectMemberDO> findByProjectId(Integer projectId);

    /**
     * 批量查询多个项目的成员
     * <p>
     * 性能优化：避免 N+1 查询问题
     *
     * @param projectIds 项目ID列表
     * @return 成员列表
     */
    List<ExpenseProjectMemberDO> findByProjectIdIn(List<Integer> projectIds);

    void deleteByProjectId(Integer projectId);
}
