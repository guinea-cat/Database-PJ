package com.example.airticket.repository;

import com.example.airticket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByLoginAccount(String loginAccount);

    Optional<User> findByIdNumberDigest(String idNumberDigest);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = ?1")
    Optional<User> findByIdForUpdate(Integer userId);
}
