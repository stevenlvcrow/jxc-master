package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AccountImportBatchDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.AccountImportBatchMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class AccountImportBatchPersistenceRepository extends AbstractMpRepository<AccountImportBatchDO> {

    private final AccountImportBatchMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public AccountImportBatchPersistenceRepository(AccountImportBatchMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<AccountImportBatchDO> mapper() {
        return mapper;
    }
}

