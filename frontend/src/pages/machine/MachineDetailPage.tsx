import { Button, Card, Descriptions, Empty, Skeleton, Space, Tag } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getMachine } from '../../api/machine'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import type { Machine } from '../../types/machine'
import '../documentModule.css'
import './MachineProfile.css'

const MACHINE_TYPE: Record<number, string> = { 1: '锯纸', 2: '复卷', 3: '通用' }
const STATUS: Record<number, string> = { 1: '启用', 2: '停用' }

export default function MachineDetailPage() {
  const { uuid } = useParams()
  const navigate = useNavigate()
  const [machine, setMachine] = useState<Machine>()
  const [loading, setLoading] = useState(true)
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)

  useEffect(() => {
    if (!uuid) return
    setLoading(true)
    getMachine(uuid)
      .then(setMachine)
      .finally(() => setLoading(false))
  }, [uuid])

  if (loading) {
    return (
      <div className="document-module-page machine-profile-page">
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    )
  }

  if (!machine) {
    return (
      <div className="document-module-page machine-profile-page">
        <Empty description="机台档案不存在" />
      </div>
    )
  }

  return (
    <div className="document-module-page machine-profile-page">
      <MesPageHeader
        title={machine.machineName}
        eyebrow="机台档案"
        description={`机台编码：${text(machine.machineCode)} · 类型：${machineTypeText(machine.machineType)} · 状态：${statusText(machine.status)}`}
        onBack={() => navigate('/machines')}
        tags={<Tag color={machine.status === 1 ? 'green' : 'default'}>{statusText(machine.status)}</Tag>}
        actions={canManageBase ? (
          <Space>
            <Button icon={<EditOutlined />} type="primary" onClick={() => navigate(`/machines/${machine.uuid}/edit`)}>
              编辑机台
            </Button>
          </Space>
        ) : undefined}
      />

      <section className="machine-detail-overview">
        <MetricCard label="机台类型" value={machineTypeText(machine.machineType)} helper="用于加工工艺匹配" />
        <MetricCard label="当前状态" value={statusText(machine.status)} helper="停用机台不建议排产" />
        <MetricCard label="机台编码" value={machine.machineCode || '-'} helper="内部设备编号" />
        <MetricCard label="最近更新" value={dateText(machine.updateTime)} helper="档案维护时间" />
      </section>

      <div className="machine-detail-grid">
        <Card className="document-module-card" title="基础资料">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="机台编码">{text(machine.machineCode)}</Descriptions.Item>
            <Descriptions.Item label="机台名称">{text(machine.machineName)}</Descriptions.Item>
            <Descriptions.Item label="机台类型">{machineTypeText(machine.machineType)}</Descriptions.Item>
            <Descriptions.Item label="状态">{statusText(machine.status)}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card className="document-module-card" title="维护信息">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="创建时间">{dateText(machine.createTime)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{dateText(machine.updateTime)}</Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>{text(machine.remark)}</Descriptions.Item>
          </Descriptions>
        </Card>
      </div>
    </div>
  )
}

function MetricCard({ helper, label, value }: { helper: string; label: string; value: string }) {
  return (
    <div className="machine-detail-metric">
      <span>{label}</span>
      <strong>{value}</strong>
      <em>{helper}</em>
    </div>
  )
}

function machineTypeText(value?: number) {
  return value ? MACHINE_TYPE[value] ?? '-' : '-'
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
