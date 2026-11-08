package com.yunshu.zhixue.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内置管理员初始化：schema.sql 中种子密码为占位符，
 * 应用首次启动时按 {@code app.default-password} 生成 BCrypt 哈希并写入。
 *
 * <p>这样避免把可用密码哈希固化在 SQL 脚本中；生产部署可通过环境变量覆盖默认密码。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.admin-init", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminAccountInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-password}")
    private String defaultPassword;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> hashes = jdbcTemplate.queryForList(
                    "SELECT password_hash FROM admin_user WHERE is_builtin = 1 AND delete_flag = 0 LIMIT 1",
                    String.class);
            if (hashes.isEmpty()) {
                log.warn("未找到内置管理员账号，跳过内置账号初始化");
                return;
            }
            String currentHash = hashes.get(0);
            if (currentHash != null && currentHash.startsWith("$2")) {
                return;
            }
            String encoded = passwordEncoder.encode(defaultPassword);
            jdbcTemplate.update(
                    "UPDATE admin_user SET password_hash = ? WHERE is_builtin = 1 AND delete_flag = 0",
                    encoded);
            log.warn("内置管理员密码已初始化为应用默认密码，请登录后立即修改。");
        } catch (DataAccessException ex) {
            log.warn("数据库不可用，跳过内置管理员初始化：{}", ex.getMostSpecificCause().getMessage());
        }
    }
}
