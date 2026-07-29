import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import type { AvailableFinishVO } from '../../types/delivery'
import { buildDeliveryCreateDTO, type DeliveryCreateFormValues } from './deliveryCreateSubmit'

describe('出库单创建参数', () => {
  it('保留货主并单独提交去除首尾空格的客户名称', () => {
    const dto = buildDeliveryCreateDTO(options('  永丰包装  '))

    expect(dto.customerUuid).toBe('owner-1')
    expect(dto.receiverCustomerName).toBe('永丰包装')
  })

  it('客户名称为空白时不提交该字段', () => {
    const dto = buildDeliveryCreateDTO(options('   '))

    expect(dto.receiverCustomerName).toBeUndefined()
  })
})

function options(receiverCustomerName: string) {
  const values: DeliveryCreateFormValues = {
    customerUuid: 'owner-1',
    receiverCustomerName,
    warehouseUuid: 'warehouse-1',
    deliveryDate: dayjs('2026-07-29'),
  }
  return {
    forceRelease: false,
    lineEdits: {},
    selectedFinishes: [finish()],
    values,
  }
}

function finish(): AvailableFinishVO {
  return {
    finishUuid: 'finish-1',
    finishRollNo: 'A000001',
    orderUuid: 'order-1',
    orderNo: 'JG001',
    paperName: '白卡',
    actualWeight: 1000,
    sourceType: 1,
    finishStatus: 2,
  }
}
