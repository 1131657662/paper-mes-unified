import { IS_INVOICE, ORDER_SETTLE_TYPE, PRIORITY } from '../../constants/processOrder'
import type { DictSelectOption } from './hooks/useRuntimeDictOptions'

export const DICT_TYPES = {
  abnormalType: 'abnormal_type',
  feeType: 'fee_type',
  invoiceType: 'invoice_type',
  priority: 'priority',
  settleType: 'settle_type',
} as const

export const CONFIG_KEYS = {
  autoFinishConfig: 'process.autoFinishConfig',
  defaultPageSize: 'ui.defaultPageSize',
  processOrderTitle: 'print.processOrderTitle',
  spareRollNoCount: 'process.spareRollNoCount',
  weightTolerancePercent: 'process.weightTolerancePercent',
} as const

export const priorityFallbackOptions = numericOptions(PRIORITY)
export const invoiceFallbackOptions = numericOptions(IS_INVOICE)
export const settleFallbackOptions = numericOptions(ORDER_SETTLE_TYPE)

export const abnormalFallbackOptions: DictSelectOption[] = [
  { code: 'damage', label: '损伤', value: 'damage' },
  { code: 'weight_diff', label: '重量偏差', value: 'weight_diff' },
]

function numericOptions(dict: Record<number, string>): DictSelectOption[] {
  return Object.entries(dict).map(([value, label]) => ({
    code: value,
    label,
    value: Number(value),
  }))
}
