import { Form } from 'antd'
import { useEffect } from 'react'
import type { FormInstance } from 'antd'
import type { ProcessStepDTO } from '../../api/processOrder'
import type { Machine } from '../../types/machine'
import type { CustomerProcessPrice } from '../../types/customer'
import { useProcessCatalog } from '../../features/processCatalog/hooks/useProcessCatalog'
import { useMachines } from '../../features/processOrderCreate/hooks/useReferenceData'
import {
  suggestedMachineUuid,
  type MachineContext,
} from '../../features/processOrderCreate/machineDefaults'
import {
  findProcessCatalog,
  processStepInitialValues,
  processStepPayload,
  processTypeDefaults,
  customerPriceFormPatch,
  serviceStepCatalogs,
  type ProcessStepFormValues,
} from './processStepCatalogModel'

const EMPTY_MACHINES: Machine[] = []

export interface ProcessStepFormOptions {
  initialValues?: ProcessStepDTO & { uuid?: string }
  defaultOriginalUuid?: string
  defaultOriginalUuids?: string[]
  batchMode?: boolean
  extraOnly: boolean
  customerPrices?: CustomerProcessPrice[]
  machineContext?: MachineContext
  onCancel: () => void
  onOk: (values: ProcessStepDTO, stepUuid?: string) => Promise<void>
  onBatchOk?: (values: ProcessStepDTO, originalUuids: string[]) => Promise<void>
}

export function useProcessStepFormState(options: ProcessStepFormOptions) {
  const [form] = Form.useForm<ProcessStepFormValues>()
  const stepType = Form.useWatch('stepType', form)
  const billingMode = Form.useWatch('billingMode', form)
  const billingBasis = Form.useWatch('billingBasis', form)
  const originalUuids = Form.useWatch('originalUuids', form)
  const query = useProcessCatalog()
  const catalogs = serviceStepCatalogs(query.data, options.extraOnly)
  const { data: machinePage, isLoading: isLoadingMachines } = useMachines()
  const machines = machinePage?.records ?? EMPTY_MACHINES
  const machineDiameter = options.machineContext?.diameter
  const machineWeight = options.machineContext?.weight
  const machineWidth = options.machineContext?.width
  const selectedCatalog = findProcessCatalog(catalogs, stepType)
  useEffect(() => {
    if (isLoadingMachines || stepType == null) return
    const currentMachineUuid = form.getFieldValue('machineUuid')
    const machineUuid = suggestedMachineUuid({
      mainStepType: stepType,
      currentMachineUuid,
      machines,
      context: { diameter: machineDiameter, weight: machineWeight, width: machineWidth },
    })
    if (machineUuid !== currentMachineUuid) form.setFieldValue('machineUuid', machineUuid)
  }, [
    form,
    isLoadingMachines,
    machines,
    machineDiameter,
    machineWeight,
    machineWidth,
    stepType,
  ])
  const submit = () => submitForm({ options, form, selectedCatalog })
  const submitWith = (onValid: (values: ProcessStepDTO) => Promise<void>) => (
    submitForm({ options, form, selectedCatalog }, onValid)
  )
  const change = (changed: Partial<ProcessStepFormValues>) => applyFormChange({
    changed,
    catalogs,
    customerPrices: options.customerPrices,
    machines: machinePage?.records ?? [],
    form,
  })
  return {
    form, billingMode, billingBasis, originalUuids, selectedCatalog, submit, submitWith, change,
    catalogs, isLoading: query.isLoading,
    machines, isLoadingMachines,
    isError: query.isError, refetch: query.refetch,
    initialValues: processStepInitialValues(options),
  }
}

interface SubmitOptions {
  options: ProcessStepFormOptions
  form: FormInstance<ProcessStepFormValues>
  selectedCatalog: ReturnType<typeof findProcessCatalog>
}

async function submitForm(
  { options, form, selectedCatalog }: SubmitOptions,
  onValid?: (values: ProcessStepDTO) => Promise<void>,
) {
  try {
    const values = await form.validateFields()
    if (!selectedCatalog) return
    const payload = processStepPayload({ values, catalog: selectedCatalog, extraOnly: options.extraOnly })
    if (onValid) {
      await onValid(payload)
    } else if (options.batchMode && options.onBatchOk) {
      await options.onBatchOk(payload, values.originalUuids ?? [])
    } else {
      await options.onOk(payload, options.initialValues?.uuid)
    }
    options.onCancel()
  } catch {
    // Ant Design keeps field and request errors visible at their source.
  }
}

interface TypeChangeState {
  changed: Partial<ProcessStepFormValues>
  catalogs: ReturnType<typeof useProcessCatalog>['data']
  form: FormInstance<ProcessStepFormValues>
  machines: Machine[]
  customerPrices?: CustomerProcessPrice[]
}

function applyFormChange({ changed, catalogs, customerPrices, form, machines }: TypeChangeState) {
  if (changed.stepType !== undefined) {
    applyTypeChange(changed.stepType, catalogs, customerPrices, form, machines ?? [])
    return
  }
  const catalog = findProcessCatalog(catalogs, form.getFieldValue('stepType'))
  if (!catalog) return
  if (changed.billingMode !== undefined || changed.billingBasis !== undefined) {
    form.setFieldsValue(customerPriceFormPatch({
      catalog,
      prices: customerPrices,
      billingMode: changed.billingMode ?? form.getFieldValue('billingMode'),
      billingBasis: changed.billingBasis ?? form.getFieldValue('billingBasis'),
    }))
  }
}

function applyTypeChange(stepType: number, catalogs: TypeChangeState['catalogs'],
  customerPrices: TypeChangeState['customerPrices'], form: TypeChangeState['form'],
  machines: TypeChangeState['machines']) {
  const catalog = findProcessCatalog(catalogs, stepType)
  const defaults = processTypeDefaults({
    catalog,
    currentBillingMode: form.getFieldValue('billingMode'),
    currentUnit: form.getFieldValue('billingBasis'),
    currentIsMain: form.getFieldValue('isMain'),
  })
  const pricePatch = customerPriceFormPatch({ catalog, prices: customerPrices, preferDefault: true })
  form.setFieldsValue({
    knifeCount: undefined,
    processWeight: undefined,
    serviceQuantity: undefined,
    billingAmount: undefined,
    unitPrice: undefined,
    ...defaults,
    ...pricePatch,
    machineUuid: suggestedMachineUuid({ mainStepType: stepType, machines }),
  })
}
