import { Button, Card, Empty, Form, Skeleton, Space, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { createWarehouse, updateWarehouse } from '../../api/warehouse'
import { isNotFoundError } from '../../api/request'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useWarehouseDetail } from '../../features/warehouse/hooks/useWarehouseDetail'
import { warehouseKeys } from '../../features/warehouse/queries/warehouseKeys'
import { useUnsavedChangesGuard } from '../../hooks/useUnsavedChangesGuard'
import type { Warehouse, WarehouseSaveDTO } from '../../types/warehouse'
import '../documentModule.css'
import WarehouseProfileForm from './WarehouseProfileForm'
import './WarehouseProfile.css'

interface Props {
  mode: 'create' | 'edit'
}

export default function WarehouseFormPage({ mode }: Props) {
  const [form] = Form.useForm<WarehouseSaveDTO>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { uuid } = useParams()
  const [submitting, setSubmitting] = useState(false)
  const isEdit = mode === 'edit'
  const { clearDirty, markDirty } = useUnsavedChangesGuard()
  const {
    data: warehouse,
    error: warehouseError,
    isError: isWarehouseError,
    isPending: isLoadingWarehouse,
    refetch: refetchWarehouse,
  } = useWarehouseDetail(isEdit ? uuid : undefined)

  useEffect(() => {
    if (warehouse) form.setFieldsValue(toFormValues(warehouse))
  }, [form, warehouse])

  const submit = async (values: WarehouseSaveDTO) => {
    setSubmitting(true)
    try {
      const savedUuid = isEdit && uuid ? uuid : await createWarehouse(values)
      if (isEdit && uuid) await updateWarehouse(uuid, values)
      if (isEdit && uuid) {
        await queryClient.invalidateQueries({ queryKey: warehouseKeys.detail(uuid).queryKey })
      }
      clearDirty()
      message.success(isEdit ? '仓库档案已保存' : '仓库档案已新增')
      navigate(`/warehouses/${savedUuid}`)
    } finally {
      setSubmitting(false)
    }
  }

  const backPath = isEdit && uuid ? `/warehouses/${uuid}` : '/warehouses'

  return (
    <div className="document-module-page warehouse-profile-page">
      <MesPageHeader
        title={isEdit ? '编辑仓库档案' : '新增仓库档案'}
        onBack={() => navigate(backPath)}
        actions={(
          <Space>
            <Button onClick={() => navigate(backPath)}>取消</Button>
            <Button type="primary" loading={submitting} onClick={() => form.submit()}>
              保存仓库
            </Button>
          </Space>
        )}
      />

      <Card className="document-module-card warehouse-profile-card" title="仓库资料">
        {isLoadingWarehouse ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : isWarehouseError && !isNotFoundError(warehouseError) ? (
          <QueryLoadErrorAlert
            message="仓库档案加载失败"
            description="请检查网络或服务状态后重新加载。"
            onRetry={() => { void refetchWarehouse() }}
          />
        ) : isWarehouseError || (isEdit && !warehouse) ? (
          <Empty description="仓库档案不存在" />
        ) : (
          <WarehouseProfileForm editing={isEdit} form={form} onFinish={submit} onValuesChange={markDirty} />
        )}
      </Card>
    </div>
  )
}

function toFormValues(warehouse: Warehouse): WarehouseSaveDTO {
  return {
    location: warehouse.location,
    remark: warehouse.remark,
    status: warehouse.status,
    isDefault: warehouse.isDefault,
    warehouseCode: warehouse.warehouseCode,
    warehouseName: warehouse.warehouseName,
  }
}
