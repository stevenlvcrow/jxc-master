package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Optional;

public abstract class AbstractMpRepository<T> {

    protected abstract BaseMapper<T> mapper();

    public T save(T dataObject) {
        Object id = ReflectionIdAccessor.getIdValue(dataObject);
        if (id == null) {
            mapper().insert(dataObject);
        } else {
            mapper().updateById(dataObject);
        }
        return dataObject;
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(mapper().selectById(id));
    }

    public List<T> findAll() {
        return mapper().selectList(null);
    }

    public List<T> findBy(Wrapper<T> wrapper) {
        return mapper().selectList(wrapper);
    }

    public void deleteById(Long id) {
        mapper().deleteById(id);
    }
}

