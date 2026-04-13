package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AccountImportRecordDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.ImportRecordView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AccountImportRecordMapper extends BaseMapper<AccountImportRecordDO> {

    List<ImportRecordView> selectRecordViewsByBatchId(@Param("batchId") Long batchId);
}

