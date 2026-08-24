import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { Button, Card, Descriptions, Drawer, Form, Input, Space, Tabs, Typography } from 'antd'
import { useState } from 'react'
import { useRemainAdjustments } from '../../features/remain/hooks/useRemainAdjustments'
import { useRemainInventory } from '../../features/remain/hooks/useRemainInventory'
import { useRemainRefunds } from '../../features/remain/hooks/useRemainRefunds'
import { useRemainRegistrations } from '../../features/remain/hooks/useRemainRegistrations'
import type { RemainAdjustment, RemainInventory, RemainRefund, RemainRegistration, RemainSale } from '../../types/remain'
import { RemainAdjustmentActionModal, type RemainAdjustmentAction } from './RemainAdjustmentActionModal'
import { RemainAdjustmentTable } from './RemainAdjustmentTable'
import { RemainInventoryTable } from './RemainInventoryTable'
import { RemainPriceModal } from './RemainPriceModal'
import { RemainRefundActionModal, type RemainRefundAction } from './RemainRefundActionModal'
import { RemainRefundTable } from './RemainRefundTable'
import { RemainRegistrationModal } from './RemainRegistrationModal'
import { RemainRegistrationTable } from './RemainRegistrationTable'
import { RemainRollbackModal } from './RemainRollbackModal'
import { RemainSaleModal } from './RemainSaleModal'
import { RemainSaleReverseModal } from './RemainSaleReverseModal'
import { RemainSaleTable } from './RemainSaleTable'
import { useRemainSales } from '../../features/remain/hooks/useRemainSales'
import { formatAmount, formatWeight, StatusTag } from './remainDisplay'
import './RemainPage.css'

