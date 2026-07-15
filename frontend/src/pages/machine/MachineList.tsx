import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Button, Popconfirm, Tag, message } from 'antd'
import { DownloadOutlined, PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import { pageMachines, deleteMachine } from '../../api/machine'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderCompatibleTableOptions } from '../../components/biz/tableToolbarOptionsRender'
import TooltipText from '../../components/biz/TooltipText'
import { mesTablePagination } from '../../components/biz/mesPaginationUtils'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import { PERMISSIONS } from '../../constants/permissions'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { useHasPermission } from '../../stores/authStore'
import type { Machine, MachineQuery } from '../../types/machine'
import { datedCsvFilename, exportRowsToCsv } from '../../utils/exportCsv'

/** 机台类型字典：1 锯纸 / 2 复卷 / 3 通用。 */
const MACHINE_TYPE: Record<number, string> = { 1: '锯纸', 2: '复卷', 3: '通用' }
/** 状态字典：1 启用 / 2 停用。 */
const STATUS: Record<number, string> = { 1: '启用', 2: '停用' }

export default function MachineList() {
  const actionRef = useRef<ActionType>(null)
  const latestQueryRef = useRef<MachineQuery>({})
  const navigate = useNavigate()
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)
  const columnsState = useTableColumnsState('table-columns-machines')
  const [exporting, setExporting] = useState(false)

  const handleDelete = async (record: Machine) => {
    await deleteMachine(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const handleExport = async () => {
    setExporting(true)
    try {
      const res = await pageMachines({ ...latestQueryRef.current, current: 1, size: 10000 })
      const result = exportRowsToCsv({
        columns: machineExportColumns(),
        filename: datedCsvFilename('机台档案'),
        rows: res.records ?? [],
      })
      message.success(`已导出 ${result.filename}`)
    } finally {
      setExporting(false)
    }
  }

  const columns: ProColumns<Machine>[] = [
    { title: '机台编码', dataIndex: 'machineCode', width: 140 },
    { title: '机台名称', dataIndex: 'machineName', width: 200, render: textCell },
    {
      title: '类型',
      dataIndex: 'machineType',
      width: 100,
      search: false,
      render: (_, r) => (
        <Tag className="mes-data-tag" color={r.machineType ? 'blue' : 'default'}>
          {r.machineType ? MACHINE_TYPE[r.machineType] ?? '-' : '-'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      valueType: 'select',
      valueEnum: {
        1: { text: '启用' },
        2: { text: '停用' },
      },
      render: (_, r) => (
        <Tag className="mes-data-tag" color={r.status === 1 ? 'green' : 'default'}>
          {r.status ? STATUS[r.status] ?? '-' : '-'}
        </Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'createTime', width: 180, search: false, valueType: 'dateTime' },
    {
      title: '操作',
      key: 'actions',
      valueType: 'option',
      width: 140,
      render: (_, record) => (
        <div className="mes-table-actions">
          <Button type="link" size="small" onClick={() => navigate(`/machines/${record.uuid}`)}>
            详情
          </Button>
          {canManageBase && (
            <Button type="link" size="small" onClick={() => navigate(`/machines/${record.uuid}/edit`)}>
              编辑
            </Button>
          )}
          {canManageBase && (
            <Popconfirm
              title="确认删除该机台？"
              description="删除后排产/回录不能选择该机台，已产生的历史记录不受影响。"
              onConfirm={() => handleDelete(record)}
            >
              <Button danger type="link" size="small">删除</Button>
            </Popconfirm>
          )}
        </div>
      ),
    },
  ]
  const resizable = useResizableTableColumns<Machine, ProColumns<Machine>>(columns, 'machines')

  return (
    <ProTable<Machine>
      className="mes-pro-table-page"
      rowKey="uuid"
      actionRef={actionRef}
      columns={resizable.columns}
      columnsState={columnsState}
      components={resizable.components}
      headerTitle="机台档案"
      toolBarRender={() => [
        <Button key="export" icon={<DownloadOutlined />} loading={exporting} onClick={handleExport}>
          导出
        </Button>,
        canManageBase && (
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => navigate('/machines/create')}>
            新增机台
          </Button>
        ),
      ].filter(Boolean)}
      request={async (params) => {
        const query = {
          current: params.current,
          size: params.pageSize,
          keyword: params.machineCode || params.machineName,
          status: params.status,
        }
        latestQueryRef.current = query
        const res = await pageMachines(query)
        return { data: res.records ?? [], total: res.total ?? 0, success: true }
      }}
      bordered
      pagination={mesTablePagination(10)}
      search={{ labelWidth: 'auto' }}
      scroll={{ x: resizable.scrollX, y: '100%' }}
      tableLayout="fixed"
      options={mesProTableOptions()}
      optionsRender={renderCompatibleTableOptions}
    />
  )
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}

function machineExportColumns() {
  return [
    { header: '机台编码', value: (row: Machine) => row.machineCode },
    { header: '机台名称', value: (row: Machine) => row.machineName },
    { header: '类型', value: (row: Machine) => row.machineType ? MACHINE_TYPE[row.machineType] ?? '-' : '-' },
    { header: '状态', value: (row: Machine) => row.status ? STATUS[row.status] ?? '-' : '-' },
    { header: '备注', value: (row: Machine) => row.remark },
    { header: '创建时间', value: (row: Machine) => row.createTime },
  ]
}
