import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import type { DraftOrderBaseDTO } from '../../../types/processOrder'

export interface BaseInfoFormValues extends Omit<DraftOrderBaseDTO, 'customerUuid' | 'orderDate' | 'expectFinishDate'> {
  customerUuid?: string
  orderDate?: Dayjs
  expectFinishDate?: Dayjs
}

export function toBaseInfoDto(value: BaseInfoFormValues): DraftOrderBaseDTO {
  return {
    ...value,
    customerUuid: value.customerUuid ?? '',
    orderDate: value.orderDate ? value.orderDate.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD'),
    expectFinishDate: value.expectFinishDate?.format('YYYY-MM-DD'),
    settleDay: value.settleType === 2 ? value.settleDay : undefined,
  }
}

export function baseInfoInitialValues(initialValue?: DraftOrderBaseDTO): BaseInfoFormValues {
  return {
    ...initialValue,
    orderDate: initialValue?.orderDate ? dayjs(initialValue.orderDate) : dayjs(),
    expectFinishDate: initialValue?.expectFinishDate ? dayjs(initialValue.expectFinishDate) : undefined,
    priority: initialValue?.priority ?? 1,
    isInvoice: initialValue?.isInvoice ?? 2,
    settleType: initialValue?.settleType ?? 2,
  }
}