export default function RemainPage() {
  const [filters, setFilters] = useState({ orderUuid: '', customerUuid: '' })
  const [registrationModal, setRegistrationModal] = useState(false)
  const [selectedRegistration, setSelectedRegistration] = useState<RemainRegistration>()
  const [priceRow, setPriceRow] = useState<RemainRegistration>()
  const [rollbackRow, setRollbackRow] = useState<RemainRegistration>()
  const [adjustmentRow, setAdjustmentRow] = useState<RemainAdjustment>()
  const [adjustmentAction, setAdjustmentAction] = useState<RemainAdjustmentAction>()
  const [refundRow, setRefundRow] = useState<RemainRefund>()
  const [refundAction, setRefundAction] = useState<RemainRefundAction>()
  const [selectedInventory, setSelectedInventory] = useState<RemainInventory[]>([])
  const [saleRows, setSaleRows] = useState<RemainInventory[]>([])
  const [saleRow, setSaleRow] = useState<RemainSale>()
  const registrations = useRemainRegistrations(filters)
  const inventory = useRemainInventory({ availableOnly: true })
  const adjustments = useRemainAdjustments()
  const refunds = useRemainRefunds()
  const sales = useRemainSales()

  const closeAdjustment = () => { setAdjustmentRow(undefined); setAdjustmentAction(undefined) }
  const closeRefund = () => { setRefundRow(undefined); setRefundAction(undefined) }
  const closeRegistration = () => setRegistrationModal(false)
  const tabs = [
    { key: 'registrations', label: '余料登记', children: <RemainRegistrationTable rows={registrations.data ?? []} loading={registrations.isLoading} onDetail={setSelectedRegistration} onPrice={setPriceRow} onRollback={setRollbackRow} /> },
    { key: 'inventory', label: '我方库存', children: <div><Space style={{ marginBottom: 12 }}><Button type="primary" disabled={selectedInventory.length === 0} onClick={() => setSaleRows(selectedInventory)}>出售或处理</Button><Typography.Text type="secondary">已选择 {selectedInventory.length} 个库存批次</Typography.Text></Space><RemainInventoryTable rows={inventory.data ?? []} loading={inventory.isLoading} selectedRowKeys={selectedInventory.map((row) => row.lotUuid)} onSelectionChange={setSelectedInventory} /></div> },
    { key: 'adjustments', label: '待调整', children: <RemainAdjustmentTable rows={adjustments.data ?? []} loading={adjustments.isLoading} onNextSettlement={(row) => { setAdjustmentRow(row); setAdjustmentAction('next-settlement') }} onCredit={(row) => { setAdjustmentRow(row); setAdjustmentAction('credit') }} onRefund={(row) => { setAdjustmentRow(row); setAdjustmentAction('refund') }} onCancel={(row) => { setAdjustmentRow(row); setAdjustmentAction('cancel') }} onReverseCredit={(row) => { setAdjustmentRow(row); setAdjustmentAction('reverse-credit') }} /> },
    { key: 'refunds', label: '退款', children: <RemainRefundTable rows={refunds.data ?? []} loading={refunds.isLoading} onApprove={(row) => { setRefundRow(row); setRefundAction('approve') }} onPay={(row) => { setRefundRow(row); setRefundAction('pay') }} onCancel={(row) => { setRefundRow(row); setRefundAction('cancel') }} /> },
    { key: 'sales', label: '出售处理', children: <RemainSaleTable rows={sales.data ?? []} loading={sales.isLoading} onReverse={setSaleRow} /> },
  ]

  return (
    <Card title="余料抵扣与我方余料处理" extra={<Space><Button icon={<ReloadOutlined />} onClick={() => { void registrations.refetch(); void inventory.refetch(); void adjustments.refetch(); void refunds.refetch(); void sales.refetch() }}>刷新</Button><Button type="primary" icon={<PlusOutlined />} onClick={() => setRegistrationModal(true)}>新建登记</Button></Space>}>
      <Form layout="inline" onFinish={(values) => setFilters(values)} initialValues={filters} style={{ marginBottom: 16 }}>
        <Form.Item name="orderUuid" label="加工单"><Input allowClear placeholder="加工单 UUID" /></Form.Item>
        <Form.Item name="customerUuid" label="客户"><Input allowClear placeholder="客户 UUID" /></Form.Item>
        <Button type="primary" htmlType="submit">查询</Button>
      </Form>
      <Tabs items={tabs} />
      <RemainRegistrationModal open={registrationModal} onClose={closeRegistration} />
      <RemainPriceModal row={priceRow} onClose={() => setPriceRow(undefined)} />
      <RemainRollbackModal row={rollbackRow} onClose={() => setRollbackRow(undefined)} />
      <RemainAdjustmentActionModal row={adjustmentRow} action={adjustmentAction} onClose={closeAdjustment} />
      <RemainRefundActionModal row={refundRow} action={refundAction} onClose={closeRefund} />
      <RemainSaleModal rows={saleRows} onClose={() => { setSaleRows([]); setSelectedInventory([]) }} />
      <RemainSaleReverseModal row={saleRow} onClose={() => setSaleRow(undefined)} />
      <RemainRegistrationDrawer row={selectedRegistration} onClose={() => setSelectedRegistration(undefined)} />
    </Card>
  )
}

function RemainRegistrationDrawer({ row, onClose }: { row?: RemainRegistration; onClose: () => void }) {
  return <Drawer title={row ? `登记单 ${row.registrationNo}` : '登记单详情'} open={Boolean(row)} onClose={onClose} width={560}>
    {row && <>
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="加工单">{row.orderUuid}</Descriptions.Item>
        <Descriptions.Item label="客户">{row.customerUuid}</Descriptions.Item>
        <Descriptions.Item label="状态"><StatusTag value={row.status} /></Descriptions.Item>
        <Descriptions.Item label="价格"><StatusTag value={row.priceStatus} /></Descriptions.Item>
        <Descriptions.Item label="转入重量">{formatWeight(row.totalTransferredWeight)}</Descriptions.Item>
        <Descriptions.Item label="已处理重量">{formatWeight(row.totalProcessedWeight)}</Descriptions.Item>
        <Descriptions.Item label="金额">{formatAmount(row.totalAmount)}</Descriptions.Item>
      </Descriptions>
      <Typography.Title level={5}>来源明细</Typography.Title>
      {(row.lines ?? []).map((line) => <div key={line.uuid ?? line.sourceFinishRollUuid} className="remain-detail-line"><Typography.Text>{line.sourceFinishRollUuid}</Typography.Text><Typography.Text>{formatWeight(line.currentOwnWeight)}</Typography.Text></div>)}
    </>}
  </Drawer>
}
