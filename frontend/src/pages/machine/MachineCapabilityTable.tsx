import { Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { MachineCapability } from '../../types/machine'
import { capabilityRangeText } from './machineCapabilityModel'
import { PROCESS_CATEGORY_LABEL } from './machineArchiveLabels'

interface Props { capabilities?: MachineCapability[] }

export default function MachineCapabilityTable({ capabilities = [] }: Props) {
  if (!capabilities.length) return <Empty description="暂无工艺能力" />
  return (
    <Table<MachineCapability>
      bordered className="mes-table-card" columns={columns} dataSource={capabilities}
      pagination={false} rowKey="catalogUuid" scroll={{ x: 840 }} size="small"
    />
  )
}

const columns: ColumnsType<MachineCapability> = [
  { title: '工艺', dataIndex: 'processName', width: 140, render: (value) => <Typography.Text strong>{value}</Typography.Text> },
  { title: '类别', dataIndex: 'processCategory', width: 90, render: (value) => <Tag>{PROCESS_CATEGORY_LABEL[value] ?? value}</Tag> },
  { title: '默认资源', dataIndex: 'defaultCapability', width: 100, render: (value) => value ? <Tag color="blue">默认</Tag> : '-' },
  { title: '候选顺序', dataIndex: 'priority', width: 100, align: 'right' },
  { title: '加工范围', width: 280, render: (_, row) => capabilityRangeText(row) },
  { title: '备注', dataIndex: 'remark', render: (value) => value || '-' },
]
