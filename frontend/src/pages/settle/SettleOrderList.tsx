import { useState } from 'react'
import { Button, Form, message } from 'antd'
import { WalletOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import DocumentListShell from '../../components/biz/DocumentListShell'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useDocumentRowSelection } from '../../components/biz/useDocumentRowSelection'
import { PERMISSIONS } from '../../constants/permissions'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import SettleOrderTable from '../../features/settle/components/SettleOrderTable'
import { useSettleOrders } from '../../features/settle/hooks/useSettleOrders'
import { useConfiguredPageSize } from '../../features/systemConfig/hooks/useConfiguredPageSize'
import { useHasPermission } from '../../stores/authStore'
import type { SettleOrder, SettleQuery } from '../../types/settle'
import ReceiveModal from './ReceiveModal'
import SettleSearchBar, { type SettleSearchFormValues } from './SettleSearchBar'
import './SettleOrderList.css'

type QueueFilter = 'all' | 'pending' | 'partial' | 'paid'

export default function SettleOrderList() {
  const [form] = Form.useForm<SettleSearchFormValues>()
  const navigate = useNavigate()
  const customersQuery = useCustomers()
  const [queueFilter, setQueueFilter] = useState<QueueFilter>('all')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useConfiguredPageSize(20)
  const [filters, setFilters] = useState<SettleQuery>({})
  const [receiveRecord, setReceiveRecord] = useState<SettleOrder | null>(null)
  const canManageSettle = useHasPermission(PERMISSIONS.settleManage)
  const canReceiveSettle = useHasPermission(PERMISSIONS.settleReceive)
  const rowSelection = useDocumentRowSelection<SettleOrder>()
  const query = { ...filters, current: page, settleStatus: settleStatus(queueFilter, filters), size: pageSize }
  const ordersQuery = useSettleOrders(query)
  const orders = ordersQuery.data?.records ?? []
  const tableDensity = tableDensityMode(orders.length, pageSize, ordersQuery.isLoading)
  const selectedReceivable = rowSelection.selectedRows.filter((record) => record.settleStatus !== 3)

  const handleSearch = (values: SettleSearchFormValues) => {
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
    if (!canReceiveSettle) return
    if (selectedReceivable.length !== 1) {
      message.warning('请选择一张未结清结算单登记收款')
      return
    }
    const record = selectedReceivable[0]
    if (record) setReceiveRecord(record)
  }

  return (
    <DocumentListShell
      title="结算管理"
      createText="新建结算单"
      canCreate={canManageSettle}
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
      leftActions={canReceiveSettle && (
        <Button icon={<WalletOutlined />} disabled={selectedReceivable.length !== 1} onClick={handleReceiveSelected}>
          登记收款
        </Button>
      )}
      loading={ordersQuery.isLoading}
      onCreate={() => navigate('/settle-orders/create')}
      onQueueChange={(value) => {
        setQueueFilter(value)
        setPage(1)
        rowSelection.clear()
      }}
    >
      {ordersQuery.isError && (
        <QueryLoadErrorAlert
          description="结算单未成功加载，当前空表不代表没有待收款或已结算记录。"
          message="结算单加载失败"
          onRetry={() => void ordersQuery.refetch()}
        />
      )}
      {customersQuery.isError && (
        <QueryLoadErrorAlert
          description="客户筛选项未成功加载，当前客户选项可能不完整。"
          message="客户资料加载失败"
          onRetry={() => void customersQuery.refetch()}
        />
      )}
      <div className="document-page-table" data-table-density={tableDensity}>
        <SettleOrderTable
          canReceiveSettle={canReceiveSettle}
          data={orders}
          loading={ordersQuery.isLoading || ordersQuery.isFetching}
          onReload={() => ordersQuery.refetch()}
          rowClassName={rowSelection.rowClassName}
          rowSelection={rowSelection.rowSelection}
          onDetail={(record) => navigate(`/settle-orders/${record.uuid}`)}
          fixedHeader={tableDensity === 'fill'}
          onRow={rowSelection.onRow}
          onReceive={(record) => setReceiveRecord(record)}
        />
      </div>
      <DocumentPaginationBar
        current={page}
        pageSize={pageSize}
        total={ordersQuery.data?.total ?? 0}
        onChange={(nextPage, nextPageSize) => {
          setPage(nextPage)
          setPageSize(nextPageSize)
          rowSelection.clear()
        }}
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

function tableDensityMode(rowCount: number, pageSize: number, loading: boolean) {
  if (loading) return 'fill'
  if (rowCount === 0) return 'empty'
  return rowCount < pageSize ? 'short' : 'fill'
}

function settleStatus(filter: QueueFilter, filters: SettleQuery) {
  if (filters.settleStatus) return filters.settleStatus
  if (filter === 'pending') return 1
  if (filter === 'partial') return 2
  if (filter === 'paid') return 3
  return undefined
}
