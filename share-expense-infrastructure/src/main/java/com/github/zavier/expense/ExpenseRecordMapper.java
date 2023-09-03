package com.github.zavier.expense;

import io.mybatis.mapper.Mapper;

@org.apache.ibatis.annotations.Mapper
public interface ExpenseRecordMapper extends Mapper<ExpenseRecordDO, Integer> {
}
