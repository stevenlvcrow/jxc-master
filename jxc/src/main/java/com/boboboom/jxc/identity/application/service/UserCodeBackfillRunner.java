package com.boboboom.jxc.identity.application.service;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;

/** 身份与权限启动任务，负责应用启动后的初始化处理。 */
@Component
public class UserCodeBackfillRunner implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final UserCodeGenerator userCodeGenerator;

    /** 身份与权限启动任务，负责应用启动后的初始化处理。 */
    public UserCodeBackfillRunner(UserAccountRepository userAccountRepositoryValue,
                                  UserCodeGenerator userCodeGeneratorValue) {
        this.userAccountRepository = userAccountRepositoryValue;
        this.userCodeGenerator = userCodeGeneratorValue;
    }

    /** 应用启动后执行初始化任务。 */
    @Override
    public void run(ApplicationArguments args) {
        List<UserAccountDO> users = userAccountRepository.findAllOrdered();
        for (UserAccountDO user : users) {
            if (!shouldBackfill(user)) {
                continue;
            }
            String generatedCode = userCodeGenerator.generate(user.getRealName(), user.getPhone());
            if (generatedCode.isBlank() || generatedCode.equals(user.getUsername())) {
                continue;
            }
            user.setUsername(generatedCode);
            userAccountRepository.update(user);
        }
    }

    private boolean shouldBackfill(UserAccountDO user) {
        if (user == null || user.getPhone() == null || user.getPhone().isBlank()) {
            return false;
        }
        String username = user.getUsername();
        if (username == null || username.isBlank()) {
            return true;
        }
        if ("admin".equalsIgnoreCase(username.trim())) {
            return false;
        }
        return username.trim().equals(user.getPhone().trim());
    }
}
