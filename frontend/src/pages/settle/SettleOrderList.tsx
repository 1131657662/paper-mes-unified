import { useState } from 'react'
import { Button, DatePicker, Form, Input, Select, message } from 'antd'
import { ReloadOutlined, SearchOutlined, WalletOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import { useNavigate } from 'react-router-dom'
import DocumentListShell from '../../components/biz/DocumentListShell'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import { useDocumentRowSelection } from '../../components/biz/useDocumentRowSelection'
import { SETTLE_STATUS, SETTLE_TYPE } from '../../constants/settle'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import SettleOrderTable from '../../features/settle/components/SettleOrderTable'
import { useSettleOrders } from '../../features/settle/hooks/useSettleOrders'
import { useConfiguredPageSize } from '../../features/systemConfig/hooks/useConfiguredPageSize'
import type { SettleOrder, SettleQuery } from '../../types/settle'
import ReceiveModal from './ReceiveModal'
import './SettleOrderList.css'

type QueueFilter = 'all' | 'pending' | 'partial' | 'paid'

interface SearchFormValues {
  customerUuid?: string
  dateRange?: [Dayjs, Dayjs] | null
  keyword?: string
  settleStatus?: number
  settleType?: number
}

export default function SettleOrderList() {
  const [form] = Form.useForm<SearchFormValues>()
  const navigate = useNavigate()
  const customersQuery = useCustomers()
  const [queueFilter, setQueueFilter] = useState<QueueFilter>('all')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useConfiguredPageSize(20)
  const [filters, setFilters] = useState<SettleQuery>({})
  const [receiveRecord, setReceiveRecord] = useState<SettleOrder | null>(null)
  const rowSelection = useDocumentRowSelection<SettleOrder>()
  const query = { ...filters, current: page, settleStatus: settleStatus(queueFilter, filters), size: pageSize }
  const ordersQuery = useSettleOrders(query)
  const orders = ordersQuery.data?.records ?? []
  const selectedReceivable = rowSelection.selectedRows.filter((record) => record.settleStatus !== 3)

  const handleSearch = (values: SearchFormValues) => {
    setFilters({
      customerUuid: values.customerUuid,
      dateFrom: values.dateRange?.[0]?.format('YYYY-MM-DD'),
      dateTo: values.dateRange?.[1]?.format('YYYY-MM-DD'),
      keyword: values.keyword?.trim() || undefined,
      settleStatus: values.settleStatus,
      settleType: values.settleType,
    })
    setPage(1)
    rowSelection.clear()
  }

  const handleReset = () => {
    form.resetFields()
    setFilters({})
    setPage(1)
    rowSelection.clear()
  }

  const handleReceiveSelected = () => {
    if (selectedReceivable.length !== 1) {
      message.warning('请选择一张未结清结算单登记收款')
      return
    }
    setReceiveRecord(selectedReceivable[0])
  }

  const handlePageChange = (nextPage: number, nextPageSize: number) => {
    setPage(nextPage)
    setPageSize(nextPageSize)
    rowSelection.clear()
  }

  return (
    <DocumentListShell
      title="结算管理"
      createText="新建结算单"
      queue={queueFilter}
      queueOptions={[
        { label: '全部', value: 'all' },
        { label: '待收款', value: 'pending' },
        { label: '部分收款', value: 'partial' },
        { label: '已结清', value: 'paid' },
      ]}
      search={(
        <SettleSearchBar
          form={form}
          customers={customersQuery.data?.records ?? []}
          loadingCustomers={customersQuery.isLoading}
          onReset={handleReset}
          onSearch={handleSearch}
        />
      )}
      leftActions={(
        <Button
          icon={<WalletOutlined />}
          disabled={selectedReceivable.length !== 1}
          onClick={handleReceiveSelected}
        >
          登记收款
        </Button>
      )}
      onCreate={() => navigate('/settle-orders/create')}
      onQueueChange={(value) => {
        setQueueFilter(value)
        setPage(1)
        rowSelection.clear()
      }}
    >
      <div className="document-page-table">
        <SettleOrderTable
          data={orders}
          loading={ordersQuery.isLoading || ordersQuery.isFetching}
          onReload={() => ordersQuery.refetch()}
          rowClassName={rowSelection.rowClassName}
          rowSelection={rowSelection.rowSelection}
          onDetail={(record) => navigate(`/settle-orders/${record.uuid}`)}
          onRow={rowSelection.onRow}
          onReceive={(record) => setReceiveRecord(record)}
        />
      </div>
      <DocumentPaginationBar
        current={page}
        pageSize={pageSize}
        total={ordersQuery.data?.total ?? 0}
        onChange={handlePageChange}
      />

      <ReceiveModal
        settleUuid={receiveRecord?.uuid ?? null}
        unreceivedAmount={receiveRecord?.unreceivedAmount ?? 0}
        open={!!receiveRecord}
        onClose={() => setReceiveRecord(null)}
        onSuccess={() => {
          setReceiveRecord(null)
          rowSelection.clear()
          ordersQuery.refetch()
        }}
      />
    </DocumentListShell>
  )
}

function SettleSearchBar({
  customers,
  form,
  loadingCustomers,
  onReset,
  onSearch,
}: {
  customers: { customerName: string; uuid: string }[]
  form: ReturnType<typeof Form.useForm<SearchFormValues>>[0]
  loadingCustomers: boolean
  onReset: () => void
  onSearch: (values: SearchFormValues) => void
}) {
  return (
    <Form form={form} layout="vertical" className="document-searchbar" onFinish={onSearch}>
      <div className="document-searchbar__grid document-searchbar__grid--six">
        <Form.Item name="keyword" label="结算单号/客户">
          <Input allowClear placeholder="输入单号、客户或备注" />
        </Form.Item>
        <Form.Item name="customerUuid" label="客户">
          <Select
            allowClear
            showSearch
            loading={loadingCustomers}
            placeholder="全部客户"
            options={customers.map((item) => ({ label: item.customerName, value: item.uuid }))}
            optionFilterProp="label"
          />
        </Form.Item>
        <Form.Item name="dateRange" label="结算日期">
          <DatePicker.RangePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="settleStatus" label="状态">
          <Select allowClear placeholder="全部状态" options={statusOptions()} />
        </Form.Item>
        <Form.Item name="settleType" label="类型">
          <Select allowClear placeholder="全部类型" options={typeOptions()} />
        </Form.Item>
        <div className="document-searchbar__actions">
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
          <Button icon={<ReloadOutlined />} onClick={onReset}>重置</Button>
        </div>
      </div>
    </Form>
  )
}

function settleStatus(filter: QueueFilter, filters: SettleQuery) {
  if (filters.settleStatus) return filters.settleStatus
  if (filter === 'pending') return 1
  if (filter === 'partial') return 2
  if (filter === 'paid') return 3
  return undefined
}

function statusOptions() {
  return Object.entries(SETTLE_STATUS).map(([value, item]) => ({
    label: item.text,
    value: Number(value),
  }))
}

function typeOptions() {
  return Object.entries(SETTLE_TYPE).map(([value, label]) => ({
    label,
    value: Number(value),
  }))
}
