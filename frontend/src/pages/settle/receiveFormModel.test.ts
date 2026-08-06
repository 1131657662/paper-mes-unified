import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import {
  buildReceiveDTO,
  discountReasonError,
  payNoError,
  receiveTotalError,
  resolveReceiveAmountChange,
  settledAmount,
} from './receiveFormModel'

describe('收款表单金额模型', () => {
  it('实际到账1790元加优惠1元按1791元结清', () => {
    const values = { cashAmount: 1790, discountAmount: 1, scrapOffsetAmount: 0 }

    expect(settledAmount(values)).toBe(1791)
    expect(receiveTotalError(values, 1791)).toBeUndefined()
  })

  it('构建请求时分开保留实际到账和优惠审批信息', () => {
    const dto = buildReceiveDTO({
      cashAmount: 1790,
      discountAmount: 1,
      payMethod: 2,
      payNo: ' PAY-001 ',
      discountReason: ' 客户确认尾差 ',
      discountApprovalUuid: 'approval-1',
      receiveDate: dayjs('2026-07-15T10:30:00'),
    })

    expect(dto).toMatchObject({
      receiveAmount: 1791,
      cashAmount: 1790,
      discountAmount: 1,
      payMethod: 2,
      payNo: 'PAY-001',
      discountReason: '客户确认尾差',
      discountApprovalUuid: 'approval-1',
      receiveDate: '2026-07-15T10:30:00',
    })
  })

  it('非现金到账缺少交易流水号时拒绝提交', () => {
    expect(payNoError({ cashAmount: 100, payMethod: 2 })).toBe('非现金到账必须填写交易流水号')
    expect(payNoError({ cashAmount: 100, payMethod: 1 })).toBeUndefined()
  })

  it('优惠金额大于零时要求填写原因', () => {
    expect(discountReasonError({ discountAmount: 1 })).toBe('优惠/尾差核销必须填写原因')
    expect(discountReasonError({ discountAmount: 1, discountReason: '尾差' })).toBeUndefined()
  })

  it('现金和优惠合计超过未收金额时拒绝提交', () => {
    const error = receiveTotalError({ cashAmount: 1790, discountAmount: 1.01 }, 1791)

    expect(error).toBe('本次结清金额不能超过未收金额')
  })

  it('同一结算单的未收金额变化时返回前后金额', () => {
    const change = resolveReceiveAmountChange({
      baseline: { settleUuid: 'settle-1', unreceivedAmount: 2274 },
      settleUuid: 'settle-1',
      unreceivedAmount: 2100.5,
    })

    expect(change).toEqual({ previousAmount: 2274, currentAmount: 2100.5 })
  })

  it('切换结算单时不把金额差异误判为外部更新', () => {
    const change = resolveReceiveAmountChange({
      baseline: { settleUuid: 'settle-1', unreceivedAmount: 2274 },
      settleUuid: 'settle-2',
      unreceivedAmount: 1325,
    })

    expect(change).toBeNull()
  })
})
