package com.github.zavier.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecordDO, Integer> {

    List<ExpenseRecordDO> findByProjectIdOrderByPayDateAsc(Integer projectId);

    /**
     * 批量查询多个项目的费用记录
     * <p>
     * 性能优化：避免 N+1 查询问题
     *
     * @param projectIds 项目ID列表
     * @return 费用记录列表（按支付日期升序排序）
     */
    List<ExpenseRecordDO> findByProjectIdInOrderByPayDateAsc(List<Integer> projectIds);

    void deleteByProjectId(Integer projectId);
}
