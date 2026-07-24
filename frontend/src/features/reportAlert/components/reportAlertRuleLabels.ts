import type {
  ReportAlertOperator,
  ReportAlertScopeType,
  ReportAlertSignalCode,
} from '../types'

export const signalOptions = [
  { label: '损耗率', value: 'LOSS_RATIO' },
  { label: '未收款占比', value: 'UNRECEIVED_RATIO' },
] satisfies Array<{ label: string; value: ReportAlertSignalCode }>

export const scopeOptions = [
  { label: '全局默认', value: 1 },
  { label: '指定客户', value: 2 },
  { label: '指定纸张', value: 3 },
  { label: '指定工艺', value: 4 },
] satisfies Array<{ label: string; value: ReportAlertScopeType }>

export const operatorOptions = [
  { label: '大于等于', value: 'GTE' },
  { label: '大于', value: 'GT' },
  { label: '小于等于', value: 'LTE' },
  { label: '小于', value: 'LT' },
] satisfies Array<{ label: string; value: ReportAlertOperator }>

export const severityOptions = [
  { label: '预警', value: 1 },
  { label: '严重', value: 2 },
] satisfies Array<{ label: string; value: 1 | 2 }>

export const processOptions = [
  { label: '锯纸', value: 1 },
  { label: '复卷', value: 2 },
] satisfies Array<{ label: string; value: 1 | 2 }>

export function optionLabel<T>(options: ReadonlyArray<{ label: string; value: T }>, value: T) {
  return options.find((item) => item.value === value)?.label ?? '-'
}
