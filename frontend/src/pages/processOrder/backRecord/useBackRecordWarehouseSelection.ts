import { useEffect, useRef } from 'react'
import { Form, type FormInstance } from 'antd'
import { useWarehouses } from '../../../features/processOrderCreate/hooks/useReferenceData'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'

interface Params {
  detail?: ProcessOrderDetailVO
  enabled: boolean
  form: FormInstance<BackRecordFormValues>
}

export function useBackRecordWarehouseSelection({ detail, enabled, form }: Params) {
  const query = useWarehouses()
  const selectedUuid = Form.useWatch('warehouseUuid', form)
  const initializedOrderRef = useRef<string | null>(null)
  const warehouses = (query.data?.records ?? []).filter((warehouse) => warehouse.status === 1)
  const savedUuid = warehouses.some((warehouse) => warehouse.uuid === detail?.order.warehouseUuid)
    ? detail?.order.warehouseUuid
    : undefined
  const suggestedUuid = savedUuid ?? warehouses.find((warehouse) => warehouse.isDefault === 1)?.uuid

  useEffect(() => {
    if (!enabled) {
      initializedOrderRef.current = null
      return
    }
    const orderUuid = detail?.order.uuid
    if (!orderUuid || !query.isSuccess || initializedOrderRef.current === orderUuid) return
    form.setFieldValue('warehouseUuid', suggestedUuid)
    initializedOrderRef.current = orderUuid
  }, [detail?.order.uuid, enabled, form, query.isSuccess, suggestedUuid])

  return {
    error: query.isError,
    loading: query.isLoading,
    selectedName: warehouses.find((warehouse) => warehouse.uuid === selectedUuid)?.warehouseName,
    warehouses,
  }
}
