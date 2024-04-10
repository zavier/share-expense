package com.github.zavier.project.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.UnitTestBase;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.mock.ExpenseProjectGatewayMock;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class ProjectAddCmdExeTest extends UnitTestBase {

    @InjectMocks
    private ProjectAddCmdExe projectAddCmdExe;

    @Spy
    private ExpenseProjectGateway expenseProjectGateway = new ExpenseProjectGatewayMock();



    @Test
    public void testCreateProjectWithoutName() {
        final ProjectAddCmd projectAddCmd = new ProjectAddCmd();
        projectAddCmd.setProjectName("");
        projectAddCmd.setProjectDesc("测试项目描述1");
        projectAddCmd.setCreateUserId(1);
        projectAddCmd.setCreateUserName("测试用户1");
        final SingleResponse<Integer> execute;
        try {
            projectAddCmdExe.execute(projectAddCmd);
            fail();
        } catch (BizException e) {
            assertEquals("项目名称不能为空", e.getMessage());
        }
    }

    @Test
    public void testCreateProject() {
        final ProjectAddCmd projectAddCmd = new ProjectAddCmd();
        projectAddCmd.setProjectName("测试项目1");
        projectAddCmd.setProjectDesc("测试项目描述1");
        projectAddCmd.setCreateUserId(1);
        projectAddCmd.setCreateUserName("测试用户1");
        final SingleResponse<Integer> execute = projectAddCmdExe.execute(projectAddCmd);

        assertTrue(execute.isSuccess());
        assertNotNull(execute.getData());

        Integer projectId = execute.getData();

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
        assertTrue(projectOpt.isPresent());

        final ExpenseProject expenseProject = projectOpt.get();
        assertFalse(expenseProject.getLocked());
        assertEquals(projectAddCmd.getProjectName(), expenseProject.getName());
        assertEquals(projectAddCmd.getProjectDesc(), expenseProject.getDescription());
        assertEquals(projectAddCmd.getCreateUserId(), expenseProject.getCreateUserId());

        final List<String> savedMember = expenseProject.listAllMember();
        assertEquals(0, savedMember.size());

    }

    @Test
    public void testCreateProjectWithMember() {
        final ProjectAddCmd projectAddCmd = new ProjectAddCmd();
        projectAddCmd.setProjectName("测试项目1");
        projectAddCmd.setProjectDesc("测试项目描述1");
        projectAddCmd.setCreateUserId(1);
        projectAddCmd.setCreateUserName("测试用户1");

        Lists.newArrayList("测试用户2", "测试用户3").forEach(projectAddCmd.getMembers()::add);

        final SingleResponse<Integer> execute = projectAddCmdExe.execute(projectAddCmd);

        assertTrue(execute.isSuccess());
        assertNotNull(execute.getData());

        Integer projectId = execute.getData();

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
        assertTrue(projectOpt.isPresent());

        final ExpenseProject expenseProject = projectOpt.get();
        assertFalse(expenseProject.getLocked());
        assertEquals(projectAddCmd.getProjectName(), expenseProject.getName());
        assertEquals(projectAddCmd.getProjectDesc(), expenseProject.getDescription());
        assertEquals(projectAddCmd.getCreateUserId(), expenseProject.getCreateUserId());

        final List<String> savedMember = expenseProject.listAllMember();
        assertEquals(2, savedMember.size());

    }
}