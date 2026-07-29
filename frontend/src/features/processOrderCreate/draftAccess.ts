import type { DraftOrderVO } from '../../types/processOrder'

export function nonDraftOrderUuid(
  requestedUuid: string | undefined,
  draft: DraftOrderVO | undefined,
): string | undefined {
  if (!requestedUuid || !draft?.order) return undefined
  return draft.order.orderStatus === 0 ? undefined : requestedUuid
}
