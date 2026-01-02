package com.github.zavier.infrastructure.project;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.Application;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectListQry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Rollback
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.jpa.properties.hibernate.use_sql_comments=true"
})
public class ExpenseProjectGatewayImplTest {

    @Autowired
    private ExpenseProjectGateway expenseProjectGateway;

    private ExpenseProject testProject;

    @BeforeEach
    void setUp() {
        // Create a test project
        testProject = new ExpenseProject();
        testProject.setName("Test Project");
        testProject.setDescription("Test Description");
        testProject.setCreateUserId(1);
        testProject.setLocked(false);
        testProject.setVersion(0);
        testProject.setChangingStatus(com.github.zavier.domain.common.ChangingStatus.NEW);
        testProject.setMemberChangingStatus(com.github.zavier.domain.common.ChangingStatus.NEW);
        testProject.setRecordChangingStatus(com.github.zavier.domain.common.ChangingStatus.UNCHANGED);

        // Add members
        testProject.addMember("Alice");
        testProject.addMember("Bob");
    }

    @Test
    void testSaveProject() {
        // When
        expenseProjectGateway.save(testProject);

        // Then
        assertNotNull(testProject.getId());
        Optional<ExpenseProject> found = expenseProjectGateway.getProjectById(testProject.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Project", found.get().getName());
        assertEquals(2, found.get().listAllMember().size());
    }

    @Test
    void testSaveProjectWithExpenseRecords() {
        // Given
        expenseProjectGateway.save(testProject);

        Optional<ExpenseProject> found = expenseProjectGateway.getProjectById(testProject.getId());
        assertTrue(found.isPresent());

        // Add expense records
        ExpenseRecord record1 = new ExpenseRecord();
        record1.setPayMember("Alice");
        record1.setAmount(new BigDecimal("100.00"));
        record1.setDate(new Date());
        record1.setExpenseType("餐饮");
        record1.setRemark("Lunch");
        record1.addConsumer("Alice");
        record1.addConsumer("Bob");

        final ExpenseProject expenseProject = found.get();
        expenseProject.addExpenseRecord(record1);
        expenseProject.setRecordChangingStatus(ChangingStatus.NEW);
        expenseProject.setChangingStatus(ChangingStatus.UPDATED);

        // When
        expenseProjectGateway.save(expenseProject);

        // Then
        found = expenseProjectGateway.getProjectById(testProject.getId());
        assertTrue(found.isPresent());
        assertEquals(1, found.get().listAllExpenseRecord().size());
    }

    @Test
    void testUpdateProjectWithOptimisticLock() {
        // Given - save initial version
        System.out.println("Before first save, version: " + testProject.getVersion());
        expenseProjectGateway.save(testProject);
        Integer originalVersion = testProject.getVersion();
        System.out.println("After first save, version: " + originalVersion);

        // Reload from database to get clean state
        testProject = expenseProjectGateway.getProjectById(testProject.getId()).get();
        System.out.println("After reload, version: " + testProject.getVersion());

        // When - update project
        testProject.setName("Updated Project");
        testProject.setChangingStatus(ChangingStatus.UPDATED);
        System.out.println("Before second save, version: " + testProject.getVersion());
        expenseProjectGateway.save(testProject);
        System.out.println("After second save, version: " + testProject.getVersion());

        // Then - version should be incremented
        assertEquals(originalVersion + 1, testProject.getVersion());

        Optional<ExpenseProject> found = expenseProjectGateway.getProjectById(testProject.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated Project", found.get().getName());
    }

    @Test
    void testDeleteProject() {
        // Given
        expenseProjectGateway.save(testProject);
        Integer projectId = testProject.getId();

        // Add expense records
        ExpenseRecord record1 = new ExpenseRecord();
        record1.setPayMember("Alice");
        record1.setAmount(new BigDecimal("100.00"));
        record1.setDate(new Date());
        record1.setExpenseType("餐饮");
        record1.addConsumer("Alice");
        testProject.addExpenseRecord(record1);
        expenseProjectGateway.save(testProject);

        // When
        expenseProjectGateway.delete(projectId);

        // Then - project should be deleted
        Optional<ExpenseProject> found = expenseProjectGateway.getProjectById(projectId);
        assertFalse(found.isPresent());
    }

    @Test
    void testGetProjectById() {
        // Given
        expenseProjectGateway.save(testProject);

        // When
        Optional<ExpenseProject> found = expenseProjectGateway.getProjectById(testProject.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals("Test Project", found.get().getName());
        assertEquals("Test Description", found.get().getDescription());
        assertEquals(1, found.get().getCreateUserId());
        assertFalse(found.get().getLocked());
        assertEquals(2, found.get().listAllMember().size());
    }

    @Test
    void testPageProject() {
        // Given - create multiple projects
        for (int i = 1; i <= 5; i++) {
            ExpenseProject project = new ExpenseProject();
            project.setName("Project " + i);
            project.setDescription("Description " + i);
            project.setCreateUserId(1);
            project.setLocked(false);
            project.setVersion(0);
            project.setChangingStatus(com.github.zavier.domain.common.ChangingStatus.NEW);
            project.setMemberChangingStatus(com.github.zavier.domain.common.ChangingStatus.UNCHANGED);
            project.setRecordChangingStatus(com.github.zavier.domain.common.ChangingStatus.UNCHANGED);
            expenseProjectGateway.save(project);
        }

        ProjectListQry query = new ProjectListQry();
        query.setPage(1);
        query.setSize(3);
        query.setOperatorId(1);

        // When
        PageResponse<ExpenseProject> response = expenseProjectGateway.pageProject(query);

        // Then
        assertNotNull(response);
        assertTrue(response.getData().size() <= 3);
    }

    @Test
    void testPageProjectWithNameFilter() {
        // Given
        expenseProjectGateway.save(testProject);

        ExpenseProject project2 = new ExpenseProject();
        project2.setName("Different Project");
        project2.setDescription("Different Description");
        project2.setCreateUserId(1);
        project2.setLocked(false);
        project2.setVersion(0);
        project2.setChangingStatus(com.github.zavier.domain.common.ChangingStatus.NEW);
        project2.setMemberChangingStatus(com.github.zavier.domain.common.ChangingStatus.UNCHANGED);
        project2.setRecordChangingStatus(com.github.zavier.domain.common.ChangingStatus.UNCHANGED);
        expenseProjectGateway.save(project2);

        ProjectListQry query = new ProjectListQry();
        query.setPage(1);
        query.setSize(10);
        query.setOperatorId(1);
        query.setName("Test");

        // When
        PageResponse<ExpenseProject> response = expenseProjectGateway.pageProject(query);

        // Then
        assertEquals(1, response.getData().size());
        assertEquals("Test Project", response.getData().get(0).getName());
    }

    @Test
    void testUpdateProjectMembers() {
        // Given
        expenseProjectGateway.save(testProject);
        assertEquals(2, testProject.listAllMember().size());

        // When - add new member
        testProject.addMember("Charlie");
        testProject.setMemberChangingStatus(com.github.zavier.domain.common.ChangingStatus.UPDATED);
        testProject.setChangingStatus(com.github.zavier.domain.common.ChangingStatus.UPDATED);
        expenseProjectGateway.save(testProject);

        // Then
        Optional<ExpenseProject> found = expenseProjectGateway.getProjectById(testProject.getId());
        assertTrue(found.isPresent());
        assertEquals(3, found.get().listAllMember().size());
        assertTrue(found.get().listAllMember().contains("Charlie"));
    }

    @Test
    void testDeleteExpenseRecords() {
        // Given
        expenseProjectGateway.save(testProject);

        ExpenseRecord record1 = new ExpenseRecord();
        record1.setPayMember("Alice");
        record1.setAmount(new BigDecimal("100.00"));
        record1.setDate(new Date());
        record1.setExpenseType("餐饮");
        record1.addConsumer("Alice");
        record1.addConsumer("Bob");
        testProject.addExpenseRecord(record1);

        ExpenseRecord record2 = new ExpenseRecord();
        record2.setPayMember("Bob");
        record2.setAmount(new BigDecimal("50.00"));
        record2.setDate(new Date());
        record2.setExpenseType("交通");
        record2.addConsumer("Alice");
        testProject.addExpenseRecord(record2);

        testProject.setRecordChangingStatus(com.github.zavier.domain.common.ChangingStatus.NEW);
        testProject.setChangingStatus(com.github.zavier.domain.common.ChangingStatus.UPDATED);
        expenseProjectGateway.save(testProject);
        assertEquals(2, testProject.listAllExpenseRecord().size());

        // When - remove one record
        Integer record1Id = testProject.listAllExpenseRecord().get(0).getId();
        testProject.removeRecord(record1Id);
        testProject.setRecordChangingStatus(com.github.zavier.domain.common.ChangingStatus.UPDATED);
        testProject.setChangingStatus(com.github.zavier.domain.common.ChangingStatus.UPDATED);
        expenseProjectGateway.save(testProject);

        // Then
        Optional<ExpenseProject> found = expenseProjectGateway.getProjectById(testProject.getId());
        assertTrue(found.isPresent());
        assertEquals(1, found.get().listAllExpenseRecord().size());
        assertEquals("交通", found.get().listAllExpenseRecord().get(0).getExpenseType());
    }
}
