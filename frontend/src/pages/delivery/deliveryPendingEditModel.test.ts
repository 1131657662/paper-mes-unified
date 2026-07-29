import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import type { DeliveryOrder } from '../../types/delivery'
import {
  buildDeliveryPendingUpdateDTO,
  deliveryPendingEditInitialValues,
} from './deliveryPendingEditModel'

describe('待出库信息编辑模型', () => {
  it('使用当前单据初始化编辑表单', () => {
    const values = deliveryPendingEditInitialValues(order())

    expect(values.deliveryDate.format('YYYY-MM-DD')).toBe('2026-07-29')
    expect(values.carNo).toBe('浙A12345')
    expect(values.receiverCustomerName).toBe('永丰包装')
  })

  it('保存时修剪文本并将空白字段转为空值', () => {
    const dto = buildDeliveryPendingUpdateDTO({
      deliveryDate: dayjs('2026-07-30'),
      receiverCustomerName: ' 永丰包装 ',
      pickerName: '   ',
      carNo: ' 浙B67890 ',
      containerNo: '',
      remark: ' 等待车辆到厂 ',
    })

    expect(dto).toEqual({
      deliveryDate: '2026-07-30',
      receiverCustomerName: '永丰包装',
      pickerName: undefined,
      carNo: '浙B67890',
      containerNo: undefined,
      remark: '等待车辆到厂',
    })
  })
})

function order(): DeliveryOrder {
  return {
    uuid: 'delivery-1',
    deliveryNo: 'CK001',
    customerUuid: 'customer-1',
    customerName: '拓翔',
    receiverCustomerName: '永丰包装',
    deliveryDate: '2026-07-29',
    totalCount: 1,
    totalWeight: 1000,
    carNo: '浙A12345',
    settleBlockAction: 0,
    deliveryStatus: 1,
  }
}
