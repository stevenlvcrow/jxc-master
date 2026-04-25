package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
public abstract class AbstractMpRepository<T> {

    protected abstract BaseMapper<T> mapper();

    /** 保存业务数据。 */
    public T save(T dataObject) {
        Object id = ReflectionIdAccessor.getIdValue(dataObject);
        if (id == null) {
            mapper().insert(dataObject);
        } else {
            mapper().updateById(dataObject);
        }
        return dataObject;
    }

    /** 按主键查询记录。 */
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(mapper().selectById(id));
    }

    /** 查询全部记录。 */
    public List<T> findAll() {
        return mapper().selectList(null);
    }

    /** 查询By。 */
    public List<T> findBy(Wrapper<T> wrapper) {
        return mapper().selectList(wrapper);
    }

    /** 按主键删除记录。 */
    public void deleteById(Long id) {
        mapper().deleteById(id);
    }
}

