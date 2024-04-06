package com.github.zavier.project;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectAddCmd;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Rollback
@Transactional
public class ProjectServiceImplTest {

    @Resource
    private ProjectService projectService;
    @Resource
    private ExpenseProjectMapper expenseProjectMapper;
    @Resource
    private ExpenseProjectMemberMapper expenseProjectMemberMapper;

    @Test
    public void createProject() throws Exception {
        final ProjectAddCmd projectAddCmd = new ProjectAddCmd();
        projectAddCmd.setProjectName("测试项目1");
        projectAddCmd.setProjectDesc("测试项目描述1");
        projectAddCmd.setUserId(1);
        projectAddCmd.setUserName("测试用户1");

        List<String> members = new ArrayList<>();
        members.add("其他成员1");
        members.add("其他成员2");
        members.add("其他成员3");
        projectAddCmd.setMembers(String.join(",", members));


        final SingleResponse<Integer> project = projectService.createProject(projectAddCmd);
        assertTrue(project.isSuccess());


        final List<ExpenseProjectDO> projectList = expenseProjectMapper.wrapper()
                .eq(ExpenseProjectDO::getId, project.getData())
                .list();
        assertEquals(1, projectList.size());
        final ExpenseProjectDO projectDO = projectList.get(0);
        assertEquals(projectAddCmd.getProjectName(), projectDO.getName());
        assertEquals(projectAddCmd.getProjectDesc(), projectDO.getDescription());
        assertEquals(projectAddCmd.getUserId(), projectDO.getCreateUserId());


        final List<ExpenseProjectMemberDO> projectMemberDOS = expenseProjectMemberMapper.wrapper()
                .eq(ExpenseProjectMemberDO::getProjectId, projectDO.getId())
                .list();
        assertEquals(3, projectMemberDOS.size());
        Set<String> memberSet = new HashSet<>(members);
        for (ExpenseProjectMemberDO projectMemberDO : projectMemberDOS) {
            final Boolean isVirtual = projectMemberDO.getIsVirtual();
            assertTrue(isVirtual);
            assertTrue(memberSet.contains(projectMemberDO.getUserName()));
        }
    }


}