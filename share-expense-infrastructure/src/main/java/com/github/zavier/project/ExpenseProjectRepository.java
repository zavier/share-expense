package com.github.zavier.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseProjectRepository extends JpaRepository<ExpenseProjectDO, Integer>,
        JpaSpecificationExecutor<ExpenseProjectDO> {

    List<ExpenseProjectDO> findByCreateUserIdOrderByCreated_atDesc(Integer createUserId);

    Page<ExpenseProjectDO> findById(Integer id, Pageable pageable);
}
