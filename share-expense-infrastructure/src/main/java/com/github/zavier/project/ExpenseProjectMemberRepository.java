package com.github.zavier.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseProjectMemberRepository extends JpaRepository<ExpenseProjectMemberDO, Integer> {

    List<ExpenseProjectMemberDO> findByProjectId(Integer projectId);

    void deleteByProjectId(Integer projectId);
}
