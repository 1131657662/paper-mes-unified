package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessCatalogUnitVO;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ProcessCatalogRepository {

    private static final String ACTIVE_CATALOG_SQL = """
            SELECT c.uuid, c.step_type, c.process_code, c.process_name, c.process_category,
                   c.pricing_strategy, c.produces_inventory_output, c.allows_loss_recording,
                   c.allows_main_process, u.unit_code, u.unit_name, u.is_default,
                   b.billing_mode
            FROM sys_process_catalog c
            LEFT JOIN sys_process_catalog_unit u ON u.catalog_uuid = c.uuid
            LEFT JOIN sys_process_catalog_billing_mode b ON b.catalog_uuid = c.uuid
            WHERE c.status = 1 AND c.is_deleted = 0
            ORDER BY c.sort_no, c.step_type, u.sort_no, b.sort_no
            """;

    private final JdbcTemplate jdbcTemplate;

    public List<ProcessCatalogVO> findActive() {
        Map<Integer, CatalogAccumulator> entries = new LinkedHashMap<>();
        RowCallbackHandler handler = resultSet -> accumulate(entries, resultSet);
        jdbcTemplate.query(ACTIVE_CATALOG_SQL, handler);
        return entries.values().stream().map(CatalogAccumulator::toView).toList();
    }

    private void accumulate(Map<Integer, CatalogAccumulator> entries, ResultSet resultSet)
            throws SQLException {
        int stepType = resultSet.getInt("step_type");
        CatalogAccumulator entry = entries.computeIfAbsent(stepType,
                ignored -> catalog(resultSet, stepType));
        entry.addUnit(resultSet);
        entry.addBillingMode(resultSet);
    }

    private CatalogAccumulator catalog(ResultSet resultSet, int stepType) {
        try {
            return new CatalogAccumulator(resultSet, stepType);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read process catalog", exception);
        }
    }

    private static final class CatalogAccumulator {
        private final String uuid;
        private final int stepType;
        private final String code;
        private final String name;
        private final String category;
        private final String strategy;
        private final boolean producesOutput;
        private final boolean allowsLoss;
        private final boolean allowsMain;
        private final Map<String, ProcessCatalogUnitVO> units = new LinkedHashMap<>();
        private final List<Integer> billingModes = new ArrayList<>();

        private CatalogAccumulator(ResultSet row, int stepType) throws SQLException {
            this.uuid = row.getString("uuid");
            this.stepType = stepType;
            this.code = row.getString("process_code");
            this.name = row.getString("process_name");
            this.category = row.getString("process_category");
            this.strategy = row.getString("pricing_strategy");
            this.producesOutput = row.getBoolean("produces_inventory_output");
            this.allowsLoss = row.getBoolean("allows_loss_recording");
            this.allowsMain = row.getBoolean("allows_main_process");
        }

        private void addUnit(ResultSet row) throws SQLException {
            String unitCode = row.getString("unit_code");
            if (unitCode != null) {
                units.putIfAbsent(unitCode, new ProcessCatalogUnitVO(
                        unitCode, row.getString("unit_name"), row.getBoolean("is_default")));
            }
        }

        private void addBillingMode(ResultSet row) throws SQLException {
            int mode = row.getInt("billing_mode");
            if (!row.wasNull() && !billingModes.contains(mode)) {
                billingModes.add(mode);
            }
        }

        private ProcessCatalogVO toView() {
            return new ProcessCatalogVO(uuid, stepType, code, name, category, strategy,
                    producesOutput, allowsLoss, allowsMain,
                    List.copyOf(units.values()), List.copyOf(billingModes));
        }
    }
}
