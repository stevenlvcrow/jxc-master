package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreAdminRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.StoreAdminView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StoreAdminRelMapper extends BaseMapper<StoreAdminRelDO> {

    StoreAdminView selectAdminByStoreId(@Param("storeId") Long storeId);

    StoreAdminView selectStoreByAdminUserId(@Param("userId") Long userId);

    List<StoreAdminView> selectAllActiveBindings();
}

