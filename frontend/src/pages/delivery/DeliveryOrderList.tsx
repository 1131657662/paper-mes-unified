import { useState } from 'react'
import { Card, Form, Input, Modal, Segmented, message } from 'antd'
import dayjs from 'dayjs'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import AvailableFinishTable from '../../features/delivery/components/AvailableFinishTable'
import DeliveryMetricStrip from '../../features/delivery/components/DeliveryMetricStrip'
import DeliveryOrderTable from '../../features/delivery/components/DeliveryOrderTable'
import DeliveryWorkbenchHeader, {
  type DeliveryHeaderForm,
} from '../../features/delivery/components/DeliveryWorkbenchHeader'
import { useAvailableFinishes } from '../../features/delivery/hooks/useAvailableFinishes'
import { useConfirmDelivery } from '../../features/delivery/hooks/useConfirmDelivery'
import { useCreateDelivery } from '../../features/delivery/hooks/useCreateDelivery'
import { useDeliveryOrders } from '../../features/delivery/hooks/useDeliveryOrders'
import type { DeliveryOrder } from '../../types/delivery'
import DeliveryDetailDrawer from './DeliveryDetailDrawer'
import './DeliveryOrderList.css'

type QueueFilter = 'all' | 'pending' | 'done'

const pageSize = 20

export default function DeliveryOrderList() {
  const [form] = Form.useForm<DeliveryHeaderForm>()
  const [queueFilter, setQueueFilter] = useState<QueueFilter>('all')
  const [page, setPage] = useState(1)
  const [selectedCustomerUuid, setSelectedCustomerUuid] = useState<string>()
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [detailUuid, setDetailUuid] = useState<string | null>(null)
  const customersQuery = useCustomers()
  const finishesQuery = useAvailableFinishes(selectedCustomerUuid)
  const ordersQuery = useDeliveryOrders({
    current: page,
    deliveryStatus: deliveryStatus(queueFilter),
    size: pageSize,
  })
  const createMutation = useCreateDelivery()
  const confirmMutation = useConfirmDelivery()

  const stock = finishesQuery.data ?? []
  const orders = ordersQuery.data?.records ?? []
  const selectedFinishes = stock.filter((item) => selectedRowKeys.includes(item.finishUuid))

  const reload = () => {
    finishesQuery.refetch()
    ordersQuery.refetch()
  }

  const handleCustomerChange = (uuid?: string) => {
    setSelectedCustomerUuid(uuid)
    setSelectedRowKeys([])
  }

  const handleCreate = async () => {
    const values = await form.validateFields()
    if (selectedFinishes.length === 0) {
      message.warning('请先勾选本次要出库的成品卷')
      return
    }
    const risky = selectedFinishes.some((item) => item.settlementRisk)
    if (risky) {
      await confirmCashRelease()
    }
    await createMutation.mutateAsync({
      carNo: values.carNo,
      containerNo: values.containerNo,
      customerUuid: values.customerUuid!,
      deliveryDate: values.deliveryDate!.format('YYYY-MM-DD'),
      forceRelease: risky,
      items: selectedFinishes.map((item) => ({ finishUuid: item.finishUuid })),
      pickerName: values.pickerName,
      remark: values.remark,
    })
    message.success('待出库单已生成')
    setSelectedRowKeys([])
  }

  const handleConfirm = async (record: DeliveryOrder) => {
    const signUser = await askSignUser(record)
    await confirmMutation.mutateAsync({
      uuid: record.uuid,
      data: signUser ? { signUser } : undefined,
    })
    message.success('出库签收完成')
  }

  return (
    <Card className="delivery-workbench mes-fill-card mes-workbench" title="出库管理">
      <Form form={form} initialValues={{ deliveryDate: dayjs() }} layout="vertical">
        <DeliveryWorkbenchHeader
          customers={customersQuery.data?.records ?? []}
          loadingCustomers={customersQuery.isLoading}
          selectedCustomerUuid={selectedCustomerUuid}
          onCustomerChange={handleCustomerChange}
          onReload={reload}
          onSubmit={handleCreate}
          submitting={createMutation.isPending}
        />
      </Form>

      <DeliveryMetricStrip orders={orders} selectedFinishes={selectedFinishes} stock={stock} />

      <div className="delivery-workbench__body">
        <section className="delivery-panel delivery-panel--stock">
          <div className="delivery-panel__head">
            <div>
              <h3>可出库库存</h3>
              <p>已回录入库且未被待出库单占用的成品卷。</p>
            </div>
          </div>
          <AvailableFinishTable
            data={stock}
            loading={finishesQuery.isLoading || finishesQuery.isFetching}
            selectedRowKeys={selectedRowKeys}
            onSelectionChange={setSelectedRowKeys}
          />
        </section>

        <section className="delivery-panel delivery-panel--orders">
          <div className="delivery-panel__head">
            <div>
              <h3>出库单据</h3>
              <p>待出库单确认签收后，成品库存状态变为已出库。</p>
            </div>
            <Segmented
              value={queueFilter}
              options={[
                { label: '全部', value: 'all' },
                { label: '待签收', value: 'pending' },
                { label: '已出库', value: 'done' },
              ]}
              onChange={(value) => {
                setQueueFilter(value as QueueFilter)
                setPage(1)
              }}
            />
          </div>
          <DeliveryOrderTable
            data={orders}
            loading={ordersQuery.isLoading || ordersQuery.isFetching}
            page={page}
            pageSize={pageSize}
            total={ordersQuery.data?.total ?? 0}
            onConfirm={handleConfirm}
            onDetail={(record) => setDetailUuid(record.uuid)}
            onPageChange={(nextPage) => setPage(nextPage)}
          />
        </section>
      </div>

      <DeliveryDetailDrawer
        uuid={detailUuid}
        open={!!detailUuid}
        onClose={() => setDetailUuid(null)}
      />
    </Card>
  )
}

function deliveryStatus(filter: QueueFilter) {
  if (filter === 'pending') return 1
  if (filter === 'done') return 2
  return undefined
}

function confirmCashRelease() {
  return new Promise<void>((resolve, reject) => {
    Modal.confirm({
      title: '次结出库确认',
      content: '本次选择包含次结且有待收款风险的加工单。确认后将按“警告放行”生成出库单。',
      okText: '警告放行',
      cancelText: '取消',
      onOk: () => resolve(),
      onCancel: () => reject(new Error('cancel')),
    })
  })
}

function askSignUser(record: DeliveryOrder) {
  let signUser = ''
  return new Promise<string>((resolve, reject) => {
    Modal.confirm({
      title: `确认签收 ${record.deliveryNo}`,
      content: (
        <div className="delivery-sign-modal">
          <p>确认后将扣减所选成品库存，状态变为已出库。</p>
          <Input
            placeholder="签收人姓名（可选）"
            onChange={(event) => {
              signUser = event.target.value
            }}
          />
        </div>
      ),
      okText: '确认签收',
      cancelText: '取消',
      onOk: () => resolve(signUser.trim()),
      onCancel: () => reject(new Error('cancel')),
    })
  })
}
