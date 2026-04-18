package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.GroupMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupRepositoryImpl implements GroupRepository {

    private final GroupMapper groupMapper;

    public GroupRepositoryImpl(GroupMapper groupMapper) {
        this.groupMapper = groupMapper;
    }

    @Override
    public Optional<GroupDO> findById(Long id) {
        return Optional.ofNullable(groupMapper.selectById(id));
    }

    @Override
    public Optional<GroupDO> findByGroupCode(String groupCode) {
        return groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                .eq(GroupDO::getGroupCode, groupCode)
                .orderByDesc(GroupDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public List<GroupDO> findAllOrdered() {
        return groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                .orderByDesc(GroupDO::getCreatedAt)
                .orderByDesc(GroupDO::getId));
    }

    @Override
    public List<GroupDO> findByIdsOrdered(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                .in(GroupDO::getId, ids)
                .orderByDesc(GroupDO::getCreatedAt)
                .orderByDesc(GroupDO::getId));
    }

    @Override
    public List<String> findAllGroupCodes() {
        return groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                        .select(GroupDO::getGroupCode))
                .stream()
                .map(GroupDO::getGroupCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    @Override
    public List<GroupStoreSummary> findActiveGroupStoreSummaries(String status) {
        return groupMapper.selectGroupStoreSummary(status);
    }

    @Override
    public void save(GroupDO group) {
        groupMapper.insert(group);
    }

    @Override
    public void update(GroupDO group) {
        groupMapper.updateById(group);
    }

    @Override
    public void deleteById(Long id) {
        groupMapper.deleteById(id);
    }
}
