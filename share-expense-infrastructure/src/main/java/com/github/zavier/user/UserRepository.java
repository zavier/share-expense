package com.github.zavier.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserDO, Integer>,
        JpaSpecificationExecutor<UserDO> {

    Optional<UserDO> findByUserName(String userName);

    Optional<UserDO> findByEmail(String email);

    Optional<UserDO> findByOpenId(String openId);
}
