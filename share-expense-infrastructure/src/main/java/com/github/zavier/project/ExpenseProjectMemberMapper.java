package com.github.zavier.project;

import com.github.zavier.domain.expense.ExpenseProjectMember;
import io.mybatis.mapper.Mapper;

@org.apache.ibatis.annotations.Mapper
public interface ExpenseProjectMemberMapper extends Mapper<ExpenseProjectMember, Integer> {
}
