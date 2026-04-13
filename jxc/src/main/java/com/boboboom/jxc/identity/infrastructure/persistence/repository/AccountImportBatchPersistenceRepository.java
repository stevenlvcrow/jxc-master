package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AccountImportBatchDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.AccountImportBatchMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountImportBatchPersistenceRepository extends AbstractMpRepository<AccountImportBatchDO> {

    private final AccountImportBatchMapper mapper;

    public AccountImportBatchPersistenceRepository(AccountImportBatchMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<AccountImportBatchDO> mapper() {
        return mapper;
    }
}

