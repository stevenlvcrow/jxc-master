package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.AccountImportRecordRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AccountImportRecordDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.AccountImportRecordMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountImportRecordRepositoryImpl implements AccountImportRecordRepository {

    private final AccountImportRecordMapper accountImportRecordMapper;

    public AccountImportRecordRepositoryImpl(AccountImportRecordMapper accountImportRecordMapper) {
        this.accountImportRecordMapper = accountImportRecordMapper;
    }

    @Override
    public void deleteByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        accountImportRecordMapper.delete(new LambdaQueryWrapper<AccountImportRecordDO>()
                .eq(AccountImportRecordDO::getUserId, userId));
    }
}
