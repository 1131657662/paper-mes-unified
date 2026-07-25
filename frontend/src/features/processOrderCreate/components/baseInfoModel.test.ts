import { describe, expect, it } from 'vitest'
import { baseInfoInitialValues, toBaseInfoDto } from './baseInfoModel'

describe('加工单基础信息结算来源', () => {
  it('新建页面未选择客户时不预设月结', () => {
    const values = baseInfoInitialValues()

    expect(values.settleType).toBeUndefined()
    expect(values.settleMode).toBeUndefined()
  })

  it('跟随客户时不提交残留的覆盖原因', () => {
    const dto = toBaseInfoDto({
      customerUuid: 'customer-1',
      customerVersion: 3,
      settleMode: 'INHERIT',
      settleType: 1,
      settleOverrideReason: '旧草稿原因',
    })

    expect(dto).toMatchObject({
      customerUuid: 'customer-1', customerVersion: 3, settleMode: 'INHERIT', settleType: 1,
    })
    expect(dto.settleOverrideReason).toBeUndefined()
  })

  it('本单覆盖时修剪覆盖原因', () => {
    const dto = toBaseInfoDto({
      customerUuid: 'customer-1',
      customerVersion: 3,
      settleMode: 'OVERRIDE',
      settleType: 2,
      settleDay: 25,
      settleOverrideReason: '  合同约定  ',
    })

    expect(dto.settleOverrideReason).toBe('合同约定')
  })
})
