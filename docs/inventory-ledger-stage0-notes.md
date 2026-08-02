# Inventory Ledger Stage 0 Notes

The V3.52 ledger foundation is intentionally isolated from existing stock write paths. The current projection combines `biz_finish_roll.remaining_weight` with active `biz_delivery_detail` locks. A reservation event and a shipment event therefore cannot be wired blindly to the same single balance: doing so would double subtract weight. The new ledger stores physical and reserved balances separately and exposes `available_*` balances as their difference.

`InventoryLedgerService.openBalance` is the explicit switch-day entry point for the first row of a finish roll. It accepts a zero opening balance to anchor a roll without fabricating pre-switch history. `InventoryOpeningReconciliation` and its line type are a comparison contract only; they require the caller to provide the checked legacy projection and opening values. No service reads old snapshots or reconstructs pre-switch events.

`DeliveryServiceImpl` and first-stock-in handling now map create/append/confirm/rollback/cancel and receipt operations to reserve/release/issue/return/RECEIPT events atomically. The recorder is a required constructor dependency and no production path silently skips ledger writes. Finished-goods scrap uses a dedicated permission-protected command with reason validation and an operation-log hook. The `ADJUSTMENT` event is supported by the immutable schema and validator, but no adjustment command or UI is exposed until its business value, scope and impact are explicitly confirmed.

The V3.52 table and constraints are mirrored in the canonical baseline (`sql/01_schema_v4.1.sql`), and the schema-gate contracts require the append-only triggers and balance checks. The migration was replayed successfully against a disposable MySQL 8.0 database; publication still requires the release-environment replay evidence.

Controlled finished-roll scrap has a dedicated permission-protected command, occupancy checks, reason/request validation, operation log, and SCRAP event. The canonical baseline includes the ledger and `sql/schema-baseline.version` is `3.52`.

Back-record rollback now appends a compensating `ADJUSTMENT` for every finish roll that is moved from in-stock back to pending. The command runs before production facts are cleared, is guarded by delivery-activity checks, and uses the process-order version plus finish-roll UUID as its idempotency key. This covers completed-order reopen, partial-record rollback to pending, and rollback-to-draft cleanup without introducing a general adjustment API.

Receipt idempotency is versioned as `RECEIPT:<process-order-version>:<finish-roll-uuid>`. A finish roll that is reopened and recorded again therefore receives a new receipt event instead of reusing the original receipt key; missing order or batch identifiers are rejected.

Delivery issue/return idempotency is versioned by the delivery detail optimistic-lock version: `ISSUE:<detail-uuid>:<detail-version>` and `RETURN:<detail-uuid>:<detail-version>`. A confirm -> rollback -> confirm cycle therefore appends a fresh issue event after the return instead of reusing the first issue event.

The scope boundary keeps historical `WORKSTATION` resource rows readable for old process-order references, but new machine writes accept only `MACHINE`; the workstation edit entry is hidden in the PC UI and rejected again by the backend.

Stage 0 remains gated until the full P0 suite, a real MySQL baseline/replay diff including V3.52, and opening reconciliation evidence pass. Historical pre-switch snapshots are not converted into synthetic events, and no stage 1 readiness is claimed yet.
