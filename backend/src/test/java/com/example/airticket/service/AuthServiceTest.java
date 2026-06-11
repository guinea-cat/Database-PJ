package com.example.airticket.service;

import com.example.airticket.dto.request.RegisterRequest;
import com.example.airticket.entity.User;
import com.example.airticket.enums.MemberLevel;
import com.example.airticket.enums.UserType;
import com.example.airticket.exception.BusinessException;
import com.example.airticket.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authService = new AuthService(userRepository);
    }

    @Test
    void registerRejectsInvalidPhoneEmailAndIdNumber() {
        RegisterRequest invalidPhone = validRegisterRequest();
        invalidPhone.phoneNumber = "12345";

        assertThatThrownBy(() -> authService.register(invalidPhone))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号必须是11位数字");

        RegisterRequest invalidEmail = validRegisterRequest();
        invalidEmail.email = "bad-mail";
        assertThatThrownBy(() -> authService.register(invalidEmail))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱格式不正确");

        RegisterRequest invalidIdNumber = validRegisterRequest();
        invalidIdNumber.idNumber = "123";
        assertThatThrownBy(() -> authService.register(invalidIdNumber))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("身份证号格式不正确");
    }

    @Test
    void registerRejectsShortPassword() {
        RegisterRequest request = validRegisterRequest();
        request.password = "12345";

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码长度不能少于6位");
    }

    @Test
    void cancelPassengerAccountAnonymizesExistingUserWithoutDeletingRow() {
        User user = new User();
        user.userId = 2;
        user.loginAccount = "passengerA";
        user.userName = "演示乘客A";
        user.userType = UserType.PASSENGER;
        user.memberLevel = MemberLevel.VIP;
        user.points = 1000;
        user.phoneNumber = "13800000001";
        user.email = "passengerA@example.com";
        user.idNumberDigest = "old-digest";
        user.passwordHash = "old-hash";

        when(userRepository.findByIdForUpdate(2)).thenReturn(Optional.of(user));

        User cancelled = authService.cancelAccount(2);

        assertThat(cancelled.loginAccount).startsWith("cancelled_2_");
        assertThat(cancelled.userName).isEqualTo("已注销用户2");
        assertThat(cancelled.phoneNumber).isNull();
        assertThat(cancelled.email).isNull();
        assertThat(cancelled.points).isZero();
        assertThat(cancelled.memberLevel).isEqualTo(MemberLevel.NORMAL);
        assertThat(cancelled.idNumberDigest).hasSize(64);
        assertThat(cancelled.passwordHash).isNotEqualTo("old-hash");
    }

    @Test
    void cancelAdminAccountIsRejected() {
        User admin = new User();
        admin.userId = 1;
        admin.userType = UserType.ADMIN;
        when(userRepository.findByIdForUpdate(1)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.cancelAccount(1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("管理员账号不允许注销");
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.loginAccount = "newPassenger";
        request.password = "pass123";
        request.userName = "新乘客";
        request.phoneNumber = "13900000000";
        request.email = "new@example.com";
        request.idNumber = "110101199001010011";
        when(userRepository.findByLoginAccount(request.loginAccount)).thenReturn(Optional.empty());
        when(userRepository.findByIdNumberDigest(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return request;
    }
}
