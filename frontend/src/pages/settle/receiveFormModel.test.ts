import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import {
  buildReceiveDTO,
  receiveTotalError,
  settledAmount,
} from './receiveFormModel'

describe('收款表单金额模型', () => {
  it('现金1790元加优惠1元按1791元结清', () => {
    const values = { cashAmount: 1790, discountAmount: 1, scrapOffsetAmount: 0 }

    expect(settledAmount(values)).toBe(1791)
    expect(receiveTotalError(values, 1791)).toBeUndefined()
  })

  it('构建请求时分开保留现金和优惠金额', () => {
    const dto = buildReceiveDTO({
      cashAmount: 1790,
      discountAmount: 1,
      payMethod: 2,
      payNo: ' PAY-001 ',
      receiveDate: dayjs('2026-07-15T10:30:00'),
    })

    expect(dto).toMatchObject({
      receiveAmount: 1791,
      cashAmount: 1790,
      discountAmount: 1,
      payMethod: 2,
      payNo: 'PAY-001',
      receiveDate: '2026-07-15T10:30:00',
    })
  })

  it('现金和优惠合计超过未收金额时拒绝提交', () => {
    const error = receiveTotalError({ cashAmount: 1790, discountAmount: 1.01 }, 1791)

    expect(error).toBe('本次结清金额不能超过未收金额')
  })
})
