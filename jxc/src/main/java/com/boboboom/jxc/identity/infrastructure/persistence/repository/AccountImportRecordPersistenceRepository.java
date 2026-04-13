package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AccountImportRecordDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.AccountImportRecordMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountImportRecordPersistenceRepository extends AbstractMpRepository<AccountImportRecordDO> {

    private final AccountImportRecordMapper mapper;

    public AccountImportRecordPersistenceRepository(AccountImportRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<AccountImportRecordDO> mapper() {
        return mapper;
    }
}

