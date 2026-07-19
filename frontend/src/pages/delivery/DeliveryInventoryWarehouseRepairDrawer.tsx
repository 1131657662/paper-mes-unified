import { ExclamationCircleOutlined, ToolOutlined } from '@ant-design/icons'
import { Alert, Button, Drawer, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd'
import type { TableColumnsType } from 'antd'
import { useState } from 'react'
import { useAssignDeliveryInventoryWarehouse } from '../../features/delivery/hooks/useAssignDeliveryInventoryWarehouse'
import { useDeliveryInventoryUnassigned } from '../../features/delivery/hooks/useDeliveryInventoryUnassigned'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryInventoryUnassignedOrder } from '../../types/deliveryInventory'
import type { Warehouse } from '../../types/warehouse'
import './DeliveryInventoryWarehouseRepairDrawer.css'

interface Props {
  open: boolean
  warehouses: Warehouse[]
  onClose: () => void
}

interface RepairForm {
  warehouseUuid: string
  reason: string
}

export default function DeliveryInventoryWarehouseRepairDrawer({ open, warehouses, onClose }: Props) {
  const [form] = Form.useForm<RepairForm>()
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState<string>()
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([])
  const query = useDeliveryInventoryUnassigned({ current: page, size: 10, keyword }, open)
  const repair = useAssignDeliveryInventoryWarehouse()
  const rows = query.data?.records ?? []
  const selectedRows = rows.filter((row) => selectedKeys.includes(row.orderUuid))
  const requiredWarehouses = [...new Set(selectedRows.map((row) => row.knownWarehouseUuid).filter(Boolean))]
  const enabledWarehouses = warehouses.filter((item) => item.status === 1)

  const submit = async () => {
    if (!selectedRows.length) return void message.warning('请先选择需要补仓的加工单')
    const values = await form.validateFields()
    if (!warehouseMatches(requiredWarehouses, values.warehouseUuid)) {
      return void message.error('所选加工单已有仓库归属，请选择与现有库存一致的仓库')
    }
    confirmRepair({ values, selectedRows, repair, onSuccess: () => closeAndReset(form, setSelectedKeys, onClose) })
  }

  return (
    <Drawer className="inventory-warehouse-repair" width={820} open={open} onClose={onClose}
      maskClosable={!repair.isPending} title={<Space><ToolOutlined /><span>未分仓库存治理</span></Space>}
      footer={<RepairFooter count={selectedRows.length} loading={repair.isPending} onCancel={onClose} onSubmit={() => void submit()} />}>
      <Alert showIcon type="warning" message="仅处理历史空仓数据"
        description="系统不会自动套用默认仓库。请根据实物位置确认仓库；提交后将同步加工单、成品库存和完成快照，并保留操作日志。" />
      <Input.Search allowClear className="inventory-warehouse-repair__search" placeholder="搜索加工单号或客户"
        onSearch={(value) => { setKeyword(value.trim() || undefined); setPage(1); setSelectedKeys([]) }} />
      {query.isError ? <Alert showIcon type="error" message="未分仓数据加载失败" action={<Button size="small" onClick={() => void query.refetch()}>重新加载</Button>} /> : null}
      <Table<DeliveryInventoryUnassignedOrder> rowKey="orderUuid" size="small" bordered
        columns={columns} dataSource={rows} loading={query.isLoading || query.isFetching}
        rowSelection={{ selectedRowKeys: selectedKeys, onChange: setSelectedKeys, getCheckboxProps: rowCheckboxProps }}
        pagination={{ current: page, pageSize: 10, total: query.data?.total ?? 0, showSizeChanger: false,
          onChange: (next) => { setPage(next); setSelectedKeys([]) } }}
        scroll={{ x: 700 }} />
      <Form<RepairForm> form={form} layout="vertical" className="inventory-warehouse-repair__form">
        <Form.Item name="warehouseUuid" label="补录仓库" extra={warehouseHint(requiredWarehouses, warehouses)}
          rules={[{ required: true, message: '请选择补录仓库' }]}>
          <Select showSearch optionFilterProp="label" placeholder="选择实物所在仓库"
            options={enabledWarehouses.map((item) => ({ label: item.warehouseName, value: item.uuid }))} />
        </Form.Item>
        <Form.Item name="reason" label="补录原因" rules={[{ required: true, min: 4, max: 200, message: '请输入4至200个字符的补录原因' }]}>
          <Input.TextArea rows={3} maxLength={200} showCount placeholder="例如：历史回录未记录仓库，经现场盘点确认" />
        </Form.Item>
      </Form>
    </Drawer>
  )
}

