package com.github.zavier.project.executor.converter;


import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.dto.ProjectAddCmd;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class ProjectConverterTest {

    private ProjectAddCmd validProjectAddCmd;
    private ProjectAddCmd invalidUserIdProjectAddCmd;
    private ProjectAddCmd invalidProjectNameProjectAddCmd;

    @Before
    public void setUp() {
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
    public void convert2AddProject_ValidInput_ShouldConvertSuccessfully() {
        ExpenseProject expenseProject = ProjectConverter.convert2AddProject(validProjectAddCmd);

        assertEquals(validProjectAddCmd.getCreateUserId(), expenseProject.getCreateUserId());
        assertEquals(validProjectAddCmd.getProjectName(), expenseProject.getName());
        assertEquals(validProjectAddCmd.getProjectDesc(), expenseProject.getDescription());
        assertFalse(expenseProject.getLocked());
        assertEquals(ChangingStatus.NEW, expenseProject.getChangingStatus());
        assertEquals(validProjectAddCmd.getMembers(), expenseProject.listAllMember());
    }

    @Test
    public void convert2AddProject_InvalidUserId_ShouldThrowException() {
        invalidUserIdProjectAddCmd.setCreateUserId(null);
        try {
            ProjectConverter.convert2AddProject(invalidUserIdProjectAddCmd);
            fail();
        } catch (BizException e) {
            assertEquals("创建人不能为空", e.getMessage());
        }
    }

    @Test
    public void convert2AddProject_InvalidProjectName_ShouldThrowException() {
        try {
            ProjectConverter.convert2AddProject(invalidProjectNameProjectAddCmd);
            fail();
        } catch (BizException e) {
            assertEquals("项目名称不能为空", e.getMessage());
        }
    }
}
