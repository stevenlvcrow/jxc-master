package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import com.boboboom.jxc.inventory.application.service.InventoryDocumentHeader;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentLine;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentType;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通用库存单据 JDBC 仓储实现。
 */
@Repository
public class InventoryDocumentRepositoryImpl implements InventoryDocumentRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public InventoryDocumentRepositoryImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public List<InventoryDocumentHeader> findHeadersByScopeOrdered(InventoryDocumentType type, String scopeType, Long scopeId) {
        return namedParameterJdbcTemplate.query(
                "SELECT * FROM dev." + type.headerTable()
                        + " WHERE scope_type = :scopeType AND scope_id = :scopeId"
                        + " ORDER BY created_at DESC, id DESC",
                new MapSqlParameterSource()
                        .addValue("scopeType", scopeType)
                        .addValue("scopeId", scopeId),
                (rs, rowNum) -> mapHeader(rs)
        );
    }

    @Override
    public List<InventoryDocumentHeader> findHeadersByScopeAndIds(InventoryDocumentType type,
                                                                  String scopeType,
                                                                  Long scopeId,
                                                                  Long createdBy,
                                                                  boolean viewAll,
                                                                  List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = "SELECT * FROM dev." + type.headerTable()
                + " WHERE scope_type = :scopeType AND scope_id = :scopeId AND id IN (:ids)"
                + (viewAll ? "" : " AND created_by = :createdBy")
                + " ORDER BY id DESC";
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("scopeType", scopeType)
                .addValue("scopeId", scopeId)
                .addValue("ids", ids);
        if (!viewAll) {
            source.addValue("createdBy", createdBy);
        }
        return namedParameterJdbcTemplate.query(sql, source, (rs, rowNum) -> mapHeader(rs));
    }

    @Override
    public Optional<InventoryDocumentHeader> findHeaderByScopeAndId(InventoryDocumentType type,
                                                                    String scopeType,
                                                                    Long scopeId,
                                                                    Long createdBy,
                                                                    boolean viewAll,
                                                                    Long id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM dev." + type.headerTable()
                + " WHERE scope_type = :scopeType AND scope_id = :scopeId AND id = :id"
                + (viewAll ? "" : " AND created_by = :createdBy")
                + " ORDER BY id DESC";
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("scopeType", scopeType)
                .addValue("scopeId", scopeId)
                .addValue("id", id);
        if (!viewAll) {
            source.addValue("createdBy", createdBy);
        }
        List<InventoryDocumentHeader> rows = namedParameterJdbcTemplate.query(sql, source, (rs, rowNum) -> mapHeader(rs));
        return rows.stream().findFirst();
    }

    @Override
    public Long countByScopeAndDocumentCode(InventoryDocumentType type, String scopeType, Long scopeId, String documentCode) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM dev." + type.headerTable()
                        + " WHERE scope_type = :scopeType AND scope_id = :scopeId AND document_code = :documentCode",
                new MapSqlParameterSource()
                        .addValue("scopeType", scopeType)
                        .addValue("scopeId", scopeId)
                        .addValue("documentCode", documentCode),
                Long.class
        );
        return count == null ? 0L : count;
    }

    @Override
    public void saveHeader(InventoryDocumentType type, InventoryDocumentHeader header) {
        String sql = "INSERT INTO dev." + type.headerTable()
                + " (scope_type, scope_id, document_code, document_date, primary_name, secondary_name, counterparty_name,"
                + " counterparty_name2, reason, upstream_code, salesman_user_id, salesman_name, total_amount, status,"
                + " workflow_process_code, workflow_definition_key, workflow_definition_id, workflow_instance_id,"
                + " workflow_task_id, workflow_task_name, workflow_status, pending_operation, remark, rejection_reason,"
                + " extra_json, created_by, approved_by, approved_at, created_at, updated_at)"
                + " VALUES (:scopeType, :scopeId, :documentCode, :documentDate, :primaryName, :secondaryName, :counterpartyName,"
                + " :counterpartyName2, :reason, :upstreamCode, :salesmanUserId, :salesmanName, :totalAmount, :status,"
                + " :workflowProcessCode, :workflowDefinitionKey, :workflowDefinitionId, :workflowInstanceId,"
                + " :workflowTaskId, :workflowTaskName, :workflowStatus, :pendingOperation, :remark, :rejectionReason,"
                + " :extraJson, :createdBy, :approvedBy, :approvedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, headerParams(header), keyHolder, new String[]{"id"});
        if (keyHolder.getKey() != null) {
            header.setId(keyHolder.getKey().longValue());
        }
    }

    @Override
    public void updateHeader(InventoryDocumentType type, InventoryDocumentHeader header) {
        String sql = "UPDATE dev." + type.headerTable()
                + " SET document_date = :documentDate, primary_name = :primaryName, secondary_name = :secondaryName,"
                + " counterparty_name = :counterpartyName, counterparty_name2 = :counterpartyName2, reason = :reason,"
                + " upstream_code = :upstreamCode, salesman_user_id = :salesmanUserId, salesman_name = :salesmanName,"
                + " total_amount = :totalAmount, status = :status, workflow_process_code = :workflowProcessCode,"
                + " workflow_definition_key = :workflowDefinitionKey, workflow_definition_id = :workflowDefinitionId,"
                + " workflow_instance_id = :workflowInstanceId, workflow_task_id = :workflowTaskId,"
                + " workflow_task_name = :workflowTaskName, workflow_status = :workflowStatus,"
                + " pending_operation = :pendingOperation, remark = :remark, rejection_reason = :rejectionReason,"
                + " extra_json = :extraJson, approved_by = :approvedBy, approved_at = :approvedAt,"
                + " updated_at = CURRENT_TIMESTAMP WHERE id = :id";
        namedParameterJdbcTemplate.update(sql, headerParams(header));
    }

    @Override
    public void deleteHeaderById(InventoryDocumentType type, Long id) {
        namedParameterJdbcTemplate.update(
                "DELETE FROM dev." + type.headerTable() + " WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id)
        );
    }

    @Override
    public List<InventoryDocumentLine> findLinesByHeaderIds(InventoryDocumentType type, List<Long> headerIds) {
        if (headerIds == null || headerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return namedParameterJdbcTemplate.query(
                "SELECT * FROM dev." + type.lineTable() + " WHERE header_id IN (:headerIds) ORDER BY id ASC",
                new MapSqlParameterSource().addValue("headerIds", headerIds),
                (rs, rowNum) -> mapLine(rs)
        );
    }

    @Override
    public List<InventoryDocumentLine> findLinesByHeaderId(InventoryDocumentType type, Long headerId) {
        if (headerId == null) {
            return Collections.emptyList();
        }
        return namedParameterJdbcTemplate.query(
                "SELECT * FROM dev." + type.lineTable() + " WHERE header_id = :headerId ORDER BY id ASC",
                new MapSqlParameterSource().addValue("headerId", headerId),
                (rs, rowNum) -> mapLine(rs)
        );
    }

    @Override
    public void deleteLinesByHeaderId(InventoryDocumentType type, Long headerId) {
        namedParameterJdbcTemplate.update(
                "DELETE FROM dev." + type.lineTable() + " WHERE header_id = :headerId",
                new MapSqlParameterSource().addValue("headerId", headerId)
        );
    }

    @Override
    public void saveLine(InventoryDocumentType type, InventoryDocumentLine line) {
        String sql = "INSERT INTO dev." + type.lineTable()
                + " (header_id, item_code, item_name, spec, category, unit_name, available_qty, quantity, unit_price,"
                + " amount, line_reason, remark, extra_json, created_at)"
                + " VALUES (:headerId, :itemCode, :itemName, :spec, :category, :unitName, :availableQty, :quantity,"
                + " :unitPrice, :amount, :lineReason, :remark, :extraJson, CURRENT_TIMESTAMP)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, lineParams(line), keyHolder, new String[]{"id"});
        if (keyHolder.getKey() != null) {
            line.setId(keyHolder.getKey().longValue());
        }
    }

    private MapSqlParameterSource headerParams(InventoryDocumentHeader header) {
        return new MapSqlParameterSource()
                .addValue("id", header.getId())
                .addValue("scopeType", header.getScopeType())
                .addValue("scopeId", header.getScopeId())
                .addValue("documentCode", header.getDocumentCode())
                .addValue("documentDate", header.getDocumentDate())
                .addValue("primaryName", header.getPrimaryName(), Types.VARCHAR)
                .addValue("secondaryName", header.getSecondaryName(), Types.VARCHAR)
                .addValue("counterpartyName", header.getCounterpartyName(), Types.VARCHAR)
                .addValue("counterpartyName2", header.getCounterpartyName2(), Types.VARCHAR)
                .addValue("reason", header.getReason(), Types.VARCHAR)
                .addValue("upstreamCode", header.getUpstreamCode(), Types.VARCHAR)
                .addValue("salesmanUserId", header.getSalesmanUserId(), Types.BIGINT)
                .addValue("salesmanName", header.getSalesmanName(), Types.VARCHAR)
                .addValue("totalAmount", header.getTotalAmount())
                .addValue("status", header.getStatus())
                .addValue("workflowProcessCode", header.getWorkflowProcessCode(), Types.VARCHAR)
                .addValue("workflowDefinitionKey", header.getWorkflowDefinitionKey(), Types.VARCHAR)
                .addValue("workflowDefinitionId", header.getWorkflowDefinitionId(), Types.VARCHAR)
                .addValue("workflowInstanceId", header.getWorkflowInstanceId(), Types.VARCHAR)
                .addValue("workflowTaskId", header.getWorkflowTaskId(), Types.VARCHAR)
                .addValue("workflowTaskName", header.getWorkflowTaskName(), Types.VARCHAR)
                .addValue("workflowStatus", header.getWorkflowStatus(), Types.VARCHAR)
                .addValue("pendingOperation", header.getPendingOperation(), Types.VARCHAR)
                .addValue("remark", header.getRemark(), Types.VARCHAR)
                .addValue("rejectionReason", header.getRejectionReason(), Types.VARCHAR)
                .addValue("extraJson", header.getExtraJson(), Types.VARCHAR)
                .addValue("createdBy", header.getCreatedBy(), Types.BIGINT)
                .addValue("approvedBy", header.getApprovedBy(), Types.BIGINT)
                .addValue("approvedAt", header.getApprovedAt(), Types.TIMESTAMP);
    }

    private MapSqlParameterSource lineParams(InventoryDocumentLine line) {
        return new MapSqlParameterSource()
                .addValue("headerId", line.getHeaderId())
                .addValue("itemCode", line.getItemCode(), Types.VARCHAR)
                .addValue("itemName", line.getItemName(), Types.VARCHAR)
                .addValue("spec", line.getSpec(), Types.VARCHAR)
                .addValue("category", line.getCategory(), Types.VARCHAR)
                .addValue("unitName", line.getUnitName(), Types.VARCHAR)
                .addValue("availableQty", line.getAvailableQty())
                .addValue("quantity", line.getQuantity())
                .addValue("unitPrice", line.getUnitPrice())
                .addValue("amount", line.getAmount())
                .addValue("lineReason", line.getLineReason(), Types.VARCHAR)
                .addValue("remark", line.getRemark(), Types.VARCHAR)
                .addValue("extraJson", line.getExtraJson(), Types.VARCHAR);
    }

    private InventoryDocumentHeader mapHeader(ResultSet rs) throws SQLException {
        InventoryDocumentHeader header = new InventoryDocumentHeader();
        header.setId(rs.getLong("id"));
        header.setScopeType(rs.getString("scope_type"));
        header.setScopeId(rs.getLong("scope_id"));
        header.setDocumentCode(rs.getString("document_code"));
        header.setDocumentDate(rs.getObject("document_date", java.time.LocalDate.class));
        header.setPrimaryName(rs.getString("primary_name"));
        header.setSecondaryName(rs.getString("secondary_name"));
        header.setCounterpartyName(rs.getString("counterparty_name"));
        header.setCounterpartyName2(rs.getString("counterparty_name2"));
        header.setReason(rs.getString("reason"));
        header.setUpstreamCode(rs.getString("upstream_code"));
        Long salesmanUserId = rs.getObject("salesman_user_id", Long.class);
        header.setSalesmanUserId(salesmanUserId);
        header.setSalesmanName(rs.getString("salesman_name"));
        header.setTotalAmount(rs.getBigDecimal("total_amount"));
        header.setStatus(rs.getString("status"));
        header.setWorkflowProcessCode(rs.getString("workflow_process_code"));
        header.setWorkflowDefinitionKey(rs.getString("workflow_definition_key"));
        header.setWorkflowDefinitionId(rs.getString("workflow_definition_id"));
        header.setWorkflowInstanceId(rs.getString("workflow_instance_id"));
        header.setWorkflowTaskId(rs.getString("workflow_task_id"));
        header.setWorkflowTaskName(rs.getString("workflow_task_name"));
        header.setWorkflowStatus(rs.getString("workflow_status"));
        header.setPendingOperation(rs.getString("pending_operation"));
        header.setRemark(rs.getString("remark"));
        header.setRejectionReason(rs.getString("rejection_reason"));
        header.setExtraJson(rs.getString("extra_json"));
        header.setCreatedBy(rs.getObject("created_by", Long.class));
        header.setApprovedBy(rs.getObject("approved_by", Long.class));
        header.setApprovedAt(rs.getObject("approved_at", java.time.LocalDateTime.class));
        header.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
        header.setUpdatedAt(rs.getObject("updated_at", java.time.LocalDateTime.class));
        return header;
    }

    private InventoryDocumentLine mapLine(ResultSet rs) throws SQLException {
        InventoryDocumentLine line = new InventoryDocumentLine();
        line.setId(rs.getLong("id"));
        line.setHeaderId(rs.getLong("header_id"));
        line.setItemCode(rs.getString("item_code"));
        line.setItemName(rs.getString("item_name"));
        line.setSpec(rs.getString("spec"));
        line.setCategory(rs.getString("category"));
        line.setUnitName(rs.getString("unit_name"));
        line.setAvailableQty(rs.getBigDecimal("available_qty"));
        line.setQuantity(rs.getBigDecimal("quantity"));
        line.setUnitPrice(rs.getBigDecimal("unit_price"));
        line.setAmount(rs.getBigDecimal("amount"));
        line.setLineReason(rs.getString("line_reason"));
        line.setRemark(rs.getString("remark"));
        line.setExtraJson(rs.getString("extra_json"));
        line.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
        return line;
    }
}
