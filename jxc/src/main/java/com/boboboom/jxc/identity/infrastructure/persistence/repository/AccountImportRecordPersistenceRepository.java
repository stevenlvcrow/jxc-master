package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AccountImportRecordDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.AccountImportRecordMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class AccountImportRecordPersistenceRepository extends AbstractMpRepository<AccountImportRecordDO> {

    private final AccountImportRecordMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public AccountImportRecordPersistenceRepository(AccountImportRecordMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<AccountImportRecordDO> mapper() {
        return mapper;
    }
}

