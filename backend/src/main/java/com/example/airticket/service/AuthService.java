package com.example.airticket.service;

import com.example.airticket.dto.request.LoginRequest;
import com.example.airticket.dto.request.RegisterRequest;
import com.example.airticket.entity.User;
import com.example.airticket.enums.MemberLevel;
import com.example.airticket.enums.UserType;
import com.example.airticket.exception.BusinessException;
import com.example.airticket.repository.UserRepository;
import com.example.airticket.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern ID_NUMBER_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$");

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User register(RegisterRequest request) {
        requireText(request.loginAccount, 45005, "登录账号不能为空");
        requireText(request.password, 45003, "密码不能为空");
        requireText(request.userName, 45004, "用户名不能为空");
        if (request.password.trim().length() < 6) {
            throw new BusinessException(45006, "密码长度不能少于6位");
        }
        validatePhone(request.phoneNumber);
        validateEmail(request.email);
        String idNumber = firstNonBlank(request.idNumber, request.idNumberDigest);
        requireText(idNumber, 45002, "身份证号不能为空");
        validateIdNumber(idNumber);
        userRepository.findByLoginAccount(request.loginAccount).ifPresent(user -> {
            throw new BusinessException(40901, "账号已存在");
        });
        String digest = SecurityUtil.sha256Digest(idNumber);
        userRepository.findByIdNumberDigest(digest).ifPresent(user -> {
            throw new BusinessException(40904, "身份证号已存在");
        });
        User user = new User();
        user.loginAccount = request.loginAccount;
        user.passwordHash = SecurityUtil.hashPassword(request.password);
        user.userName = request.userName;
        user.phoneNumber = request.phoneNumber;
        user.email = request.email;
        user.idNumberDigest = digest;
        user.userType = UserType.PASSENGER;
        user.memberLevel = MemberLevel.NORMAL;
        user.points = 0;
        User saved = userRepository.save(user);
        log.info("auth.register userId={} loginAccount={} userType={} idDigestPrefix={}",
                saved.userId, saved.loginAccount, saved.userType, safeDigestPrefix(saved.idNumberDigest));
        return saved;
    }

    public User login(LoginRequest request) {
        requireText(request.loginAccount, 45005, "登录账号不能为空");
        requireText(request.password, 45003, "密码不能为空");
        User user = userRepository.findByLoginAccount(request.loginAccount)
                .orElseThrow(() -> new BusinessException(40102, "用户名或密码错误"));
        if (!SecurityUtil.matchesPassword(request.password, user.passwordHash)) {
            log.warn("auth.loginFailed loginAccount={} reason=password_mismatch", request.loginAccount);
            throw new BusinessException(40102, "用户名或密码错误");
        }
        log.info("auth.loginSuccess userId={} loginAccount={} userType={}", user.userId, user.loginAccount, user.userType);
        return user;
    }

    @Transactional
    public User cancelAccount(Integer userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        if (user.userType == UserType.ADMIN) {
            throw new BusinessException(45010, "管理员账号不允许注销");
        }
        String suffix = user.userId + "_" + System.currentTimeMillis();
        user.loginAccount = "cancelled_" + suffix;
        user.userName = "已注销用户" + user.userId;
        user.phoneNumber = null;
        user.email = null;
        user.points = 0;
        user.memberLevel = MemberLevel.NORMAL;
        user.idNumberDigest = SecurityUtil.sha256Digest("cancelled-id-" + suffix);
        user.passwordHash = SecurityUtil.hashPassword("cancelled-password-" + suffix);
        log.info("auth.cancelAccount userId={}", userId);
        return user;
    }

    private void requireText(String value, int code, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(code, message);
        }
    }

    private void validatePhone(String phoneNumber) {
        requireText(phoneNumber, 45007, "手机号不能为空");
        if (!PHONE_PATTERN.matcher(phoneNumber.trim()).matches()) {
            throw new BusinessException(45008, "手机号必须是11位数字");
        }
    }

    private void validateEmail(String email) {
        requireText(email, 45009, "邮箱不能为空");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessException(45011, "邮箱格式不正确");
        }
    }

    private void validateIdNumber(String idNumber) {
        if (!ID_NUMBER_PATTERN.matcher(idNumber.trim()).matches()) {
            throw new BusinessException(45012, "身份证号格式不正确");
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }

    private String safeDigestPrefix(String digest) {
        if (digest == null || digest.length() < 8) {
            return "null";
        }
        return digest.substring(0, 8);
    }
}
