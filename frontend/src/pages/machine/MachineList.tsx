import { useRef, useState } from 'react'
import { Button, message } from 'antd'
import { DownloadOutlined, PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import { pageMachines, deleteMachine } from '../../api/machine'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderCompatibleTableOptions } from '../../components/biz/tableToolbarOptionsRender'
import { mesTablePagination } from '../../components/biz/mesPaginationUtils'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import { PERMISSIONS } from '../../constants/permissions'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { useHasPermission } from '../../stores/authStore'
import type { Machine, MachineQuery } from '../../types/machine'
import { datedCsvFilename, exportRowsToCsv } from '../../utils/exportCsv'
import { MACHINE_STATUS_LABEL, RESOURCE_KIND_LABEL } from './machineArchiveLabels'
import { machineListColumns } from './machineListColumns'

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
        filename: datedCsvFilename('机台与工位档案'),
        rows: res.records ?? [],
      })
      message.success(`已导出 ${result.filename}`)
    } finally {
      setExporting(false)
    }
  }

  const columns = machineListColumns({ canManage: canManageBase, navigate, onDelete: handleDelete })
  const resizable = useResizableTableColumns<Machine, ProColumns<Machine>>(columns, 'machines')

  return (
    <ProTable<Machine>
      className="mes-pro-table-page"
      rowKey="uuid"
      actionRef={actionRef}
      columns={resizable.columns}
      columnsState={columnsState}
      components={resizable.components}
      headerTitle="机台与工位档案"
      toolBarRender={() => [
        <Button key="export" icon={<DownloadOutlined />} loading={exporting} onClick={handleExport}>
          导出
        </Button>,
        canManageBase && (
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => navigate('/machines/create')}>
            新增资源
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

function machineExportColumns() {
  return [
    { header: '资源编码', value: (row: Machine) => row.machineCode },
    { header: '资源名称', value: (row: Machine) => row.machineName },
    { header: '资源类型', value: (row: Machine) => RESOURCE_KIND_LABEL[row.resourceKind ?? 'MACHINE'] },
    { header: '工艺能力', value: (row: Machine) => row.capabilities?.map((item) => `${item.processName}${item.defaultCapability ? '(默认)' : ''}`).join('、') },
    { header: '状态', value: (row: Machine) => MACHINE_STATUS_LABEL[row.status ?? 0] ?? '-' },
    { header: '备注', value: (row: Machine) => row.remark },
    { header: '创建时间', value: (row: Machine) => row.createTime },
  ]
}
