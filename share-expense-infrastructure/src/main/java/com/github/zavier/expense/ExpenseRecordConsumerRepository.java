package com.github.zavier.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRecordConsumerRepository extends JpaRepository<ExpenseRecordConsumerDO, Integer> {

    List<ExpenseRecordConsumerDO> findByProjectId(Integer projectId);

    List<ExpenseRecordConsumerDO> findByRecordId(Integer recordId);

    void deleteByProjectId(Integer projectId);
}
