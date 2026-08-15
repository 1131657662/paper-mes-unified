import { getProcessOrder } from '../../../api/processOrder'
import type { OriginalRoll, ProcessRollDispositionAction, ProcessRollDispositionDTO } from '../../../types/processOrder'

export interface ProcessRollDispositionFormValues {
  rollUuids?: string[]
  action: ProcessRollDispositionAction
  reason: string
  warehouseUuid?: string
  actualWeight?: number
}

/** Single-roll actions must never retain a stale multi-selection from CANCEL. */
export function normalizeRollSelection(
  action: ProcessRollDispositionAction,
  rollUuids: string[],
): string[] {
  return action === 'CANCEL' ? rollUuids : rollUuids.slice(0, 1)
}

interface DisposeSelectedRollsParams {
  rollUuids: string[]
  orderUuid: string
  expectedOrderVersion: number
  values: ProcessRollDispositionFormValues
  dispose: (params: { rollUuid: string; orderUuid: string; dto: ProcessRollDispositionDTO }) => Promise<unknown>
  requestIdFor: (key: string) => string
  onApplied: (count: number) => void
}

export async function disposeSelectedRolls(params: DisposeSelectedRollsParams): Promise<void> {
  let expectedVersion = params.expectedOrderVersion
  for (let index = 0; index < params.rollUuids.length; index += 1) {
    const rollUuid = params.rollUuids[index]
    if (!rollUuid) continue
    await params.dispose({
      rollUuid,
      orderUuid: params.orderUuid,
      dto: buildDispositionDto(
        params.values,
        expectedVersion,
        params.requestIdFor(dispositionRequestKey(rollUuid, params.values)),
      ),
    })
    params.onApplied(index + 1)
    if (index === params.rollUuids.length - 1) continue
    const latest = await getProcessOrder(params.orderUuid)
    if (latest.order.version == null) throw new Error('处置后未返回最新加工单版本')
    expectedVersion = latest.order.version
  }
}

export function buildDispositionDto(
  values: ProcessRollDispositionFormValues,
  expectedOrderVersion: number,
  requestId: string = crypto.randomUUID(),
): ProcessRollDispositionDTO {
  return {
    action: values.action,
    requestId,
    reason: values.reason.trim(),
    expectedOrderVersion,
    warehouseUuid: values.action === 'DIRECT_SHIP' ? values.warehouseUuid : undefined,
    actualWeight: values.action === 'DIRECT_SHIP' ? values.actualWeight : undefined,
  }
}

export function dispositionRequestKey(
  rollUuid: string,
  values: ProcessRollDispositionFormValues,
): string {
  return JSON.stringify([
    rollUuid,
    values.action,
    values.reason.trim(),
    values.warehouseUuid ?? null,
    values.actualWeight ?? null,
  ])
}

export function eligibleRolls(rolls: OriginalRoll[]): OriginalRoll[] {
  return rolls.filter((roll) => roll.dispositionAction == null
    && roll.isChecked !== 1 && ![3, 4, 5].includes(roll.rollStatus ?? 0))
}

export function successText(action: ProcessRollDispositionAction, count: number): string {
  if (action === 'DIRECT_SHIP') return '母卷已转直发并生成入库成品，计费已重算'
  if (action === 'SPLIT_TO_ORDER') return '母卷已拆分到新的待下发代加工单'
  return count > 1 ? `已取消 ${count} 卷母卷本次加工，计费已重算` : '母卷已取消本次加工，计费已重算'
}
