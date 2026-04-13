package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.StoreAdminView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StoreMapper extends BaseMapper<StoreDO> {

    List<StoreAdminView> selectStoreAdminViewByGroupId(@Param("groupId") Long groupId,
                                                       @Param("status") String status);
}

