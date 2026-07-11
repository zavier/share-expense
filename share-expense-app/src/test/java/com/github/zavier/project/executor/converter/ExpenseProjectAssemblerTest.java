package com.github.zavier.project.executor.converter;

import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.dto.ProjectAddCmd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseProjectAssemblerTest {

    private ProjectAddCmd validProjectAddCmd;
    private ProjectAddCmd invalidUserIdProjectAddCmd;
    private ProjectAddCmd invalidProjectNameProjectAddCmd;

    @BeforeEach
    void setUp() {
        validProjectAddCmd = new ProjectAddCmd();
        validProjectAddCmd.setCreateUserId(1);
        validProjectAddCmd.setProjectName("Valid Project");
        validProjectAddCmd.setProjectDesc("Valid Description");
        validProjectAddCmd.setMembers(Arrays.asList("Member1", "Member2"));

        invalidUserIdProjectAddCmd = new ProjectAddCmd();
        invalidUserIdProjectAddCmd.setCreateUserId(999); // 假设999不存在
        invalidUserIdProjectAddCmd.setProjectName("Valid Project");
        invalidUserIdProjectAddCmd.setProjectDesc("Valid Description");
        invalidUserIdProjectAddCmd.setMembers(Collections.singletonList("Member1"));

        invalidProjectNameProjectAddCmd = new ProjectAddCmd();
        invalidProjectNameProjectAddCmd.setCreateUserId(1);
        invalidProjectNameProjectAddCmd.setProjectName(""); // 无效的项目名称
        invalidProjectNameProjectAddCmd.setProjectDesc("Valid Description");
        invalidProjectNameProjectAddCmd.setMembers(Collections.singletonList("Member1"));
    }

    @Test
    void toExpenseProject_ValidInput_ShouldConvertSuccessfully() {
        ExpenseProject expenseProject = ExpenseProjectAssembler.toExpenseProject(validProjectAddCmd);

        assertEquals(validProjectAddCmd.getCreateUserId(), expenseProject.getCreateUserId());
        assertEquals(validProjectAddCmd.getProjectName(), expenseProject.getName());
        assertEquals(validProjectAddCmd.getProjectDesc(), expenseProject.getDescription());
        assertFalse(expenseProject.getLocked());
        assertEquals(validProjectAddCmd.getMembers(), expenseProject.listAllMember());
    }

    @Test
    void toExpenseProject_InvalidUserId_ShouldThrowException() {
        invalidUserIdProjectAddCmd.setCreateUserId(null);
        BizException ex = assertThrows(BizException.class,
                () -> ExpenseProjectAssembler.toExpenseProject(invalidUserIdProjectAddCmd));
        assertEquals("创建人不能为空", ex.getMessage());
    }

    @Test
    void toExpenseProject_InvalidProjectName_ShouldThrowException() {
        BizException ex = assertThrows(BizException.class,
                () -> ExpenseProjectAssembler.toExpenseProject(invalidProjectNameProjectAddCmd));
        assertEquals("项目名称不能为空", ex.getMessage());
    }
}
