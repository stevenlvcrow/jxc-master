package com.boboboom.jxc.cost.infrastructure.persistence.repository;

import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardDetail;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardHeader;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardLine;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardVersion;
import com.boboboom.jxc.cost.domain.repository.CostCardRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/**
 * 成本卡 JDBC 仓储实现。
 */
@Repository
public class CostCardRepositoryImpl implements CostCardRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public CostCardRepositoryImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public List<CostCardHeader> findCards(String scopeType, Long scopeId) {
        return namedParameterJdbcTemplate.query("""
                SELECT *
                FROM dev.cost_card
                WHERE scope_type = :scopeType AND scope_id = :scopeId
                ORDER BY updated_at DESC, id DESC
                """, scopeParams(scopeType, scopeId), (rs, rowNum) -> mapHeader(rs));
    }

    @Override
    public Optional<CostCardDetail> findDetail(String scopeType, Long scopeId, Long id) {
        List<CostCardHeader> headers = namedParameterJdbcTemplate.query("""
                SELECT *
                FROM dev.cost_card
                WHERE scope_type = :scopeType AND scope_id = :scopeId AND id = :id
                """, scopeParams(scopeType, scopeId).addValue("id", id), (rs, rowNum) -> mapHeader(rs));
        if (headers.isEmpty()) {
            return Optional.empty();
        }
        CostCardHeader header = headers.get(0);
        List<CostCardVersion> versions = namedParameterJdbcTemplate.query("""
                SELECT *
                FROM dev.cost_card_version
                WHERE card_id = :cardId
                ORDER BY version_no DESC
                """, new MapSqlParameterSource("cardId", header.id()), (rs, rowNum) -> mapVersion(rs));
        return Optional.of(new CostCardDetail(
                header.id(),
                header.scopeType(),
                header.scopeId(),
                header.cardCode(),
                header.cardName(),
                header.cardType(),
                header.status(),
                header.effectiveVersionId(),
                header.linkedDishCount(),
                header.remark(),
                header.createdBy(),
                versions
        ));
    }

    @Override
    public long countByCodePrefix(String scopeType, Long scopeId, String prefix) {
        Long count = namedParameterJdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM dev.cost_card
                WHERE scope_type = :scopeType AND scope_id = :scopeId AND card_code LIKE :prefix
                """, scopeParams(scopeType, scopeId).addValue("prefix", prefix + "%"), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public Long saveCard(CostCardHeader header) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update("""
                INSERT INTO dev.cost_card
                (scope_type, scope_id, card_code, card_name, card_type, status, effective_version_id,
                 linked_dish_count, remark, created_by, created_at, updated_at)
                VALUES (:scopeType, :scopeId, :cardCode, :cardName, :cardType, :status, :effectiveVersionId,
                 :linkedDishCount, :remark, :createdBy, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, headerParams(header), keyHolder, new String[]{"id"});
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    @Override
    public void updateCard(CostCardHeader header) {
        namedParameterJdbcTemplate.update("""
                UPDATE dev.cost_card
                SET card_name = :cardName,
                    card_type = :cardType,
                    remark = :remark,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, headerParams(header));
    }

    @Override
    public Long saveVersion(CostCardVersion version) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update("""
                INSERT INTO dev.cost_card_version
                (card_id, version_no, effective_date, version_status, referenced, remark, created_at, updated_at)
                VALUES (:cardId, :versionNo, :effectiveDate, :versionStatus, :referenced, :remark,
                 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, versionParams(version), keyHolder, new String[]{"id"});
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    @Override
    public void saveLines(Long versionId, List<CostCardLine> lines) {
        for (CostCardLine line : lines) {
            namedParameterJdbcTemplate.update("""
                    INSERT INTO dev.cost_card_line
                    (version_id, item_code, item_name, unit_name, quantity, conversion_rate, loss_rate,
                     default_warehouse, remark, created_at)
                    VALUES (:versionId, :itemCode, :itemName, :unitName, :quantity, :conversionRate,
                     :lossRate, :defaultWarehouse, :remark, CURRENT_TIMESTAMP)
                    """, lineParams(versionId, line));
        }
    }

    @Override
    public void activateVersion(Long cardId, Long versionId) {
        namedParameterJdbcTemplate.update("""
                UPDATE dev.cost_card_version
                SET version_status = CASE WHEN id = :versionId THEN 'ACTIVE' ELSE 'DISABLED' END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE card_id = :cardId
                """, new MapSqlParameterSource().addValue("cardId", cardId).addValue("versionId", versionId));
        namedParameterJdbcTemplate.update("""
                UPDATE dev.cost_card
                SET status = 'ENABLED',
                    effective_version_id = :versionId,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :cardId
                """, new MapSqlParameterSource().addValue("cardId", cardId).addValue("versionId", versionId));
    }

    @Override
    public void disableCard(Long cardId) {
        namedParameterJdbcTemplate.update("""
                UPDATE dev.cost_card
                SET status = 'DISABLED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :cardId
                """, new MapSqlParameterSource("cardId", cardId));
    }

    @Override
    public void bindDish(String scopeType, Long scopeId, String dishId, Long cardId, String defaultWarehouse, boolean enabled) {
        namedParameterJdbcTemplate.update("""
                INSERT INTO dev.dish_cost_card_binding
                (scope_type, scope_id, dish_id, card_id, default_warehouse, enabled, created_at, updated_at)
                VALUES (:scopeType, :scopeId, :dishId, :cardId, :defaultWarehouse, :enabled,
                 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (scope_type, scope_id, dish_id)
                DO UPDATE SET card_id = EXCLUDED.card_id,
                              default_warehouse = EXCLUDED.default_warehouse,
                              enabled = EXCLUDED.enabled,
                              updated_at = CURRENT_TIMESTAMP
                """, scopeParams(scopeType, scopeId)
                .addValue("dishId", dishId)
                .addValue("cardId", cardId)
                .addValue("defaultWarehouse", defaultWarehouse)
                .addValue("enabled", enabled));
    }

    private MapSqlParameterSource scopeParams(String scopeType, Long scopeId) {
        return new MapSqlParameterSource()
                .addValue("scopeType", scopeType)
                .addValue("scopeId", scopeId);
    }

    private MapSqlParameterSource headerParams(CostCardHeader header) {
        return new MapSqlParameterSource()
                .addValue("id", header.id())
                .addValue("scopeType", header.scopeType())
                .addValue("scopeId", header.scopeId())
                .addValue("cardCode", header.cardCode())
                .addValue("cardName", header.cardName())
                .addValue("cardType", header.cardType())
                .addValue("status", header.status())
                .addValue("effectiveVersionId", header.effectiveVersionId(), Types.BIGINT)
                .addValue("linkedDishCount", header.linkedDishCount())
                .addValue("remark", header.remark(), Types.VARCHAR)
                .addValue("createdBy", header.createdBy(), Types.BIGINT);
    }

    private MapSqlParameterSource versionParams(CostCardVersion version) {
        return new MapSqlParameterSource()
                .addValue("cardId", version.cardId())
                .addValue("versionNo", version.versionNo())
                .addValue("effectiveDate", version.effectiveDate())
                .addValue("versionStatus", version.versionStatus())
                .addValue("referenced", version.referenced())
                .addValue("remark", version.remark(), Types.VARCHAR);
    }

    private MapSqlParameterSource lineParams(Long versionId, CostCardLine line) {
        return new MapSqlParameterSource()
                .addValue("versionId", versionId)
                .addValue("itemCode", line.itemCode())
                .addValue("itemName", line.itemName())
                .addValue("unitName", line.unitName())
                .addValue("quantity", line.quantity())
                .addValue("conversionRate", line.conversionRate())
                .addValue("lossRate", line.lossRate())
                .addValue("defaultWarehouse", line.defaultWarehouse())
                .addValue("remark", line.remark(), Types.VARCHAR);
    }

    private CostCardHeader mapHeader(ResultSet rs) throws SQLException {
        return new CostCardHeader(
                rs.getLong("id"),
                rs.getString("scope_type"),
                rs.getLong("scope_id"),
                rs.getString("card_code"),
                rs.getString("card_name"),
                rs.getString("card_type"),
                rs.getString("status"),
                rs.getObject("effective_version_id", Long.class),
                rs.getInt("linked_dish_count"),
                rs.getString("remark"),
                rs.getObject("created_by", Long.class)
        );
    }

    private CostCardVersion mapVersion(ResultSet rs) throws SQLException {
        return new CostCardVersion(
                rs.getLong("id"),
                rs.getLong("card_id"),
                rs.getInt("version_no"),
                rs.getObject("effective_date", java.time.LocalDate.class),
                rs.getString("version_status"),
                rs.getBoolean("referenced"),
                rs.getString("remark")
        );
    }
}
