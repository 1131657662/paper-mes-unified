import dayjs, { type Dayjs } from 'dayjs'
import type { DeliveryOrder } from '../../types/delivery'
import type { DeliveryPendingUpdateDTO } from '../../types/deliveryPendingUpdate'

export interface DeliveryPendingEditFormValues {
  receiverCustomerName?: string
  deliveryDate: Dayjs
  pickerName?: string
  carNo?: string
  containerNo?: string
  remark?: string
}

export function deliveryPendingEditInitialValues(
  order: DeliveryOrder,
): DeliveryPendingEditFormValues {
  return {
    receiverCustomerName: order.receiverCustomerName,
    deliveryDate: dayjs(order.deliveryDate),
    pickerName: order.pickerName,
    carNo: order.carNo,
    containerNo: order.containerNo,
    remark: order.remark,
  }
}

export function buildDeliveryPendingUpdateDTO(
  values: DeliveryPendingEditFormValues,
): DeliveryPendingUpdateDTO {
  return {
    receiverCustomerName: optionalText(values.receiverCustomerName),
    deliveryDate: values.deliveryDate.format('YYYY-MM-DD'),
    pickerName: optionalText(values.pickerName),
    carNo: optionalText(values.carNo),
    containerNo: optionalText(values.containerNo),
    remark: optionalText(values.remark),
  }
}

function optionalText(value?: string): string | undefined {
  return value?.trim() || undefined
}
