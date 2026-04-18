package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.identity.domain.repository.UnitRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UnitAdministrationService {

    private static final String UNIT_CODE_PREFIX = "DWBM";

    private final UnitRepository unitRepository;
    private final BusinessCodeGenerator businessCodeGenerator;

    public UnitAdministrationService(UnitRepository unitRepository,
                                     BusinessCodeGenerator businessCodeGenerator) {
        this.unitRepository = unitRepository;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public List<UnitDO> listUnits(String scopeType,
                                  Long scopeId,
                                  String keyword,
                                  String status,
                                  String unitType) {
        List<UnitDO> rows = unitRepository.findByScope(scopeType, scopeId);
        String normalizedKeyword = trimNullable(keyword);
        String normalizedStatus = normalizeStatusNullable(status);
        String normalizedType = normalizeUnitTypeNullable(unitType);
        return rows.stream()
                .filter(row -> normalizedKeyword == null
                        || contains(row.getUnitCode(), normalizedKeyword)
                        || contains(row.getUnitName(), normalizedKeyword))
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .filter(row -> normalizedType == null || normalizedType.equals(row.getUnitType()))
                .toList();
    }

    @Transactional
    public UnitDO createUnit(String scopeType,
                             Long scopeId,
                             String unitCode,
                             String unitName,
                             String unitType,
                             String status,
                             String remark) {
        String resolvedUnitCode = trimNullable(unitCode);
        if (resolvedUnitCode == null) {
            resolvedUnitCode = generateUnitCode(scopeType, scopeId);
        }
        ensureCodeUnique(scopeType, scopeId, resolvedUnitCode, null);
        ensureNameUnique(scopeType, scopeId, unitName, null);
        UnitDO entity = new UnitDO();
        entity.setScopeType(scopeType);
        entity.setScopeId(scopeId);
        entity.setUnitCode(resolvedUnitCode);
        entity.setUnitName(unitName);
        entity.setUnitType(normalizeUnitType(unitType));
        entity.setStatus(normalizeStatus(status));
        entity.setRemark(trimNullable(remark));
        unitRepository.save(entity);
        return entity;
    }

    @Transactional
    public UnitDO updateUnit(Long id,
                             String scopeType,
                             Long scopeId,
                             String unitCode,
                             String unitName,
                             String unitType,
                             String status,
                             String remark) {
        UnitDO entity = requireUnit(id, scopeType, scopeId);
        ensureCodeUnique(scopeType, scopeId, unitCode, id);
        ensureNameUnique(scopeType, scopeId, unitName, id);
        entity.setUnitCode(unitCode);
        entity.setUnitName(unitName);
        entity.setUnitType(normalizeUnitType(unitType));
        entity.setStatus(normalizeStatus(status));
        entity.setRemark(trimNullable(remark));
        unitRepository.update(entity);
        return entity;
    }

    @Transactional
    public UnitDO updateUnitStatus(Long id, String scopeType, Long scopeId, String status) {
        UnitDO entity = requireUnit(id, scopeType, scopeId);
        entity.setStatus(normalizeStatus(status));
        unitRepository.update(entity);
        return entity;
    }

    @Transactional
    public void deleteUnit(Long id, String scopeType, Long scopeId) {
        requireUnit(id, scopeType, scopeId);
        unitRepository.deleteByIdAndScope(id, scopeType, scopeId);
    }

    public UnitDO requireUnit(Long id, String scopeType, Long scopeId) {
        return unitRepository.findByIdAndScope(id, scopeType, scopeId)
                .orElseThrow(() -> new BusinessException("单位不存在"));
    }

    private void ensureCodeUnique(String scopeType, Long scopeId, String unitCode, Long excludedId) {
        UnitDO exists = unitRepository.findByScopeAndUnitCode(scopeType, scopeId, unitCode)
                .filter(row -> excludedId == null || !excludedId.equals(row.getId()))
                .orElse(null);
        if (exists != null) {
            throw new BusinessException("单位编码已存在");
        }
    }

    private void ensureNameUnique(String scopeType, Long scopeId, String unitName, Long excludedId) {
        UnitDO exists = unitRepository.findByScopeAndUnitName(scopeType, scopeId, unitName)
                .filter(row -> excludedId == null || !excludedId.equals(row.getId()))
                .orElse(null);
        if (exists != null) {
            throw new BusinessException("单位名称已存在");
        }
    }

    private String normalizeStatusNullable(String value) {
        String status = trimNullable(value);
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return normalizeStatus(status);
    }

    private String normalizeStatus(String value) {
        String status = trimNullable(value);
        if (status == null || status.isEmpty()) {
            return "ENABLED";
        }
        if ("ENABLED".equalsIgnoreCase(status)) {
            return "ENABLED";
        }
        if ("DISABLED".equalsIgnoreCase(status)) {
            return "DISABLED";
        }
        throw new BusinessException("状态参数非法");
    }

    private String normalizeUnitTypeNullable(String value) {
        String unitType = trimNullable(value);
        if (unitType == null || "ALL".equalsIgnoreCase(unitType)) {
            return null;
        }
        return normalizeUnitType(unitType);
    }

    private String normalizeUnitType(String value) {
        String unitType = trimNullable(value);
        if (unitType == null || unitType.isEmpty()) {
            return "STANDARD";
        }
        if ("STANDARD".equalsIgnoreCase(unitType)) {
            return "STANDARD";
        }
        if ("AUXILIARY".equalsIgnoreCase(unitType)) {
            return "AUXILIARY";
        }
        throw new BusinessException("单位类型参数非法");
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateUnitCode(String scopeType, Long scopeId) {
        List<String> existingCodes = unitRepository.findByScope(scopeType, scopeId).stream()
                .map(UnitDO::getUnitCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.nextCode(UNIT_CODE_PREFIX, existingCodes);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }
}
