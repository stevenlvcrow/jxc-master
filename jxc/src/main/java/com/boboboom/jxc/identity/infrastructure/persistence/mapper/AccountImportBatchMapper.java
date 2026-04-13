package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AccountImportBatchDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AccountImportBatchMapper extends BaseMapper<AccountImportBatchDO> {

    List<AccountImportBatchDO> selectRecentBatches(@Param("sourceSystem") String sourceSystem,
                                                   @Param("limit") Integer limit);
}

