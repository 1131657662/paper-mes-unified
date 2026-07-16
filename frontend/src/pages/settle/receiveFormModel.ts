import type { Dayjs } from 'dayjs'
import type { ReceiveDTO } from '../../types/settle'

export interface ReceiveFormValues {
  cashAmount?: number
  discountAmount?: number
  scrapOffsetAmount?: number
  scrapWeight?: number
  payMethod?: number
  payNo?: string
  receiveDate?: Dayjs
  remark?: string
}

export function buildReceiveDTO(values: ReceiveFormValues, requestId = crypto.randomUUID()): ReceiveDTO {
  return {
    requestId,
    receiveAmount: settledAmount(values),
    cashAmount: roundMoney(values.cashAmount),
    scrapOffsetAmount: roundMoney(values.scrapOffsetAmount),
    discountAmount: roundMoney(values.discountAmount),
    scrapWeight: roundWeight(values.scrapWeight),
    payMethod: Number(values.cashAmount ?? 0) > 0 ? values.payMethod : undefined,
    payNo: cleanText(values.payNo),
    receiveDate: values.receiveDate?.format('YYYY-MM-DDTHH:mm:ss'),
    remark: cleanText(values.remark),
  }
}

export function settledAmount(values: Pick<ReceiveFormValues,
  'cashAmount' | 'scrapOffsetAmount' | 'discountAmount'>): number {
  return roundMoney(Number(values.cashAmount ?? 0)
    + Number(values.scrapOffsetAmount ?? 0)
    + Number(values.discountAmount ?? 0))
}

export function receiveTotalError(values: ReceiveFormValues, unreceivedAmount: number): string | undefined {
  const total = settledAmount(values)
  if (total <= 0) return '本次结清金额必须大于 0'
  if (total > roundMoney(unreceivedAmount)) return '本次结清金额不能超过未收金额'
  return undefined
}

export function scrapWeightError(values: ReceiveFormValues): string | undefined {
  if (Number(values.scrapOffsetAmount ?? 0) > 0 && Number(values.scrapWeight ?? 0) <= 0) {
    return '废纸抵扣金额大于 0 时，废纸重量必须大于 0'
  }
  return undefined
}

export function payMethodError(values: ReceiveFormValues): string | undefined {
  if (Number(values.cashAmount ?? 0) > 0 && !values.payMethod) {
    return '现金实收金额大于 0 时必须选择收款方式'
  }
  return undefined
}

export function roundMoney(value?: number): number {
  return Math.round(Number(value || 0) * 100) / 100
}

export function roundWeight(value?: number): number {
  return Math.round(Number(value || 0) * 1000) / 1000
}

export function roundPrice(value: number): number {
  return Math.round(Number(value || 0) * 10000) / 10000
}

function cleanText(value?: string): string | undefined {
  const text = value?.trim()
  return text || undefined
}
