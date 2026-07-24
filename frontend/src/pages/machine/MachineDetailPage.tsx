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
import MachineCapabilityTable from './MachineCapabilityTable'
import { MACHINE_STATUS_LABEL, RESOURCE_KIND_LABEL } from './machineArchiveLabels'
import './MachineProfile.css'

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
        <Empty description="机台或工位档案不存在" />
      </div>
    )
  }

  return (
    <div className="document-module-page machine-profile-page">
      <MesPageHeader
        title={machine.machineName}
        eyebrow="机台与工位档案"
        description={`资源编码：${text(machine.machineCode)} · 类型：${resourceKindText(machine)} · 状态：${statusText(machine.status)}`}
        onBack={() => navigate('/machines')}
        tags={<Tag color={machine.status === 1 ? 'green' : 'default'}>{statusText(machine.status)}</Tag>}
        actions={canManageBase ? (
          <Space>
            <Button icon={<EditOutlined />} type="primary" onClick={() => navigate(`/machines/${machine.uuid}/edit`)}>
              编辑资源
            </Button>
          </Space>
        ) : undefined}
      />

      <section className="machine-detail-overview">
        <MetricCard label="资源类型" value={resourceKindText(machine)} helper="设备或生产工位" />
        <MetricCard label="工艺能力" value={`${machine.capabilities?.length ?? 0} 项`} helper={capabilityNames(machine)} />
        <MetricCard label="默认工艺" value={`${defaultCount(machine)} 项`} helper="自动推荐的加工资源" />
        <MetricCard label="当前状态" value={statusText(machine.status)} helper="停用机台不建议排产" />
      </section>

      <div className="machine-detail-grid">
        <Card className="document-module-card" title="基础资料">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="资源编码">{text(machine.machineCode)}</Descriptions.Item>
            <Descriptions.Item label="资源名称">{text(machine.machineName)}</Descriptions.Item>
            <Descriptions.Item label="资源类型">{resourceKindText(machine)}</Descriptions.Item>
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
      <Card className="document-module-card" title="工艺能力与默认规则">
        <MachineCapabilityTable capabilities={machine.capabilities} />
      </Card>
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

function statusText(value?: number) {
  return MACHINE_STATUS_LABEL[value ?? 0] ?? '-'
}

function resourceKindText(machine: Machine) {
  return RESOURCE_KIND_LABEL[machine.resourceKind ?? 'MACHINE']
}

function defaultCount(machine: Machine) {
  return machine.capabilities?.filter((item) => item.defaultCapability).length ?? 0
}

function capabilityNames(machine: Machine) {
  return machine.capabilities?.map((item) => item.processName).join(' / ') || '尚未配置'
}

function dateText(value?: string) {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}

function text(value?: string) {
  return value || '-'
}
