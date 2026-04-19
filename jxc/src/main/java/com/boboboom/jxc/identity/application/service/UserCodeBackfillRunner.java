package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserCodeBackfillRunner implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final UserCodeGenerator userCodeGenerator;

    public UserCodeBackfillRunner(UserAccountRepository userAccountRepository,
                                  UserCodeGenerator userCodeGenerator) {
        this.userAccountRepository = userAccountRepository;
        this.userCodeGenerator = userCodeGenerator;
    }

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
