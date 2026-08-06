import { Form, type FormInstance } from 'antd'
import type { DraftOrderBaseDTO } from '../../../types/processOrder'
import type { OrderSettlementMode } from '../../../types/processOrder'
import {
  DICT_TYPES,
  invoiceFallbackOptions,
  priorityFallbackOptions,
  settleFallbackOptions,
} from '../../systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../systemConfig/hooks/useRuntimeDictOptions'
import type { ReferenceOption } from '../types'
import type { ProcessOrderSettlementMode } from '../../../types/settlementSemantics'
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
  customerVersionStale: boolean
  settleOptions: ReturnType<typeof useNumberDictOptions>['options']
  settleMode?: OrderSettlementMode
  settleType?: ProcessOrderSettlementMode
  onCustomerChange: (customerUuid: string) => void
  onSettleModeChange: (mode: OrderSettlementMode) => void
  refreshCustomerSettlement: () => void
  onValuesChange: (_: Partial<BaseInfoFormValues>, values: BaseInfoFormValues) => void
}

export function useBaseInfoStepForm({ customers, onChange }: Options): BaseInfoFormSession {
  const [form] = Form.useForm<BaseInfoFormValues>()
  const { options: priorityOptions } = useNumberDictOptions(DICT_TYPES.priority, priorityFallbackOptions)
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const { options: settleOptions } = useNumberDictOptions(DICT_TYPES.settleType, settleFallbackOptions)
  const customerUuid = Form.useWatch('customerUuid', form)
  const settleType = Form.useWatch('settleType', form)
  const settleMode = Form.useWatch('settleMode', form)
  const customerVersion = Form.useWatch('customerVersion', form)
  const selectedCustomer = customers.find((item) => item.value === customerUuid)
  const customerVersionStale = Boolean(
    settleMode && selectedCustomer?.version != null && selectedCustomer.version !== customerVersion,
  )
  const onCustomerChange = (uuid: string) => applyCustomerDefaults({ customers, form, onChange, uuid })
  const onSettleModeChange = (mode: OrderSettlementMode) => applySettleMode({ customers, form, mode, onChange })
  const refreshCustomerSettlement = () => confirmLatestCustomerSettlement({ customers, form, onChange })
  const onValuesChange = (_: Partial<BaseInfoFormValues>, values: BaseInfoFormValues) => onChange?.(toBaseInfoDto(values))

  return {
    customerVersionStale, form, invoiceOptions, priorityOptions, refreshCustomerSettlement,
    selectedCustomer, settleMode, settleOptions, settleType, onCustomerChange, onSettleModeChange,
    onValuesChange,
  }
}

interface CustomerDefaultsOptions extends Options {
  form: FormInstance<BaseInfoFormValues>
  uuid: string
}

function applyCustomerDefaults({ customers, form, onChange, uuid }: CustomerDefaultsOptions) {
  const customer = customers.find((item) => item.value === uuid)
  if (!customer) return
  const settleType = customer.settleType
  const patch: Partial<BaseInfoFormValues> = {
    customerVersion: customer.version,
    isInvoice: customer.defaultInvoice ?? 2,
    settleDay: settleType === 2 ? customer.settleDay : undefined,
    settleMode: 'INHERIT',
    settleOverrideReason: undefined,
    settleType,
    taxRate: customer.taxRate,
  }
  form.setFieldsValue(patch)
  onChange?.(toBaseInfoDto({ ...form.getFieldsValue(), ...patch, customerUuid: uuid }))
}

interface SettleModeOptions extends Options {
  form: FormInstance<BaseInfoFormValues>
  mode: OrderSettlementMode
}

function applySettleMode({ customers, form, mode, onChange }: SettleModeOptions) {
  const uuid = form.getFieldValue('customerUuid')
  const customer = customers.find((item) => item.value === uuid)
  if (!customer) return
  if (mode === 'INHERIT') {
    applyCustomerDefaults({ customers, form, onChange, uuid })
    return
  }
  const patch: Partial<BaseInfoFormValues> = { customerVersion: customer.version, settleMode: mode }
  form.setFieldsValue(patch)
  onChange?.(toBaseInfoDto({ ...form.getFieldsValue(), ...patch }))
}

function confirmLatestCustomerSettlement({ customers, form, onChange }: Options & {
  form: FormInstance<BaseInfoFormValues>
}) {
  const values = form.getFieldsValue()
  const customer = customers.find((item) => item.value === values.customerUuid)
  if (!customer) return
  if (values.settleMode !== 'OVERRIDE') {
    applyCustomerDefaults({ customers, form, onChange, uuid: customer.value })
    return
  }
  const patch = { customerVersion: customer.version }
  form.setFieldsValue(patch)
  onChange?.(toBaseInfoDto({ ...values, ...patch }))
}
