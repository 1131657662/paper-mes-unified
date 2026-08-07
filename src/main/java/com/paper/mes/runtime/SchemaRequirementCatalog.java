package com.paper.mes.runtime;

import java.util.List;

final class SchemaRequirementCatalog {

    private SchemaRequirementCatalog() {
    }

    static List<SchemaRequirement> criticalStructures() {
        return List.of(
                table("biz_process_order_issue_version"),
                column("biz_process_order_issue_version", "request_id"),
                column("biz_process_order_issue_version", "payload_hash"),
                index("biz_process_order_issue_version", "uk_process_order_issue_version"),
                index("biz_process_order_issue_version", "uk_process_order_issue_request"),
                trigger("trg_process_order_issue_version_no_terminal_update"),
                trigger("trg_process_order_issue_version_no_terminal_delete"),
                table("biz_process_order_append_session"),
                table("biz_process_order_append_roll"),
                column("biz_process_order_append_session", "commit_request_id"),
                column("biz_process_order_append_session", "active_order_uuid"),
                index("biz_process_order_append_session", "uk_process_append_active_order"),
                table("biz_receive_record"),
                column("biz_receive_record", "request_id"),
                column("biz_receive_record", "request_hash"),
                index("biz_receive_record", "uk_receive_settle_request"),
                table("biz_settle_discount_approval"),
                column("biz_settle_discount_approval", "cash_amount"),
                column("biz_settle_discount_approval", "scrap_offset_amount"),
                column("biz_settle_discount_approval", "request_hash"),
                column("biz_settle_discount_approval", "required_level"),
                column("biz_settle_discount_approval", "unreceived_snapshot"),
                column("biz_settle_discount_approval", "discount_percent"),
                column("biz_settle_discount_approval", "decision_reason"),
                column("biz_settle_discount_approval", "cancel_by"),
                column("biz_settle_discount_approval", "cancel_by_name"),
                column("biz_settle_discount_approval", "cancel_time"),
                column("biz_settle_discount_approval", "policy_version"),
                column("biz_settle_discount_approval", "active_settle_uuid"),
                index("biz_settle_discount_approval", "uk_discount_approval_active_settle"),
                index("biz_settle_discount_approval", "idx_discount_approval_inbox"),
                index("biz_settle_discount_approval", "idx_discount_approval_requester"),
                constraint("biz_settle_discount_approval", "chk_discount_approval_amount_positive"),
                constraint("biz_settle_discount_approval", "chk_discount_approval_status"),
                constraint("biz_settle_discount_approval", "chk_discount_approval_level"),
                constraint("biz_settle_discount_approval", "chk_discount_approval_components"),
                table("biz_inventory_transaction"),
                column("biz_inventory_transaction", "idempotency_key"),
                column("biz_inventory_transaction", "payload_hash"),
                index("biz_inventory_transaction", "uk_inventory_transaction_idempotency"),
                trigger("trg_inventory_transaction_no_update"),
                trigger("trg_inventory_transaction_no_delete")
        );
    }

    private static SchemaRequirement table(String table) {
        return new SchemaRequirement(SchemaRequirement.Kind.TABLE, table, table);
    }

    private static SchemaRequirement column(String table, String column) {
        return new SchemaRequirement(SchemaRequirement.Kind.COLUMN, table, column);
    }

    private static SchemaRequirement index(String table, String index) {
        return new SchemaRequirement(SchemaRequirement.Kind.INDEX, table, index);
    }

    private static SchemaRequirement constraint(String table, String constraint) {
        return new SchemaRequirement(SchemaRequirement.Kind.CONSTRAINT, table, constraint);
    }

    private static SchemaRequirement trigger(String trigger) {
        return new SchemaRequirement(SchemaRequirement.Kind.TRIGGER, "", trigger);
    }
}
