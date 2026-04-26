package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.GroupMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class GroupRepositoryImpl implements GroupRepository {

    private final GroupMapper groupMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public GroupRepositoryImpl(GroupMapper groupMapperValue) {
        this.groupMapper = groupMapperValue;
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<GroupDO> findById(Long id) {
        return Optional.ofNullable(groupMapper.selectById(id));
    }

    /** 查询By集团编码。 */
    @Override
    public Optional<GroupDO> findByGroupCode(String groupCode) {
        return groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                .eq(GroupDO::getGroupCode, groupCode)
                .orderByDesc(GroupDO::getId))
                .stream()
                .findFirst();
    }

    /** 查询All排序。 */
    @Override
    public List<GroupDO> findAllOrdered() {
        return groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                .orderByDesc(GroupDO::getCreatedAt)
                .orderByDesc(GroupDO::getId));
    }

    /** 查询By标识排序。 */
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

    /** 查询All集团Codes。 */
    @Override
    public List<String> findAllGroupCodes() {
        return groupMapper.selectList(new LambdaQueryWrapper<GroupDO>()
                        .select(GroupDO::getGroupCode))
                .stream()
                .map(GroupDO::getGroupCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    /** 查询Active集团门店Summaries。 */
    @Override
    public List<GroupStoreSummary> findActiveGroupStoreSummaries(String status) {
        return groupMapper.selectGroupStoreSummary(status);
    }

    /** 保存业务数据。 */
    @Override
    public void save(GroupDO group) {
        groupMapper.insert(group);
    }

    /** 更新业务记录。 */
    @Override
    public void update(GroupDO group) {
        groupMapper.updateById(group);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        groupMapper.deleteById(id);
    }
}
