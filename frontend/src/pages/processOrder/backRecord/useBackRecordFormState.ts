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
  const displayValues = useBackRecordDisplayValues(form, filledValues)
  const initialize = useCallback((nextDetail: ProcessOrderDetailVO) => {
    const initialValues = buildInitialValues(nextDetail)
    form.setFieldsValue(initialValues)
    setFilledValues(initialValues)
    initializedOrderRef.current = nextDetail.order.uuid
  }, [form])

  useEffect(() => {
    const detailUuid = detail?.order.uuid
    if (enabled && detail && initializedOrderRef.current !== detailUuid) {
      initialize(detail)
    }
    if (!enabled) {
      initializedOrderRef.current = null
      form.resetFields()
      setFilledValues({})
    }
  }, [detail, enabled, form, initialize])

  return {
    displayValues,
    form,
    initialize,
    resetInitialization: () => { initializedOrderRef.current = null },
    syncFilledValues: setFilledValues,
  }
}

function buildInitialValues(detail: ProcessOrderDetailVO): BackRecordFormValues {
  const initialValues = initialBackRecordValues(detail)
  const workbench = buildBackRecordWorkbench(detail)
  for (const item of workbench.items) {
    if (!item.roll) continue
    for (const entry of item.finishes) {
      if (entry.bindMode !== 'inferred') continue
      const current = initialValues.finishes?.[entry.finish.uuid]
      if (current) current.originalUuid = item.roll.uuid
    }
  }
  initialValues.onSiteOutputs = buildInitialOnSiteOutputGroups(detail, workbench.items)
  return initialValues
}
