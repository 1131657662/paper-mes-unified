import { Form, type FormInstance } from 'antd'
import type { DraftOrderBaseDTO } from '../../../types/processOrder'
import {
  DICT_TYPES,
  invoiceFallbackOptions,
  priorityFallbackOptions,
  settleFallbackOptions,
} from '../../systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../systemConfig/hooks/useRuntimeDictOptions'
import type { ReferenceOption } from '../types'
import { toBaseInfoDto, type BaseInfoFormValues } from './baseInfoModel'

interface Options {
  customers: ReferenceOption[]
  onChange?: (value: DraftOrderBaseDTO) => void
}

export interface BaseInfoFormSession {
  form: FormInstance<BaseInfoFormValues>
  invoiceOptions: ReturnType<typeof useNumberDictOptions>['options']
  priorityOptions: ReturnType<typeof useNumberDictOptions>['options']
  selectedCustomer?: ReferenceOption
  settleOptions: ReturnType<typeof useNumberDictOptions>['options']
  settleType?: number
  onCustomerChange: (customerUuid: string) => void
  onValuesChange: (_: Partial<BaseInfoFormValues>, values: BaseInfoFormValues) => void
}

export function useBaseInfoStepForm({ customers, onChange }: Options): BaseInfoFormSession {
  const [form] = Form.useForm<BaseInfoFormValues>()
  const { options: priorityOptions } = useNumberDictOptions(DICT_TYPES.priority, priorityFallbackOptions)
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const { options: settleOptions } = useNumberDictOptions(DICT_TYPES.settleType, settleFallbackOptions)
  const customerUuid = Form.useWatch('customerUuid', form)
  const settleType = Form.useWatch('settleType', form)
  const selectedCustomer = customers.find((item) => item.value === customerUuid)
  const onCustomerChange = (uuid: string) => applyCustomerDefaults({ customers, form, onChange, uuid })
  const onValuesChange = (_: Partial<BaseInfoFormValues>, values: BaseInfoFormValues) => onChange?.(toBaseInfoDto(values))

  return { form, invoiceOptions, priorityOptions, selectedCustomer, settleOptions, settleType, onCustomerChange, onValuesChange }
}

interface CustomerDefaultsOptions extends Options {
  form: FormInstance<BaseInfoFormValues>
  uuid: string
}

function applyCustomerDefaults({ customers, form, onChange, uuid }: CustomerDefaultsOptions) {
  const customer = customers.find((item) => item.value === uuid)
  if (!customer) return
  const settleType = customer.settleType ?? 2
  const patch: Partial<BaseInfoFormValues> = {
    isInvoice: customer.defaultInvoice ?? 2,
    settleDay: settleType === 2 ? customer.settleDay : undefined,
    settleType,
    taxRate: customer.taxRate,
  }
  form.setFieldsValue(patch)
  onChange?.(toBaseInfoDto({ ...form.getFieldsValue(), ...patch, customerUuid: uuid }))
}
