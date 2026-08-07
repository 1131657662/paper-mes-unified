# Inventory Ledger Stage 0 Notes

The V3.52 ledger foundation is intentionally isolated from existing stock write paths. The current projection combines `biz_finish_roll.remaining_weight` with active `biz_delivery_detail` locks. A reservation event and a shipment event therefore cannot be wired blindly to the same single balance: doing so would double subtract weight. The new ledger stores physical and reserved balances separately and exposes `available_*` balances as their difference.

`InventoryLedgerService.openBalance` is the explicit switch-day entry point for the first row of a finish roll. It accepts a zero opening balance to anchor a roll without fabricating pre-switch history. `InventoryOpeningReconciliation` and its line type are a comparison contract only; they require the caller to provide the checked legacy projection and opening values. No service reads old snapshots or reconstructs pre-switch events.

`DeliveryServiceImpl` and first-stock-in handling now map create/append/confirm/rollback/cancel and receipt operations to reserve/release/issue/return/RECEIPT events atomically. The recorder is a required constructor dependency and no production path silently skips ledger writes. Finished-goods scrap uses a dedicated permission-protected command with reason validation and an operation-log hook. The `ADJUSTMENT` event is supported by the immutable schema and validator, but no adjustment command or UI is exposed until its business value, scope and impact are explicitly confirmed.

The V3.52 table and constraints are mirrored in the current 3.63 canonical baseline (`sql/01_schema_v4.1.sql`), and the schema-gate contracts require the append-only triggers and balance checks. The V3.50-V3.62 window was replayed successfully in disposable and release environments.

Controlled finished-roll scrap has a dedicated permission-protected command, occupancy checks, reason/request validation, operation log, and SCRAP event. The canonical baseline includes the ledger and `sql/schema-baseline.version` is `3.63`.

Back-record rollback now appends a compensating `ADJUSTMENT` for every finish roll that is moved from in-stock back to pending. The command runs before production facts are cleared, is guarded by delivery-activity checks, and uses the process-order version plus finish-roll UUID as its idempotency key. This covers completed-order reopen, partial-record rollback to pending, and rollback-to-draft cleanup without introducing a general adjustment API.

Receipt idempotency is versioned as `RECEIPT:<process-order-version>:<finish-roll-uuid>`. A finish roll that is reopened and recorded again therefore receives a new receipt event instead of reusing the original receipt key; missing order or batch identifiers are rejected.

Delivery issue/return idempotency is versioned by the delivery detail optimistic-lock version: `ISSUE:<detail-uuid>:<detail-version>` and `RETURN:<detail-uuid>:<detail-version>`. A confirm -> rollback -> confirm cycle therefore appends a fresh issue event after the return instead of reusing the first issue event.

The scope boundary keeps historical `WORKSTATION` resource rows readable for old process-order references, but new machine writes accept only `MACHINE`; the workstation edit entry is hidden in the PC UI and rejected again by the backend.

Stage 0 closed in production at `2026-08-03T09:48:30+08:00` after the full P0 suite, switch-day opening reconciliation, three-party sign-off, and core smoke checks passed. Historical pre-switch snapshots were not converted into synthetic events. Stage 1 may proceed from the production opening baseline, while the V1-V3.49 historical migration chain remains explicitly unverified.

The production cutover followed the controlled path: authenticated preview, fixed preview SHA-256, preflight, one formal opening call, postcheck, sign-off, smoke checks, and unfreeze. Evidence is retained under `/opt/paper-mes/releases/ec21502-stage0-prepared/cutover/bb75c2be-3a6c-44a8-936b-e4cf7473c0a1`; 721 rolls reconciled with zero differences at physical `41900.000 kg`, reserved `10780.000 kg`, and available `31120.000 kg`. The approved V1-V3.49 historical fixture is still unavailable, so no full historical migration-chain claim is made.
