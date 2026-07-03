import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Button, Popconfirm, Tag, message } from 'antd'
import { DownloadOutlined, PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import {
  pageWarehouses,
  deleteWarehouse,
} from '../../api/warehouse'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import TooltipText from '../../components/biz/TooltipText'
import { mesTablePagination } from '../../components/biz/mesPaginationUtils'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import { PERMISSIONS } from '../../constants/permissions'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { useHasPermission } from '../../stores/authStore'
import type { Warehouse, WarehouseQuery } from '../../types/warehouse'
import { datedCsvFilename, exportRowsToCsv } from '../../utils/exportCsv'

/** 状态字典：1 启用 / 2 停用。 */
const STATUS: Record<number, string> = { 1: '启用', 2: '停用' }

export default function WarehouseList() {
  const actionRef = useRef<ActionType>(null)
  const latestQueryRef = useRef<WarehouseQuery>({})
  const navigate = useNavigate()
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)
  const columnsState = useTableColumnsState('table-columns-warehouses')
  const [exporting, setExporting] = useState(false)

  const handleDelete = async (record: Warehouse) => {
    await deleteWarehouse(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const handleExport = async () => {
    setExporting(true)
    try {
      const res = await pageWarehouses({ ...latestQueryRef.current, current: 1, size: 10000 })
      const result = exportRowsToCsv({
        columns: warehouseExportColumns(),
        filename: datedCsvFilename('仓库档案'),
        rows: res.records ?? [],
      })
      message.success(`已导出 ${result.filename}`)
    } finally {
      setExporting(false)
    }
  }

  const columns: ProColumns<Warehouse>[] = [
    { title: '仓库编码', dataIndex: 'warehouseCode', width: 140 },
    { title: '仓库名称', dataIndex: 'warehouseName', width: 200, render: textCell },
    { title: '库位', dataIndex: 'location', width: 200, search: false },
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
          <Button type="link" size="small" onClick={() => navigate(`/warehouses/${record.uuid}`)}>
            详情
          </Button>
          {canManageBase && (
            <Button type="link" size="small" onClick={() => navigate(`/warehouses/${record.uuid}/edit`)}>
              编辑
            </Button>
          )}
          {canManageBase && (
            <Popconfirm
              title="确认删除该仓库？"
              description="删除后新单不能选择该仓库，已产生的出入库记录不受影响。"
              onConfirm={() => handleDelete(record)}
            >
              <Button danger type="link" size="small">删除</Button>
            </Popconfirm>
          )}
        </div>
      ),
    },
  ]
  const resizable = useResizableTableColumns<Warehouse, ProColumns<Warehouse>>(columns, 'warehouses')

  return (
    <ProTable<Warehouse>
      className="mes-pro-table-page"
      rowKey="uuid"
      actionRef={actionRef}
      columns={resizable.columns}
      columnsState={columnsState}
      components={resizable.components}
      headerTitle="仓库档案"
      toolBarRender={() => [
        <Button key="export" icon={<DownloadOutlined />} loading={exporting} onClick={handleExport}>
          导出
        </Button>,
        canManageBase && (
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => navigate('/warehouses/create')}>
            新增仓库
          </Button>
        ),
      ].filter(Boolean)}
      request={async (params) => {
        const query = {
          current: params.current,
          size: params.pageSize,
          keyword: params.warehouseCode || params.warehouseName,
          status: params.status,
        }
        latestQueryRef.current = query
        const res = await pageWarehouses(query)
        return { data: res.records ?? [], total: res.total ?? 0, success: true }
      }}
      bordered
      pagination={mesTablePagination(10)}
      search={{ labelWidth: 'auto' }}
      scroll={{ x: resizable.scrollX, y: '100%' }}
      tableLayout="fixed"
      options={mesProTableOptions()}
    />
  )
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}

function warehouseExportColumns() {
  return [
    { header: '仓库编码', value: (row: Warehouse) => row.warehouseCode },
    { header: '仓库名称', value: (row: Warehouse) => row.warehouseName },
    { header: '库位', value: (row: Warehouse) => row.location },
    { header: '状态', value: (row: Warehouse) => row.status ? STATUS[row.status] ?? '-' : '-' },
    { header: '备注', value: (row: Warehouse) => row.remark },
    { header: '创建时间', value: (row: Warehouse) => row.createTime },
  ]
}