const columns: TableColumnsType<DeliveryInventoryUnassignedOrder> = [
  { title: '加工单号', dataIndex: 'orderNo', fixed: 'left', width: 140 },
  { title: '客户', dataIndex: 'customerName', width: 130, ellipsis: true },
  { title: '未分仓', dataIndex: 'unassignedRollCount', align: 'right', width: 80, render: (value) => `${value} 卷` },
  { title: '剩余重量', dataIndex: 'unassignedWeight', align: 'right', width: 100, render: (value) => formatTon(value) },
  { title: '已知仓库', dataIndex: 'knownWarehouseName', width: 120, render: (value) => value || <Typography.Text type="secondary">待人工确认</Typography.Text> },
  { title: '处理条件', key: 'state', width: 100, render: (_, row) => repairState(row) },
]

function rowCheckboxProps(row: DeliveryInventoryUnassignedOrder) {
  const disabled = row.warehouseConflict || row.activeLockCount > 0
  return { disabled, title: disabled ? repairStateText(row) : undefined }
}

function repairState(row: DeliveryInventoryUnassignedOrder) {
  if (row.warehouseConflict) return <Tag color="error">仓库冲突</Tag>
  if (row.activeLockCount > 0) return <Tag color="warning">出库占用</Tag>
  return <Tag color="success">可补录</Tag>
}

function repairStateText(row: DeliveryInventoryUnassignedOrder) {
  return row.warehouseConflict ? '同一加工单存在多个已知仓库，请先核对数据' : '存在活动出库占用，请先处理出库单'
}

function warehouseMatches(required: (string | undefined)[], selected: string) {
  return required.length <= 1 && (!required[0] || required[0] === selected)
}

function warehouseHint(required: (string | undefined)[], warehouses: Warehouse[]) {
  if (required.length !== 1 || !required[0]) return '必须按现场实物位置选择，不会自动采用默认仓库'
  const warehouse = warehouses.find((item) => item.uuid === required[0])
  return `所选加工单已有部分库存归属 ${warehouse?.warehouseName ?? '指定仓库'}，本次必须保持一致`
}

function RepairFooter(props: { count: number; loading: boolean; onCancel: () => void; onSubmit: () => void }) {
  return <div className="inventory-warehouse-repair__footer"><Typography.Text type="secondary">已选择 {props.count} 个加工单</Typography.Text><Space><Button onClick={props.onCancel}>取消</Button><Button type="primary" icon={<ToolOutlined />} loading={props.loading} disabled={!props.count} onClick={props.onSubmit}>确认补仓</Button></Space></div>
}

function confirmRepair(options: { values: RepairForm; selectedRows: DeliveryInventoryUnassignedOrder[]; repair: ReturnType<typeof useAssignDeliveryInventoryWarehouse>; onSuccess: () => void }) {
  Modal.confirm({ title: '确认补录历史库存仓库？', icon: <ExclamationCircleOutlined />,
    content: `将处理 ${options.selectedRows.length} 个加工单、${options.selectedRows.reduce((sum, row) => sum + row.unassignedRollCount, 0)} 卷成品。此操作会写入审计日志。`,
    okText: '确认补仓', cancelText: '返回核对', onOk: async () => { await options.repair.mutateAsync({ orderUuids: options.selectedRows.map((row) => row.orderUuid), ...options.values }); options.onSuccess() } })
}

function closeAndReset(form: ReturnType<typeof Form.useForm<RepairForm>>[0], setKeys: (keys: React.Key[]) => void, onClose: () => void) {
  form.resetFields(); setKeys([]); onClose()
}
