import type { Dayjs } from 'dayjs'
import type { ReceiveDTO } from '../../types/settle'

export interface ReceiveFormValues {
  cashAmount?: number
  discountAmount?: number
  discountReason?: string
  discountApprovalUuid?: string
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
    discountReason: cleanText(values.discountReason),
    discountApprovalUuid: cleanText(values.discountApprovalUuid),
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
    return '实际到账金额大于 0 时必须选择到账方式'
  }
  return undefined
}

export function payNoError(values: ReceiveFormValues): string | undefined {
  const requiresPayNo = Number(values.cashAmount ?? 0) > 0 && [2, 3, 4].includes(values.payMethod ?? 0)
  if (requiresPayNo && !values.payNo?.trim()) return '非现金到账必须填写交易流水号'
  return undefined
}

export function discountReasonError(values: ReceiveFormValues): string | undefined {
  if (Number(values.discountAmount ?? 0) > 0 && !values.discountReason?.trim()) {
    return '优惠/尾差核销必须填写原因'
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
