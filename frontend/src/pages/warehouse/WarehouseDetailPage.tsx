import { Button, Card, Descriptions, Empty, Skeleton, Space, Tag } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getWarehouse } from '../../api/warehouse'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import type { Warehouse } from '../../types/warehouse'
import '../documentModule.css'
import './WarehouseProfile.css'

const STATUS: Record<number, string> = { 1: '启用', 2: '停用' }

export default function WarehouseDetailPage() {
  const { uuid } = useParams()
  const navigate = useNavigate()
  const [warehouse, setWarehouse] = useState<Warehouse>()
  const [loading, setLoading] = useState(true)
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)

  useEffect(() => {
    if (!uuid) return
    setLoading(true)
    getWarehouse(uuid)
      .then(setWarehouse)
      .finally(() => setLoading(false))
  }, [uuid])

  if (loading) {
    return (
      <div className="document-module-page warehouse-profile-page">
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    )
  }

  if (!warehouse) {
    return (
      <div className="document-module-page warehouse-profile-page">
        <Empty description="仓库档案不存在" />
      </div>
    )
  }

  return (
    <div className="document-module-page warehouse-profile-page">
      <MesPageHeader
        title={warehouse.warehouseName}
        eyebrow="仓库档案"
        description={`仓库编码：${text(warehouse.warehouseCode)} · 库位：${text(warehouse.location)} · 状态：${statusText(warehouse.status)}`}
        onBack={() => navigate('/warehouses')}
        tags={<Tag color={warehouse.status === 1 ? 'green' : 'default'}>{statusText(warehouse.status)}</Tag>}
        actions={canManageBase ? (
          <Space>
            <Button icon={<EditOutlined />} type="primary" onClick={() => navigate(`/warehouses/${warehouse.uuid}/edit`)}>
              编辑仓库
            </Button>
          </Space>
        ) : undefined}
      />

      <section className="warehouse-detail-overview">
        <MetricCard label="库位/地址" value={warehouse.location || '-'} helper="用于出库和库内识别" />
        <MetricCard label="当前状态" value={statusText(warehouse.status)} helper="停用仓库不建议选用" />
        <MetricCard label="仓库编码" value={warehouse.warehouseCode || '-'} helper="内部仓库编号" />
        <MetricCard label="最近更新" value={dateText(warehouse.updateTime)} helper="档案维护时间" />
      </section>

      <div className="warehouse-detail-grid">
        <Card className="document-module-card" title="基础资料">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="仓库编码">{text(warehouse.warehouseCode)}</Descriptions.Item>
            <Descriptions.Item label="仓库名称">{text(warehouse.warehouseName)}</Descriptions.Item>
            <Descriptions.Item label="库位/地址">{text(warehouse.location)}</Descriptions.Item>
            <Descriptions.Item label="状态">{statusText(warehouse.status)}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card className="document-module-card" title="维护信息">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="创建时间">{dateText(warehouse.createTime)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{dateText(warehouse.updateTime)}</Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>{text(warehouse.remark)}</Descriptions.Item>
          </Descriptions>
        </Card>
      </div>
    </div>
  )
}

function MetricCard({ helper, label, value }: { helper: string; label: string; value: string }) {
  return (
    <div className="warehouse-detail-metric">
      <span>{label}</span>
      <strong>{value}</strong>
      <em>{helper}</em>
    </div>
  )
}

function statusText(value?: number) {
  return value ? STATUS[value] ?? '-' : '-'
}

function dateText(value?: string) {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}

function text(value?: string) {
  return value || '-'
}
