import { Modal } from 'antd'
import type { Dayjs } from 'dayjs'
import type { AvailableFinishVO, DeliveryCreateDTO } from '../../types/delivery'
import type { DeliveryLineEdit } from './deliverySelectionModel'

export interface DeliveryCreateFormValues {
  carNo?: string
  containerNo?: string
  customerUuid: string
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

export function confirmDeliveryCashRelease(): Promise<boolean> {
  return new Promise<boolean>((resolve) => {
    Modal.confirm({
      title: '次结出库确认',
      content: '本次选择包含次结且有待收款风险的加工单。确认后将按“警告放行”生成出库单。',
      okText: '警告放行',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}
