import { useRef } from 'react'
import type { ReactNode } from 'react'
import { Button, Popconfirm, Tag, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import {
  pageCustomers,
  deleteCustomer,
} from '../../api/customer'
import TooltipText from '../../components/biz/TooltipText'
import { mesTablePagination } from '../../components/biz/mesPaginationUtils'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import { PERMISSIONS } from '../../constants/permissions'
import { DICT_TYPES, invoiceFallbackOptions, settleFallbackOptions } from '../../features/systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../features/systemConfig/hooks/useRuntimeDictOptions'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { useHasPermission } from '../../stores/authStore'
import type { Customer } from '../../types/customer'

const PRICE_TAX_TYPE: Record<number, string> = { 1: '含税价', 2: '未税价' }

export default function CustomerList() {
  const actionRef = useRef<ActionType>(null)
  const navigate = useNavigate()
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)
  const columnsState = useTableColumnsState('table-columns-customers')
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const { options: settleOptions } = useNumberDictOptions(DICT_TYPES.settleType, settleFallbackOptions)

  const handleDelete = async (record: Customer) => {
    await deleteCustomer(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const columns: ProColumns<Customer>[] = [
    { title: '客户编码', dataIndex: 'customerCode', width: 140 },
    { title: '客户名称', dataIndex: 'customerName', width: 200, render: textCell },
    { title: '联系人', dataIndex: 'contact', width: 120, search: false },
    { title: '电话', dataIndex: 'phone', width: 140, search: false },
    {
      title: '结算方式',
      dataIndex: 'settleType',
      width: 100,
      search: false,
      render: (_, r) => (
        <Tag className="mes-data-tag" color={r.settleType === 2 ? 'blue' : 'default'}>
          {settleText(r, settleOptions)}
        </Tag>
      ),
    },
    {
      title: '默认单价',
      dataIndex: 'price',
      width: 160,
      search: false,
      render: (_, r) => priceCell(r),
    },
    {
      title: '默认开票',
      dataIndex: 'defaultInvoice',
      width: 120,
      search: false,
      render: (_, r) => (
        <Tag className="mes-data-tag" color={r.defaultInvoice === 1 ? 'green' : 'default'}>
          {optionLabel(invoiceOptions, r.defaultInvoice)}
        </Tag>
      ),
    },
    {
      title: '税率',
      dataIndex: 'taxRate',
      width: 90,
      search: false,
      render: (_, r) => (r.defaultInvoice === 1 ? `${r.taxRate ?? 0}%` : '-'),
    },
    { title: '送货地址', dataIndex: 'deliveryAddress', width: 220, search: false, render: textCell },
    { title: '创建时间', dataIndex: 'createTime', width: 180, search: false, valueType: 'dateTime' },
    {
      title: '操作',
      key: 'actions',
      valueType: 'option',
      width: 140,
      render: (_, record) => (
        <div className="mes-table-actions">
          <Button type="link" size="small" onClick={() => navigate(`/customers/${record.uuid}`)}>
            详情
          </Button>
          {canManageBase && (
            <Button type="link" size="small" onClick={() => navigate(`/customers/${record.uuid}/edit`)}>
              编辑
            </Button>
          )}
          {canManageBase && (
            <Popconfirm
              title="确认删除该客户？"
              description="删除后新建加工单不能再选择该客户，历史单据不受影响。"
              onConfirm={() => handleDelete(record)}
            >
              <Button danger type="link" size="small">删除</Button>
            </Popconfirm>
          )}
        </div>
      ),
    },
  ]
  const resizable = useResizableTableColumns<Customer, ProColumns<Customer>>(columns, 'customers')

  return (
    <ProTable<Customer>
      className="mes-pro-table-page"
      rowKey="uuid"
      actionRef={actionRef}
      columns={resizable.columns}
      columnsState={columnsState}
      components={resizable.components}
      headerTitle="客户管理"
      toolBarRender={() => canManageBase ? [
        <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => navigate('/customers/create')}>
          新增客户
        </Button>,
      ] : []}
      request={async (params) => {
        const res = await pageCustomers({
          current: params.current,
          size: params.pageSize,
          keyword: params.customerCode || params.customerName,
        })
        return { data: res.records ?? [], total: res.total ?? 0, success: true }
      }}
      bordered
      pagination={mesTablePagination(10)}
      search={{ labelWidth: 'auto' }}
      scroll={{ x: resizable.scrollX, y: '100%' }}
    />
  )
}

function settleText(customer: Customer, options?: Array<{ label: string; value: number | string }>) {
  if (customer.settleType === 2) {
    const text = optionLabel(options, customer.settleType)
    return customer.settleDay ? `${text} ${customer.settleDay}日` : text
  }
  if (customer.settleType === 1) return optionLabel(options, customer.settleType)
  return '-'
}

function optionLabel(options: Array<{ label: string; value: number | string }> | undefined, value?: number) {
  if (value == null) return '-'
  return options?.find((item) => item.value === value)?.label ?? '-'
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}

function priceCell(customer: Customer) {
  const saw = customer.sawPrice == null ? '-' : `锯 ¥${customer.sawPrice}/刀`
  const rewind = customer.rewindPrice == null ? '-' : `复 ¥${customer.rewindPrice}/吨`
  const taxType = customer.priceIncludeTax ? PRICE_TAX_TYPE[customer.priceIncludeTax] ?? '-' : '-'
  return (
    <div className="customer-price-cell">
      <span>{saw}</span>
      <span>{rewind}</span>
      <em>{taxType}</em>
    </div>
  )
}
