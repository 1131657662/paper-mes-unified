import { useRef } from 'react'
import type { ReactNode } from 'react'
import { Button, Popconfirm, Tag, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import {
  pageWarehouses,
  deleteWarehouse,
} from '../../api/warehouse'
import TooltipText from '../../components/biz/TooltipText'
import { mesPageSizeOptions, mesPaginationShowTotal } from '../../components/biz/MesPaginationBar'
import { MES_PRO_TABLE_SCROLL } from '../../components/biz/tableScroll'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import type { Warehouse } from '../../types/warehouse'

/** 状态字典：1 启用 / 2 停用。 */
const STATUS: Record<number, string> = { 1: '启用', 2: '停用' }

export default function WarehouseList() {
  const actionRef = useRef<ActionType>(null)
  const navigate = useNavigate()
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)

  const handleDelete = async (record: Warehouse) => {
    await deleteWarehouse(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const columns: ProColumns<Warehouse>[] = [
    { title: '仓库编码', dataIndex: 'warehouseCode', width: 140 },
    { title: '仓库名称', dataIndex: 'warehouseName', render: textCell },
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

  return (
    <ProTable<Warehouse>
      className="mes-pro-table-page"
      rowKey="uuid"
      actionRef={actionRef}
      columns={columns}
      headerTitle="仓库档案"
      toolBarRender={() => canManageBase ? [
        <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => navigate('/warehouses/create')}>
          新增仓库
        </Button>,
      ] : []}
      request={async (params) => {
        const res = await pageWarehouses({
          current: params.current,
          size: params.pageSize,
          keyword: params.warehouseCode || params.warehouseName,
          status: params.status,
        })
        return { data: res.records ?? [], total: res.total ?? 0, success: true }
      }}
      bordered
      pagination={{
        defaultPageSize: 10,
        showSizeChanger: true,
        pageSizeOptions: mesPageSizeOptions,
        showTotal: mesPaginationShowTotal,
      }}
      search={{ labelWidth: 'auto' }}
      scroll={MES_PRO_TABLE_SCROLL}
    />
  )
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}
