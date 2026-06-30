import { Button, Card, Form, Skeleton, Space, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createWarehouse, getWarehouse, updateWarehouse } from '../../api/warehouse'
import MesPageHeader from '../../components/layout/MesPageHeader'
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
  const { uuid } = useParams()
  const [loading, setLoading] = useState(mode === 'edit')
  const [submitting, setSubmitting] = useState(false)
  const isEdit = mode === 'edit'

  useEffect(() => {
    if (!isEdit || !uuid) return
    setLoading(true)
    getWarehouse(uuid)
      .then((data) => form.setFieldsValue(toFormValues(data)))
      .finally(() => setLoading(false))
  }, [form, isEdit, uuid])

  const submit = async (values: WarehouseSaveDTO) => {
    setSubmitting(true)
    try {
      const savedUuid = isEdit && uuid ? uuid : await createWarehouse(values)
      if (isEdit && uuid) await updateWarehouse(uuid, values)
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
        description="维护加工、成品和出库相关仓库资料，出库和库存视图会按这里的仓库名称与状态识别。"
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
        {loading ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : (
          <WarehouseProfileForm editing={isEdit} form={form} onFinish={submit} />
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
    warehouseCode: warehouse.warehouseCode,
    warehouseName: warehouse.warehouseName,
  }
}
