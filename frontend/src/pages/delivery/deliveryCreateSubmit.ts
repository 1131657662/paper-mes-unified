import type { Dayjs } from 'dayjs'
import type { AvailableFinishVO, DeliveryCreateDTO } from '../../types/delivery'
import type { DeliveryLineEdit } from './deliverySelectionModel'

export interface DeliveryCreateFormValues {
  carNo?: string
  containerNo?: string
  customerUuid: string
  receiverCustomerName?: string
  warehouseUuid: string
  deliveryDate: Dayjs
  pickerName?: string
  remark?: string
}

interface BuildDeliveryCreateDTOOptions {
  forceRelease: boolean
  lineEdits: Record<string, DeliveryLineEdit>
  selectedFinishes: AvailableFinishVO[]
  values: DeliveryCreateFormValues
}

export function buildDeliveryCreateDTO(
  options: BuildDeliveryCreateDTOOptions,
): DeliveryCreateDTO {
  const { forceRelease, lineEdits, selectedFinishes, values } = options
  return {
    carNo: values.carNo,
    containerNo: values.containerNo,
    customerUuid: values.customerUuid,
    receiverCustomerName: values.receiverCustomerName?.trim() || undefined,
    warehouseUuid: values.warehouseUuid,
    deliveryDate: values.deliveryDate.format('YYYY-MM-DD'),
    forceRelease,
    items: selectedFinishes.map((item) => ({
      finishUuid: item.finishUuid,
      outWeight: lineEdits[item.finishUuid]?.outWeight,
      remark: lineEdits[item.finishUuid]?.remark,
    })),
    pickerName: values.pickerName,
    remark: values.remark,
  }
}
