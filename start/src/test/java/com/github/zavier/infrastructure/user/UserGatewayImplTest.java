package com.github.zavier.infrastructure.user;

import com.alibaba.cola.dto.PageResponse;
import com.github.zavier.Application;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserListQry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Rollback
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
public class UserGatewayImplTest {

    @Autowired
    private UserGateway userGateway;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
    }

    @Test
    void testSaveUser() {
        // When
        User saved = userGateway.save(testUser);

        // Then
        assertNotNull(saved.getUserId());
        assertEquals("testuser", saved.getUserName());

        // Verify can be retrieved
        Optional<User> found = userGateway.getUserById(saved.getUserId());
        assertTrue(found.isPresent());
    }

    @Test
    void testGetByUserName() {
        // Given
        User saved = userGateway.save(testUser);

        // When
        Optional<User> found = userGateway.getByUserName("testuser");

        // Then
        assertTrue(found.isPresent());
        assertEquals(saved.getUserId(), found.get().getUserId());
    }

    @Test
    void testGetByEmail() {
        // Given
        userGateway.save(testUser);

        // When
        Optional<User> found = userGateway.getByEmail("test@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUserName());
    }

    @Test
    void testGetByOpenId() {
        // Given
        testUser.setOpenId("openid123");
        userGateway.save(testUser);

        // When
        Optional<User> found = userGateway.getByOpenId("openid123");

        // Then
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUserName());
    }

    @Test
    void testPageUser() {
        // Given - create multiple users
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setUserName("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPasswordHash("password" + i);
            userGateway.save(user);
        }

        UserListQry query = new UserListQry();
        query.setPage(1);
        query.setSize(3);

        // When
        PageResponse<User> response = userGateway.pageUser(query);

        // Then
        assertNotNull(response);
        assertTrue(response.getData().size() <= 3);
    }

    @Test
    void testPageUserWithNameFilter() {
        // Given
        User user1 = new User();
        user1.setUserName("alice");
        user1.setEmail("alice@example.com");
        user1.setPasswordHash("password1");
        userGateway.save(user1);

        User user2 = new User();
        user2.setUserName("bob");
        user2.setEmail("bob@example.com");
        user2.setPasswordHash("password2");
        userGateway.save(user2);

        User user3 = new User();
        user3.setUserName("alice2");
        user3.setEmail("alice2@example.com");
        user3.setPasswordHash("password3");
        userGateway.save(user3);

        UserListQry query = new UserListQry();
        query.setPage(1);
        query.setSize(10);
        query.setUserName("alice");

        // When
        PageResponse<User> response = userGateway.pageUser(query);

        // Then
        assertEquals(2, response.getData().size());
        assertTrue(response.getData().stream().allMatch(u -> u.getUserName().startsWith("alice")));
    }

    @Test
    void testPageUserWithUserIdListFilter() {
        // Given
        User user1 = new User();
        user1.setUserName("user1");
        user1.setEmail("user1@example.com");
        user1.setPasswordHash("password1");
        userGateway.save(user1);

        User user2 = new User();
        user2.setUserName("user2");
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("password2");
        userGateway.save(user2);

        UserListQry query = new UserListQry();
        query.setPage(1);
        query.setSize(10);
        query.setUserIdList(Arrays.asList(user1.getUserId(), user2.getUserId()));

        // When
        PageResponse<User> response = userGateway.pageUser(query);

        // Then
        assertEquals(2, response.getData().size());
    }
}
