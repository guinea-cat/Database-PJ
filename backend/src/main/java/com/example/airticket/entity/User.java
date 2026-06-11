package com.example.airticket.entity;

import com.example.airticket.enums.MemberLevel;
import com.example.airticket.enums.UserType;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "`User`")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserId")
    public Integer userId;

    @Column(name = "LoginAccount", nullable = false, unique = true)
    public String loginAccount;

    @Column(name = "UserName", nullable = false)
    public String userName;

    @Column(name = "IdNumberDigest", nullable = false, unique = true, length = 64)
    public String idNumberDigest;

    @Column(name = "PasswordHash", nullable = false)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "UserType", nullable = false)
    public UserType userType;

    @Column(name = "PhoneNumber")
    public String phoneNumber;

    @Column(name = "Email")
    public String email;

    @Column(name = "Points", nullable = false)
    public Integer points = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "MemberLevel", nullable = false)
    public MemberLevel memberLevel = MemberLevel.NORMAL;

    @Column(name = "CreatedAt", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (points == null) {
            points = 0;
        }
        if (memberLevel == null) {
            memberLevel = MemberLevel.NORMAL;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
