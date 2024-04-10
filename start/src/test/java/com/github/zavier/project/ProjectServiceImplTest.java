package com.github.zavier.project;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectAddCmd;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Rollback
@Transactional
public class ProjectServiceImplTest {

    @Resource
    private ProjectService projectService;
    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    @Test
    public void testCreateProjectWithMember() throws Exception {
        final ProjectAddCmd projectAddCmd = new ProjectAddCmd();
        projectAddCmd.setProjectName("测试项目1");
        projectAddCmd.setProjectDesc("测试项目描述1");
        projectAddCmd.setCreateUserId(1);
        projectAddCmd.setCreateUserName("测试用户1");

        List<String> members = new ArrayList<>();
        members.add("其他成员1");
        members.add("其他成员2");
        members.add("其他成员3");
        projectAddCmd.setMembers(members);

        final SingleResponse<Integer> project = projectService.createProject(projectAddCmd);
        assertTrue(project.isSuccess());

        final Optional<ExpenseProject> createdProjectOpt = expenseProjectGateway.getProjectById(project.getData());
        assertTrue(createdProjectOpt.isPresent());


        final ExpenseProject expenseProject = createdProjectOpt.get();
        assertFalse(expenseProject.getLocked());
        assertEquals(projectAddCmd.getProjectName(), expenseProject.getName());
        assertEquals(projectAddCmd.getProjectDesc(), expenseProject.getDescription());
        assertEquals(projectAddCmd.getCreateUserId(), expenseProject.getCreateUserId());

        final List<String> savedMember = expenseProject.listAllMember();
        assertEquals(members.size(), savedMember.size());

        assertTrue(savedMember.containsAll(members));
    }


    @Test
    public void testAddMember() {
//        projectService.addProjectMember()
    }

}