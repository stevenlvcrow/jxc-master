package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.AccountImportRecordRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AccountImportRecordDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.AccountImportRecordMapper;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class AccountImportRecordRepositoryImpl implements AccountImportRecordRepository {

    private final AccountImportRecordMapper accountImportRecordMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public AccountImportRecordRepositoryImpl(AccountImportRecordMapper accountImportRecordMapperValue) {
        this.accountImportRecordMapper = accountImportRecordMapperValue;
    }

    /** 删除By用户标识。 */
    @Override
    public void deleteByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        accountImportRecordMapper.delete(new LambdaQueryWrapper<AccountImportRecordDO>()
                .eq(AccountImportRecordDO::getUserId, userId));
    }
}
