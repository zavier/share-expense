package com.github.zavier.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecordDO, Integer> {

    List<ExpenseRecordDO> findByProjectIdOrderByPayDateAsc(Integer projectId);

    void deleteByProjectId(Integer projectId);
}
