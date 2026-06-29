import { useState } from 'react'
import { Card, Form, Segmented, message } from 'antd'
import dayjs from 'dayjs'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import SettleCandidateTable from '../../features/settle/components/SettleCandidateTable'
import SettleMetricStrip from '../../features/settle/components/SettleMetricStrip'
import SettleOrderTable from '../../features/settle/components/SettleOrderTable'
import SettleWorkbenchHeader, {
  type SettleWorkbenchForm,
} from '../../features/settle/components/SettleWorkbenchHeader'
import { useCreateSettleByOrders } from '../../features/settle/hooks/useCreateSettleByOrders'
import { useSettleCandidates } from '../../features/settle/hooks/useSettleCandidates'
import { useSettleOrders } from '../../features/settle/hooks/useSettleOrders'
import type { SettleCandidateQuery, SettleOrder } from '../../types/settle'
import ReceiveModal from './ReceiveModal'
import SettleDetailDrawer from './SettleDetailDrawer'
import './SettleOrderList.css'

type QueueFilter = 'all' | 'pending' | 'partial' | 'paid'

const pageSize = 20

export default function SettleOrderList() {
  const [form] = Form.useForm<SettleWorkbenchForm>()
  const [candidateQuery, setCandidateQuery] = useState<SettleCandidateQuery>({})
  const [queueFilter, setQueueFilter] = useState<QueueFilter>('all')
  const [page, setPage] = useState(1)
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [detailUuid, setDetailUuid] = useState<string | null>(null)
  const [receiveRecord, setReceiveRecord] = useState<SettleOrder | null>(null)

  const customersQuery = useCustomers()
  const candidatesQuery = useSettleCandidates(candidateQuery)
  const ordersQuery = useSettleOrders({
    current: page,
    settleStatus: settleStatus(queueFilter),
    size: pageSize,
  })
  const createMutation = useCreateSettleByOrders()

  const candidates = candidatesQuery.data ?? []
  const orders = ordersQuery.data?.records ?? []
  const selectedCandidates = candidates.filter((item) => selectedRowKeys.includes(item.orderUuid))

  const reload = () => {
    candidatesQuery.refetch()
    ordersQuery.refetch()
  }

  const handleCustomerChange = (uuid?: string) => {
    const values = form.getFieldsValue()
    setSelectedRowKeys([])
    setCandidateQuery(buildCandidateQuery({ ...values, customerUuid: uuid }))
  }

  const handleCreate = async () => {
    const values = await form.validateFields()
    if (selectedCandidates.length === 0) {
      message.warning('请先勾选需要结算的加工单')
      return
    }
    const period = values.period
    await createMutation.mutateAsync({
      isInvoice: values.isInvoice,
      orderUuids: selectedCandidates.map((item) => item.orderUuid),
      periodEnd: period?.[1]?.format('YYYY-MM-DD'),
      periodStart: period?.[0]?.format('YYYY-MM-DD'),
      remark: values.remark,
      settleDate: values.settleDate?.format('YYYY-MM-DD'),
    })
    message.success('结算单已生成')
    setSelectedRowKeys([])
  }

  return (
    <Card className="settle-workbench mes-fill-card mes-workbench" title="结算管理">
      <Form
        form={form}
        initialValues={{ isInvoice: 2, settleDate: dayjs() }}
        layout="vertical"
        onValuesChange={(_, values) => {
          setCandidateQuery(buildCandidateQuery(values))
          setSelectedRowKeys([])
        }}
      >
        <SettleWorkbenchHeader
          customers={customersQuery.data?.records ?? []}
          loadingCustomers={customersQuery.isLoading}
          selectedCount={selectedCandidates.length}
          submitting={createMutation.isPending}
          onCreate={handleCreate}
          onCustomerChange={handleCustomerChange}
          onReload={reload}
        />
      </Form>

      <SettleMetricStrip orders={orders} selectedCandidates={selectedCandidates} />

      <div className="settle-workbench__body">
        <section className="settle-panel settle-panel--candidates">
          <div className="settle-panel__head">
            <div>
              <h3>可结算加工单</h3>
              <p>仅显示已完成且尚未结算的加工单；本阶段按整单入账，原卷粒度会在下一阶段升级。</p>
            </div>
          </div>
          <SettleCandidateTable
            data={candidates}
            loading={candidatesQuery.isLoading || candidatesQuery.isFetching}
            selectedRowKeys={selectedRowKeys}
            onSelectionChange={setSelectedRowKeys}
          />
        </section>

        <section className="settle-panel settle-panel--orders">
          <div className="settle-panel__head">
            <div>
              <h3>结算单据</h3>
              <p>查看应收、已收、未收与收款进度，未结清单据可继续登记收款。</p>
            </div>
            <Segmented
              value={queueFilter}
              options={[
                { label: '全部', value: 'all' },
                { label: '待收', value: 'pending' },
                { label: '部分', value: 'partial' },
                { label: '结清', value: 'paid' },
              ]}
              onChange={(value) => {
                setQueueFilter(value as QueueFilter)
                setPage(1)
              }}
            />
          </div>
          <SettleOrderTable
            data={orders}
            loading={ordersQuery.isLoading || ordersQuery.isFetching}
            page={page}
            pageSize={pageSize}
            total={ordersQuery.data?.total ?? 0}
            onDetail={(record) => setDetailUuid(record.uuid)}
            onPageChange={(nextPage) => setPage(nextPage)}
            onReceive={(record) => setReceiveRecord(record)}
          />
        </section>
      </div>

      <SettleDetailDrawer
        uuid={detailUuid}
        open={!!detailUuid}
        onClose={() => setDetailUuid(null)}
      />
      <ReceiveModal
        settleUuid={receiveRecord?.uuid ?? null}
        unreceivedAmount={receiveRecord?.unreceivedAmount ?? 0}
        open={!!receiveRecord}
        onClose={() => setReceiveRecord(null)}
        onSuccess={() => {
          setReceiveRecord(null)
          reload()
        }}
      />
    </Card>
  )
}

function buildCandidateQuery(values: SettleWorkbenchForm): SettleCandidateQuery {
  return {
    customerUuid: values.customerUuid,
    periodEnd: values.period?.[1]?.format('YYYY-MM-DD'),
    periodStart: values.period?.[0]?.format('YYYY-MM-DD'),
  }
}

function settleStatus(filter: QueueFilter) {
  if (filter === 'pending') return 1
  if (filter === 'partial') return 2
  if (filter === 'paid') return 3
  return undefined
}
