import { Button, Popconfirm, Tag } from 'antd'
import type { ProColumns } from '@ant-design/pro-components'
import type { NavigateFunction } from 'react-router-dom'
import TooltipText from '../../components/biz/TooltipText'
import type { Machine } from '../../types/machine'
import { MACHINE_STATUS_LABEL, RESOURCE_KIND_LABEL } from './machineArchiveLabels'

interface Options {
  canManage: boolean
  navigate: NavigateFunction
  onDelete: (machine: Machine) => Promise<void>
}

export function machineListColumns(options: Options): ProColumns<Machine>[] {
  return [
    { title: '资源编码', dataIndex: 'machineCode', width: 140 },
    { title: '资源名称', dataIndex: 'machineName', width: 180, render: (value) => <TooltipText value={value} /> },
    {
      title: '资源类型', dataIndex: 'resourceKind', width: 100, search: false,
      render: (_, row) => <Tag>{RESOURCE_KIND_LABEL[row.resourceKind ?? 'MACHINE']}</Tag>,
    },
    {
      title: '工艺能力', dataIndex: 'capabilities', width: 310, search: false,
      render: (_, row) => renderCapabilityTags(row),
    },
    {
      title: '状态', dataIndex: 'status', width: 100, valueType: 'select',
      valueEnum: { 1: { text: '启用' }, 2: { text: '停用' } },
      render: (_, row) => <Tag color={row.status === 1 ? 'green' : 'default'}>
        {MACHINE_STATUS_LABEL[row.status ?? 0] ?? '-'}
      </Tag>,
    },
    { title: '创建时间', dataIndex: 'createTime', width: 180, search: false, valueType: 'dateTime' },
    {
      title: '操作', key: 'actions', valueType: 'option', width: 140,
      render: (_, row) => renderMachineActions(row, options),
    },
  ]
}

function renderCapabilityTags(machine: Machine) {
  if (!machine.capabilities?.length) return <span>-</span>
  return (
    <div className="machine-capability-tags">
      {machine.capabilities.map((item) => (
        <Tag color={item.defaultCapability ? 'blue' : undefined} key={item.catalogUuid}>
          {item.processName}{item.defaultCapability ? ' · 默认' : ''}
        </Tag>
      ))}
    </div>
  )
}

function renderMachineActions(machine: Machine, options: Options) {
  return (
    <div className="mes-table-actions">
      <Button type="link" size="small" onClick={() => options.navigate(`/machines/${machine.uuid}`)}>详情</Button>
      {options.canManage && <Button type="link" size="small"
        onClick={() => options.navigate(`/machines/${machine.uuid}/edit`)}>编辑</Button>}
      {options.canManage && (
        <Popconfirm title="确认删除该生产资源？"
          description="删除后加工单不能再选择，历史记录和机台快照不受影响。"
          onConfirm={() => options.onDelete(machine)}>
          <Button danger type="link" size="small">删除</Button>
        </Popconfirm>
      )}
    </div>
  )
}
