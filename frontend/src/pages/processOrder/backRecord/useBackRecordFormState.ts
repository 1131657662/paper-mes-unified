import { useCallback, useEffect, useRef, useState } from 'react'
import { Form } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { buildInitialOnSiteOutputGroups } from './backRecordOnSiteOutputModel'
import {
  initialBackRecordValues,
  type BackRecordFormValues,
} from './backRecordUtils'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'
import { useBackRecordDisplayValues } from './useBackRecordDisplayValues'

interface UseBackRecordFormStateOptions {
  detail?: ProcessOrderDetailVO
  enabled: boolean
}

export function useBackRecordFormState(options: UseBackRecordFormStateOptions) {
  const { detail, enabled } = options
  const [form] = Form.useForm<BackRecordFormValues>()
  const [filledValues, setFilledValues] = useState<BackRecordFormValues>({})
  const initializedOrderRef = useRef<string | null>(null)
  const initializedVersionRef = useRef<number | undefined>(undefined)
  const displayValues = useBackRecordDisplayValues(form, filledValues)
  const initialize = useCallback((nextDetail: ProcessOrderDetailVO) => {
    const initialValues = buildInitialValues(nextDetail)
    form.setFieldsValue(initialValues)
    setFilledValues(initialValues)
    initializedOrderRef.current = nextDetail.order.uuid
    initializedVersionRef.current = nextDetail.order.version
  }, [form])
  const refreshPreservingValues = useCallback((nextDetail: ProcessOrderDetailVO) => {
    const currentValues = form.getFieldsValue(true) as BackRecordFormValues
    const mergedValues = mergeFormValues(buildInitialValues(nextDetail), currentValues)
    form.setFieldsValue(mergedValues)
    setFilledValues(mergedValues)
    initializedOrderRef.current = nextDetail.order.uuid
    initializedVersionRef.current = nextDetail.order.version
  }, [form])
  const resetInitialization = useCallback(() => {
    initializedOrderRef.current = null
    initializedVersionRef.current = undefined
  }, [])

  useEffect(() => {
    const detailUuid = detail?.order.uuid
    if (enabled && detail && initializedOrderRef.current !== detailUuid) {
      initialize(detail)
    }
    if (!enabled) {
      resetInitialization()
      form.resetFields()
      setFilledValues({})
    }
  }, [detail, enabled, form, initialize, resetInitialization])

  return {
    displayValues,
    form,
    getInitializedVersion: () => initializedVersionRef.current,
    initialize,
    refreshPreservingValues,
    resetInitialization,
    syncFilledValues: setFilledValues,
  }
}

function buildInitialValues(detail: ProcessOrderDetailVO): BackRecordFormValues {
  const initialValues = initialBackRecordValues(detail)
  const workbench = buildBackRecordWorkbench(detail)
  initialValues.onSiteOutputs = buildInitialOnSiteOutputGroups(detail, workbench.items)
  return initialValues
}

function mergeFormValues(
  initialValues: BackRecordFormValues,
  currentValues: BackRecordFormValues,
): BackRecordFormValues {
  return {
    ...initialValues,
    ...currentValues,
    finishAdjustments: { ...initialValues.finishAdjustments, ...currentValues.finishAdjustments },
    finishes: { ...initialValues.finishes, ...currentValues.finishes },
    onSiteOutputs: { ...initialValues.onSiteOutputs, ...currentValues.onSiteOutputs },
    rolls: { ...initialValues.rolls, ...currentValues.rolls },
    steps: { ...initialValues.steps, ...currentValues.steps },
    trims: { ...initialValues.trims, ...currentValues.trims },
  }
}
