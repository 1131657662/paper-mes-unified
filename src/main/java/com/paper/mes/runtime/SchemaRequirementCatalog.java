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
                column("biz_process_order", "post_production_note"),
                table("biz_process_order_append_session"),
                table("biz_process_order_append_roll"),
                table("biz_process_roll_disposition"),
                index("biz_process_roll_disposition", "uk_process_roll_disposition_source"),
                index("biz_process_roll_disposition", "uk_process_roll_disposition_request"),
                column("biz_original_roll", "disposition_action"),
                index("biz_original_roll", "idx_original_roll_disposition"),
                column("biz_process_order_append_session", "commit_request_id"),
                column("biz_process_order_append_session", "active_order_uuid"),
                index("biz_process_order_append_session", "uk_process_append_active_order"),
                column("biz_original_roll", "roll_weight"),
                column("biz_original_roll", "weight_status"),
                column("biz_original_roll", "weight_source"),
                column("biz_original_roll", "weight_recorded_at"),
                column("biz_original_roll", "weight_recorded_by"),
                column("biz_finish_original_rel", "consume_ratio"),
                column("biz_process_roll_disposition", "target_finish_uuids"),
                column("biz_process_step", "billing_weight_status"),
                column("biz_process_step", "billing_weight_basis"),
                column("biz_process_step", "pricing_dirty"),
                table("biz_project_memory_doc"),
                table("biz_project_memory_patch_audit"),
                index("biz_project_memory_patch_audit", "uk_project_memory_patch_idempotency"),
                column("biz_process_order", "ai_requirement_json"),
                column("biz_process_config_draft", "ai_intent_json"),
                table("biz_process_ai_conversation"),
                column("biz_process_ai_conversation", "memory_generation"),
                column("biz_process_ai_conversation", "clarification_round"),
                index("biz_process_ai_conversation", "uk_ai_conversation_id"),
                index("biz_process_ai_conversation", "uk_ai_conversation_order"),
                constraint("biz_process_ai_conversation", "chk_ai_conversation_clarification_round"),
                table("biz_process_ai_message"),
                column("biz_process_ai_message", "memory_generation"),
                index("biz_process_ai_message", "uk_ai_message_sequence"),
                index("biz_process_ai_message", "uk_ai_message_idempotency_generation"),
                indexColumns("biz_process_ai_message", "uk_ai_message_idempotency_generation",
                        "conversation_id,memory_generation,idempotency_key"),
                table("biz_process_ai_parse"),
                column("biz_process_ai_parse", "memory_generation"),
                column("biz_process_ai_parse", "dialogue_state"),
                column("biz_process_ai_parse", "result_kind"),
                column("biz_process_ai_parse", "workflow_version"),
                column("biz_process_ai_parse", "understanding_json"),
                column("biz_process_ai_parse", "question_json"),
                column("biz_process_ai_parse", "corrections_json"),
                column("biz_process_ai_parse", "input_hash"),
                column("biz_process_ai_parse", "context_hash"),
                column("biz_process_ai_parse", "preview_hash"),
                column("biz_process_ai_parse", "failure_code"),
                column("biz_process_ai_parse", "failure_trace_id"),
                column("biz_process_ai_parse", "required_default_ids"),
                column("biz_process_ai_parse", "acknowledged_default_ids"),
                index("biz_process_ai_parse", "uk_ai_parse_id"),
                index("biz_process_ai_parse", "uk_ai_parse_conversation_revision"),
                index("biz_process_ai_parse", "uk_ai_parse_request_idempotency"),
                index("biz_process_ai_parse", "uk_ai_parse_apply_idempotency"),
                index("biz_process_ai_parse", "idx_ai_parse_order_version"),
                index("biz_process_ai_parse", "idx_ai_parse_conversation_dialogue"),
                constraint("biz_process_ai_parse", "chk_ai_parse_dialogue_state"),
                constraint("biz_process_ai_parse", "chk_ai_parse_result_kind"),
                constraint("biz_process_ai_parse", "chk_ai_parse_workflow_version"),
                constraint("biz_process_ai_parse", "chk_ai_parse_result_consistency"),
                table("biz_process_ai_packaging_candidate"),
                index("biz_process_ai_packaging_candidate", "uk_ai_packaging_candidate_parse_owner"),
                index("biz_process_ai_packaging_candidate", "idx_ai_packaging_candidate_pending"),
                index("biz_process_ai_packaging_candidate", "idx_ai_packaging_candidate_conversation"),
                table("sys_ai_call_audit"),
                index("sys_ai_call_audit", "idx_ai_audit_order_time"),
                index("sys_ai_call_audit", "idx_ai_audit_attempt"),
                table("sys_ai_provider_secret"),
                table("biz_project_memory_candidate"),
                column("biz_project_memory_candidate", "review_notes"),
                index("biz_project_memory_candidate", "uk_memory_candidate_id"),
                table("biz_project_memory_candidate_evidence"),
                column("biz_project_memory_candidate_evidence", "order_ref_hash"),
                column("biz_project_memory_candidate_evidence", "parse_ref_hash"),
                nullableColumn("biz_project_memory_candidate_evidence", "order_uuid"),
                nullableColumn("biz_project_memory_candidate_evidence", "parse_id"),
                column("biz_project_memory_candidate_evidence", "audit_context_ciphertext"),
                column("biz_project_memory_candidate_evidence", "audit_context_hash"),
                column("biz_project_memory_candidate_evidence", "source_type"),
                column("biz_project_memory_candidate_evidence", "final_value_json"),
                column("biz_project_memory_candidate_evidence", "preview_ready"),
                index("biz_project_memory_candidate_evidence", "uk_memory_candidate_order_ref"),
                indexColumns("biz_project_memory_candidate_evidence", "uk_memory_candidate_order_ref",
                        "candidate_uuid,order_ref_hash"),
                index("biz_project_memory_candidate_evidence", "idx_memory_evidence_parse"),
                constraint("biz_project_memory_candidate_evidence", "fk_memory_evidence_order"),
                constraint("biz_project_memory_candidate_evidence", "fk_memory_evidence_parse"),
                foreignKeyDeleteRule("biz_project_memory_candidate_evidence", "fk_memory_evidence_order",
                        "SET NULL"),
                foreignKeyDeleteRule("biz_project_memory_candidate_evidence", "fk_memory_evidence_parse",
                        "SET NULL"),
                table("biz_project_memory_learning_outbox"),
                index("biz_project_memory_learning_outbox", "uk_memory_learning_event"),
                index("biz_project_memory_learning_outbox", "idx_memory_learning_due"),
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
                trigger("trg_inventory_transaction_no_delete"),
                column("biz_finish_roll", "ownership_status"),
                column("biz_finish_roll", "remain_own_weight"),
                column("biz_finish_roll", "remain_transfer_state"),
                index("biz_finish_roll", "idx_finish_ownership"),
                table("biz_remain_registration"),
                index("biz_remain_registration", "uk_remain_registration_request"),
                table("biz_remain_registration_line"),
                index("biz_remain_registration_line", "uk_remain_line_active_source"),
                table("biz_remain_inventory_lot"),
                table("biz_remain_inventory_ledger"),
                index("biz_remain_inventory_ledger", "uk_remain_ledger_request"),
                table("biz_remain_price_version"),
                table("biz_remain_application"),
                column("biz_remain_application", "adjustment_uuid"),
                index("biz_remain_application", "uk_remain_application_request"),
                table("biz_remain_application_line"),
                table("biz_remain_adjustment"),
                index("biz_remain_adjustment", "uk_remain_adjustment_request"),
                table("biz_remain_adjustment_line"),
                table("biz_remain_customer_credit_account"),
                index("biz_remain_customer_credit_account", "uk_remain_credit_account_customer"),
                table("biz_remain_customer_credit_ledger"),
                index("biz_remain_customer_credit_ledger", "uk_remain_credit_ledger_request"),
                table("biz_remain_refund"),
                index("biz_remain_refund", "uk_remain_refund_request"),
                table("biz_remain_sale"),
                index("biz_remain_sale", "uk_remain_sale_request"),
                table("biz_remain_sale_line"),
                column("biz_receive_record", "source_type"),
                column("biz_receive_record", "remain_application_uuid"),
                index("biz_receive_record", "idx_receive_source_type")
        );
    }

    private static SchemaRequirement table(String table) {
        return new SchemaRequirement(SchemaRequirement.Kind.TABLE, table, table);
    }

    private static SchemaRequirement column(String table, String column) {
        return new SchemaRequirement(SchemaRequirement.Kind.COLUMN, table, column);
    }

    private static SchemaRequirement nullableColumn(String table, String column) {
        return new SchemaRequirement(SchemaRequirement.Kind.COLUMN_NULLABILITY, table, column);
    }

    private static SchemaRequirement index(String table, String index) {
        return new SchemaRequirement(SchemaRequirement.Kind.INDEX, table, index);
    }

    private static SchemaRequirement indexColumns(String table, String index, String columns) {
        return new SchemaRequirement(SchemaRequirement.Kind.INDEX_COLUMNS, table, index + "=" + columns);
    }

    private static SchemaRequirement constraint(String table, String constraint) {
        return new SchemaRequirement(SchemaRequirement.Kind.CONSTRAINT, table, constraint);
    }

    private static SchemaRequirement foreignKeyDeleteRule(String table, String constraint, String rule) {
        return new SchemaRequirement(SchemaRequirement.Kind.FOREIGN_KEY_DELETE_RULE,
                table, constraint + "=" + rule);
    }

    private static SchemaRequirement trigger(String trigger) {
        return new SchemaRequirement(SchemaRequirement.Kind.TRIGGER, "", trigger);
    }
}
