package com.github.zavier.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRecordConsumerRepository extends JpaRepository<ExpenseRecordConsumerDO, Integer> {

    List<ExpenseRecordConsumerDO> findByProjectId(Integer projectId);

    List<ExpenseRecordConsumerDO> findByRecordId(Integer recordId);

    /**
     * 批量查询多条记录的消费人员
     * <p>
     * 性能优化：避免 N+1 查询问题
     *
     * @param recordIds 费用记录ID列表
     * @return 消费人员列表
     */
    List<ExpenseRecordConsumerDO> findByRecordIdIn(List<Integer> recordIds);

    void deleteByProjectId(Integer projectId);
}
