package com.github.zavier.user;

import com.alibaba.cola.dto.PageResponse;
import com.github.zavier.converter.UserConverter;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserListQry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class UserGateWayImpl implements UserGateway {
    @Resource
    private UserRepository userRepository;


    @Override
    public Optional<User> getByUserName(@NotNull String username) {
        return userRepository.findByUserName(username)
                .map(UserConverter::toUser);
    }

    @Override
    public Optional<User> getUserById(@NotNull Integer userId) {
        return userRepository.findById(userId)
                .map(UserConverter::toUser);
    }

    @Override
    public Optional<User> getByEmail(@NotNull String email) {
        return userRepository.findByEmail(email)
                .map(UserConverter::toUser);
    }

    @Override
    public Optional<User> getByOpenId(@NotNull String openId) {
        return userRepository.findByOpenId(openId)
                .map(UserConverter::toUser);
    }

    @Override
    public User save(User user) {
        final UserDO userDO = new UserDO();
        userDO.setUserName(user.getUserName());
        userDO.setEmail(user.getEmail());
        userDO.setPasswordHash(user.getPasswordHash());
        userDO.setOpenId(user.getOpenId());
        userDO.setCreatedAt(new Date());
        userDO.setUpdatedAt(new Date());
        final UserDO saved = userRepository.save(userDO);
        user.setUserId(saved.getId());
        return user;
    }

    @Override
    public PageResponse<User> pageUser(UserListQry userListQry){
        // Build specification for dynamic query
        Specification<UserDO> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(userListQry.getUserName())) {
                predicates.add(cb.like(root.get("userName"), userListQry.getUserName() + "%"));
            }
            if (StringUtils.isNotBlank(userListQry.getEmail())) {
                predicates.add(cb.like(root.get("email"), userListQry.getEmail() + "%"));
            }
            if (userListQry.getUserId() != null) {
                predicates.add(cb.equal(root.get("id"), userListQry.getUserId()));
            }
            if (CollectionUtils.isNotEmpty(userListQry.getUserIdList())) {
                predicates.add(root.get("id").in(userListQry.getUserIdList()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Create pageable
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(userListQry.getPage() - 1, userListQry.getSize(), sort);

        // Execute query
        Page<UserDO> page = userRepository.findAll(spec, pageable);

        // Convert to domain objects
        final List<User> userList = page.stream()
                .map(UserConverter::toUser)
                .collect(Collectors.toList());

        return PageResponse.of(userList, (int) page.getTotalElements(), page.getSize(), userListQry.getPage());
    }
}
