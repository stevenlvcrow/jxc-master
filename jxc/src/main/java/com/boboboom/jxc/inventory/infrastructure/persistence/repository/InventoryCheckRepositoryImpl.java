package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import com.boboboom.jxc.inventory.application.service.InventoryCheckHeader;
import com.boboboom.jxc.inventory.application.service.InventoryCheckKind;
import com.boboboom.jxc.inventory.application.service.InventoryCheckLine;
import com.boboboom.jxc.inventory.domain.repository.InventoryCheckRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 盘点单 JDBC 仓储实现。
 */
@Repository
public class InventoryCheckRepositoryImpl implements InventoryCheckRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public InventoryCheckRepositoryImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public List<InventoryCheckHeader> findHeadersByScopeAndKindOrdered(InventoryCheckKind kind, String scopeType, Long scopeId) {
        return namedParameterJdbcTemplate.query(
                "SELECT * FROM dev." + kind.getHeaderTable()
                        + " WHERE scope_type = :scopeType AND scope_id = :scopeId"
                        + " ORDER BY created_at DESC, id DESC",
                new MapSqlParameterSource()
                        .addValue("scopeType", scopeType)
                        .addValue("scopeId", scopeId),
                (rs, rowNum) -> mapHeader(rs)
        );
    }

    @Override
    public List<InventoryCheckHeader> findHeadersByScopeAndKindAndIds(InventoryCheckKind kind,
                                                                      String scopeType,
                                                                      Long scopeId,
                                                                      Long createdBy,
                                                                      boolean viewAll,
                                                                      List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = "SELECT * FROM dev." + kind.getHeaderTable()
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
    public Optional<InventoryCheckHeader> findHeaderByScopeAndKindAndId(InventoryCheckKind kind,
                                                                        String scopeType,
                                                                        Long scopeId,
                                                                        Long createdBy,
                                                                        boolean viewAll,
                                                                        Long id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM dev." + kind.getHeaderTable()
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
        List<InventoryCheckHeader> rows = namedParameterJdbcTemplate.query(sql, source, (rs, rowNum) -> mapHeader(rs));
        return rows.stream().findFirst();
    }

    @Override
    public Long countByScopeAndKindAndDocumentCode(InventoryCheckKind kind, String scopeType, Long scopeId, String documentCode) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM dev." + kind.getHeaderTable()
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
    public void saveHeader(InventoryCheckKind kind, InventoryCheckHeader header) {
        String sql = "INSERT INTO dev." + kind.getHeaderTable()
                + " (scope_type, scope_id, document_code, check_date, warehouse_name, check_range_type, freeze_stock,"
                + " collaborative_flag, plan_name, third_party_document, salesman_user_id, salesman_name, item_count,"
                + " total_book_amount, total_actual_amount, total_diff_amount, diff_status, generated_status, print_status,"
                + " status, remark, rejection_reason, extra_json, created_by, approved_by, approved_at, created_at, updated_at)"
                + " VALUES (:scopeType, :scopeId, :documentCode, :checkDate, :warehouseName, :checkRangeType, :freezeStock,"
                + " :collaborativeFlag, :planName, :thirdPartyDocument, :salesmanUserId, :salesmanName, :itemCount,"
                + " :totalBookAmount, :totalActualAmount, :totalDiffAmount, :diffStatus, :generatedStatus, :printStatus,"
                + " :status, :remark, :rejectionReason, :extraJson, :createdBy, :approvedBy, :approvedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, headerParams(header), keyHolder, new String[]{"id"});
        if (keyHolder.getKey() != null) {
            header.setId(keyHolder.getKey().longValue());
        }
    }

    @Override
    public void updateHeader(InventoryCheckKind kind, InventoryCheckHeader header) {
        String sql = "UPDATE dev." + kind.getHeaderTable()
                + " SET check_date = :checkDate, warehouse_name = :warehouseName, check_range_type = :checkRangeType,"
                + " freeze_stock = :freezeStock, collaborative_flag = :collaborativeFlag, plan_name = :planName,"
                + " third_party_document = :thirdPartyDocument, salesman_user_id = :salesmanUserId, salesman_name = :salesmanName,"
                + " item_count = :itemCount, total_book_amount = :totalBookAmount, total_actual_amount = :totalActualAmount,"
                + " total_diff_amount = :totalDiffAmount, diff_status = :diffStatus, generated_status = :generatedStatus,"
                + " print_status = :printStatus, status = :status, remark = :remark, rejection_reason = :rejectionReason,"
                + " extra_json = :extraJson, approved_by = :approvedBy, approved_at = :approvedAt, updated_at = CURRENT_TIMESTAMP"
                + " WHERE id = :id";
        namedParameterJdbcTemplate.update(sql, headerParams(header));
    }

    @Override
    public void deleteHeaderById(InventoryCheckKind kind, Long id) {
        namedParameterJdbcTemplate.update(
                "DELETE FROM dev." + kind.getHeaderTable() + " WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id)
        );
    }

    @Override
    public List<InventoryCheckLine> findLinesByHeaderIds(InventoryCheckKind kind, List<Long> headerIds) {
        if (headerIds == null || headerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return namedParameterJdbcTemplate.query(
                "SELECT * FROM dev." + kind.getLineTable() + " WHERE header_id IN (:headerIds) ORDER BY id ASC",
                new MapSqlParameterSource().addValue("headerIds", headerIds),
                (rs, rowNum) -> mapLine(rs)
        );
    }

    @Override
    public List<InventoryCheckLine> findLinesByHeaderId(InventoryCheckKind kind, Long headerId) {
        if (headerId == null) {
            return Collections.emptyList();
        }
        return namedParameterJdbcTemplate.query(
                "SELECT * FROM dev." + kind.getLineTable() + " WHERE header_id = :headerId ORDER BY id ASC",
                new MapSqlParameterSource().addValue("headerId", headerId),
                (rs, rowNum) -> mapLine(rs)
        );
    }

    @Override
    public void deleteLinesByHeaderId(InventoryCheckKind kind, Long headerId) {
        namedParameterJdbcTemplate.update(
                "DELETE FROM dev." + kind.getLineTable() + " WHERE header_id = :headerId",
                new MapSqlParameterSource().addValue("headerId", headerId)
        );
    }

    @Override
    public void saveLine(InventoryCheckKind kind, InventoryCheckLine line) {
        String sql = "INSERT INTO dev." + kind.getLineTable()
                + " (header_id, item_code, item_name, spec, category, unit_name, available_qty, book_qty, actual_qty,"
                + " book_price, book_amount, actual_amount, diff_qty, diff_amount, profit_qty, loss_qty, profit_loss_reason,"
                + " profit_inbound_price, profit_amount, loss_outbound_price, loss_amount, abnormal_flag, remark, extra_json, created_at)"
                + " VALUES (:headerId, :itemCode, :itemName, :spec, :category, :unitName, :availableQty, :bookQty, :actualQty,"
                + " :bookPrice, :bookAmount, :actualAmount, :diffQty, :diffAmount, :profitQty, :lossQty, :profitLossReason,"
                + " :profitInboundPrice, :profitAmount, :lossOutboundPrice, :lossAmount, :abnormalFlag, :remark, :extraJson, CURRENT_TIMESTAMP)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, lineParams(line), keyHolder, new String[]{"id"});
        if (keyHolder.getKey() != null) {
            line.setId(keyHolder.getKey().longValue());
        }
    }

    private MapSqlParameterSource headerParams(InventoryCheckHeader header) {
        return new MapSqlParameterSource()
                .addValue("id", header.getId())
                .addValue("scopeType", header.getScopeType())
                .addValue("scopeId", header.getScopeId())
                .addValue("documentCode", header.getDocumentCode())
                .addValue("checkDate", header.getCheckDate())
                .addValue("warehouseName", header.getWarehouseName(), Types.VARCHAR)
                .addValue("checkRangeType", header.getCheckRangeType(), Types.VARCHAR)
                .addValue("freezeStock", header.getFreezeStock())
                .addValue("collaborativeFlag", header.getCollaborativeFlag())
                .addValue("planName", header.getPlanName(), Types.VARCHAR)
                .addValue("thirdPartyDocument", header.getThirdPartyDocument(), Types.VARCHAR)
                .addValue("salesmanUserId", header.getSalesmanUserId(), Types.BIGINT)
                .addValue("salesmanName", header.getSalesmanName(), Types.VARCHAR)
                .addValue("itemCount", header.getItemCount())
                .addValue("totalBookAmount", header.getTotalBookAmount())
                .addValue("totalActualAmount", header.getTotalActualAmount())
                .addValue("totalDiffAmount", header.getTotalDiffAmount())
                .addValue("diffStatus", header.getDiffStatus())
                .addValue("generatedStatus", header.getGeneratedStatus(), Types.VARCHAR)
                .addValue("printStatus", header.getPrintStatus())
                .addValue("status", header.getStatus())
                .addValue("remark", header.getRemark(), Types.VARCHAR)
                .addValue("rejectionReason", header.getRejectionReason(), Types.VARCHAR)
                .addValue("extraJson", header.getExtraJson(), Types.VARCHAR)
                .addValue("createdBy", header.getCreatedBy(), Types.BIGINT)
                .addValue("approvedBy", header.getApprovedBy(), Types.BIGINT)
                .addValue("approvedAt", header.getApprovedAt(), Types.TIMESTAMP);
    }

    private MapSqlParameterSource lineParams(InventoryCheckLine line) {
        return new MapSqlParameterSource()
                .addValue("headerId", line.getHeaderId())
                .addValue("itemCode", line.getItemCode(), Types.VARCHAR)
                .addValue("itemName", line.getItemName(), Types.VARCHAR)
                .addValue("spec", line.getSpec(), Types.VARCHAR)
                .addValue("category", line.getCategory(), Types.VARCHAR)
                .addValue("unitName", line.getUnitName(), Types.VARCHAR)
                .addValue("availableQty", line.getAvailableQty())
                .addValue("bookQty", line.getBookQty())
                .addValue("actualQty", line.getActualQty())
                .addValue("bookPrice", line.getBookPrice())
                .addValue("bookAmount", line.getBookAmount())
                .addValue("actualAmount", line.getActualAmount())
                .addValue("diffQty", line.getDiffQty())
                .addValue("diffAmount", line.getDiffAmount())
                .addValue("profitQty", line.getProfitQty())
                .addValue("lossQty", line.getLossQty())
                .addValue("profitLossReason", line.getProfitLossReason(), Types.VARCHAR)
                .addValue("profitInboundPrice", line.getProfitInboundPrice())
                .addValue("profitAmount", line.getProfitAmount())
                .addValue("lossOutboundPrice", line.getLossOutboundPrice())
                .addValue("lossAmount", line.getLossAmount())
                .addValue("abnormalFlag", line.getAbnormalFlag())
                .addValue("remark", line.getRemark(), Types.VARCHAR)
                .addValue("extraJson", line.getExtraJson(), Types.VARCHAR);
    }

    private InventoryCheckHeader mapHeader(ResultSet rs) throws SQLException {
        InventoryCheckHeader header = new InventoryCheckHeader();
        header.setId(rs.getLong("id"));
        header.setScopeType(rs.getString("scope_type"));
        header.setScopeId(rs.getLong("scope_id"));
        header.setDocumentCode(rs.getString("document_code"));
        header.setCheckDate(rs.getObject("check_date", java.time.LocalDate.class));
        header.setWarehouseName(rs.getString("warehouse_name"));
        header.setCheckRangeType(rs.getString("check_range_type"));
        header.setFreezeStock(rs.getObject("freeze_stock", Boolean.class));
        header.setCollaborativeFlag(rs.getObject("collaborative_flag", Boolean.class));
        header.setPlanName(rs.getString("plan_name"));
        header.setThirdPartyDocument(rs.getString("third_party_document"));
        header.setSalesmanUserId(rs.getObject("salesman_user_id", Long.class));
        header.setSalesmanName(rs.getString("salesman_name"));
        header.setItemCount(rs.getObject("item_count", Integer.class));
        header.setTotalBookAmount(rs.getBigDecimal("total_book_amount"));
        header.setTotalActualAmount(rs.getBigDecimal("total_actual_amount"));
        header.setTotalDiffAmount(rs.getBigDecimal("total_diff_amount"));
        header.setDiffStatus(rs.getString("diff_status"));
        header.setGeneratedStatus(rs.getString("generated_status"));
        header.setPrintStatus(rs.getString("print_status"));
        header.setStatus(rs.getString("status"));
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

    private InventoryCheckLine mapLine(ResultSet rs) throws SQLException {
        InventoryCheckLine line = new InventoryCheckLine();
        line.setId(rs.getLong("id"));
        line.setHeaderId(rs.getLong("header_id"));
        line.setItemCode(rs.getString("item_code"));
        line.setItemName(rs.getString("item_name"));
        line.setSpec(rs.getString("spec"));
        line.setCategory(rs.getString("category"));
        line.setUnitName(rs.getString("unit_name"));
        line.setAvailableQty(rs.getBigDecimal("available_qty"));
        line.setBookQty(rs.getBigDecimal("book_qty"));
        line.setActualQty(rs.getBigDecimal("actual_qty"));
        line.setBookPrice(rs.getBigDecimal("book_price"));
        line.setBookAmount(rs.getBigDecimal("book_amount"));
        line.setActualAmount(rs.getBigDecimal("actual_amount"));
        line.setDiffQty(rs.getBigDecimal("diff_qty"));
        line.setDiffAmount(rs.getBigDecimal("diff_amount"));
        line.setProfitQty(rs.getBigDecimal("profit_qty"));
        line.setLossQty(rs.getBigDecimal("loss_qty"));
        line.setProfitLossReason(rs.getString("profit_loss_reason"));
        line.setProfitInboundPrice(rs.getBigDecimal("profit_inbound_price"));
        line.setProfitAmount(rs.getBigDecimal("profit_amount"));
        line.setLossOutboundPrice(rs.getBigDecimal("loss_outbound_price"));
        line.setLossAmount(rs.getBigDecimal("loss_amount"));
        line.setAbnormalFlag(rs.getString("abnormal_flag"));
        line.setRemark(rs.getString("remark"));
        line.setExtraJson(rs.getString("extra_json"));
        line.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
        return line;
    }
}
