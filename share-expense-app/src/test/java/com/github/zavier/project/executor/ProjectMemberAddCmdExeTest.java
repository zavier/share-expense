package com.github.zavier.project.executor;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.expense.ExpenseProject;
import com.google.common.collect.Lists;

import com.github.zavier.UnitTestBase;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectMemberAddCmd;
import com.github.zavier.mock.ExpenseProjectGatewayMock;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import static org.junit.Assert.*;

public class ProjectMemberAddCmdExeTest extends UnitTestBase {

    @InjectMocks
    private ProjectMemberAddCmdExe projectMemberAddCmdExe;

    @Spy
    private ExpenseProjectGateway expenseProjectGateway = new ExpenseProjectGatewayMock();

    @Test
    public void testAddEmptyMember() {
        final ProjectMemberAddCmd projectMemberAddCmd = new ProjectMemberAddCmd();
        projectMemberAddCmd.setProjectId(0);
        projectMemberAddCmd.setMembers(Lists.newArrayList());
        try {
            final Response response = projectMemberAddCmdExe.addProjectMember(projectMemberAddCmd);
            fail();
        } catch (BizException e) {
            assertEquals("成员信息不能为空", e.getMessage());
        }
    }

    @Test
    public void testAddInvalidProjectMember() {
        final ProjectMemberAddCmd projectMemberAddCmd = new ProjectMemberAddCmd();
        projectMemberAddCmd.setProjectId(0);
        projectMemberAddCmd.setMembers(Lists.newArrayList("igy"));
        try {
            final Response response = projectMemberAddCmdExe.addProjectMember(projectMemberAddCmd);
            fail();
        } catch (BizException e) {
            assertEquals("项目不存在", e.getMessage());
        }
    }

    @Test
    public void testAddRepeatMember() {
        final ExpenseProject expenseProject = new ExpenseProject();
        expenseProject.setName("测试项目");
        expenseProject.setId(1);
        expenseProjectGateway.save(expenseProject);

        final ProjectMemberAddCmd projectMemberAddCmd = new ProjectMemberAddCmd();
        projectMemberAddCmd.setProjectId(1);
        projectMemberAddCmd.setMembers(Lists.newArrayList("张三", "李四", "张三"));
        try {
            final Response response = projectMemberAddCmdExe.addProjectMember(projectMemberAddCmd);
            fail();
        } catch (BizException e) {
            assertEquals("添加用户已存在:张三", e.getMessage());
        }
    }
}