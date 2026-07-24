import { useState } from 'react'
import { Form } from 'antd'
import { useNavigate } from 'react-router-dom'
import DocumentListShell from '../../components/biz/DocumentListShell'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useDocumentRowSelection } from '../../components/biz/useDocumentRowSelection'
import DeliveryOrderTable from '../../features/delivery/components/DeliveryOrderTable'
import { useDeliveryListSummary } from '../../features/delivery/hooks/useDeliveryListSummary'
import { useDeliveryOrders } from '../../features/delivery/hooks/useDeliveryOrders'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import { useConfiguredPageSize } from '../../features/systemConfig/hooks/useConfiguredPageSize'
import type { DeliveryOrder, DeliveryQuery } from '../../types/delivery'
import DeliveryListSummary from './DeliveryListSummary'
import DeliveryListToolbar from './DeliveryListToolbar'
import DeliverySearchBar from './DeliverySearchBar'
import { deliveryStatus, tableDensityMode, type DeliveryQueueFilter, type DeliverySearchFormValues } from './deliveryListModel'
import { useDeliveryListActions } from './useDeliveryListActions'
import './DeliveryOrderList.css'

export default function DeliveryOrderList() {
  const [form] = Form.useForm<DeliverySearchFormValues>()
  const navigate = useNavigate()
  const customersQuery = useCustomers()
  const [queue, setQueue] = useState<DeliveryQueueFilter>('all')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useConfiguredPageSize(20)
  const [filters, setFilters] = useState<DeliveryQuery>({})
  const selection = useDocumentRowSelection<DeliveryOrder>()
  const ordersQuery = useDeliveryOrders({ ...filters, current: page, deliveryStatus: deliveryStatus(queue), size: pageSize })
  const summaryQuery = useDeliveryListSummary(filters)
  const orders = ordersQuery.data?.records ?? []
  const tableDensity = tableDensityMode(orders.length, pageSize, ordersQuery.isLoading)
  const actions = useDeliveryListActions({ clearSelection: selection.clear, filters, queue,
    refetch: () => void ordersQuery.refetch(), selectedRows: selection.selectedRows })

  const handleSearch = (values: DeliverySearchFormValues) => {
    setFilters(searchFilters(values))
    setPage(1)
    selection.clear()
  }

  const handleReset = () => {
    form.resetFields()
    setFilters({})
    setPage(1)
    selection.clear()
  }

  const handleQueueChange = (value: DeliveryQueueFilter) => {
    setQueue(value)
    setPage(1)
    selection.clear()
  }

  const handlePageChange = (nextPage: number, nextSize: number) => {
    setPage(nextPage)
    setPageSize(nextSize)
    selection.clear()
  }

  return (
    <DocumentListShell title="出库管理" createText="新建出库单" canCreate={actions.canManage}
      queue={queue} queueOptions={queueOptions(summaryQuery.data)}
      search={<DeliverySearchBar form={form} customers={customersQuery.data?.records ?? []}
        loadingCustomers={customersQuery.isLoading} onReset={handleReset} onSearch={handleSearch} />}
      leftActions={<DeliveryListToolbar actions={actions} />} loading={ordersQuery.isLoading}
      summary={<DeliveryListSummary summary={summaryQuery.data} />}
      onCreate={() => navigate('/delivery-orders/create')} onQueueChange={handleQueueChange}>
      <ListErrors ordersQuery={ordersQuery} customersQuery={customersQuery} summaryQuery={summaryQuery} />
      <div className="document-page-table" data-table-density={tableDensity}>
        <DeliveryOrderTable canConfirmDelivery={actions.canConfirm} data={orders}
          loading={ordersQuery.isLoading || ordersQuery.isFetching} onReload={() => ordersQuery.refetch()}
          rowClassName={selection.rowClassName} rowSelection={selection.rowSelection} onConfirm={actions.confirm}
          onDetail={(record) => navigate(`/delivery-orders/${record.uuid}`)}
          fixedHeader={tableDensity !== 'empty'} onRow={selection.onRow} />
      </div>
      <DocumentPaginationBar current={page} pageSize={pageSize} total={ordersQuery.data?.total ?? 0} onChange={handlePageChange} />
    </DocumentListShell>
  )
}

function searchFilters(values: DeliverySearchFormValues): DeliveryQuery {
  return { customerUuid: values.customerUuid, dateFrom: values.dateRange?.[0]?.format('YYYY-MM-DD'),
    dateTo: values.dateRange?.[1]?.format('YYYY-MM-DD'), keyword: values.keyword?.trim() || undefined }
}

function queueOptions(summary: ReturnType<typeof useDeliveryListSummary>['data']):
  { label: string; value: DeliveryQueueFilter }[] {
  return [
    { label: `全部 ${summary?.totalDocumentCount ?? '-'}`, value: 'all' },
    { label: `待出库 ${summary?.pendingDocumentCount ?? '-'}`, value: 'pending' },
    { label: `已出库 ${summary?.deliveredDocumentCount ?? '-'}`, value: 'done' },
    { label: `已作废 ${summary?.voidDocumentCount ?? '-'}`, value: 'void' },
  ]
}

function ListErrors({ customersQuery, ordersQuery, summaryQuery }: {
  customersQuery: ReturnType<typeof useCustomers>
  ordersQuery: ReturnType<typeof useDeliveryOrders>
  summaryQuery: ReturnType<typeof useDeliveryListSummary>
}) {
  return <>
    {ordersQuery.isError && <QueryLoadErrorAlert message="出库单加载失败" description="出库单未成功加载，当前空表不代表没有出库记录。" onRetry={() => void ordersQuery.refetch()} />}
    {customersQuery.isError && <QueryLoadErrorAlert message="客户资料加载失败" description="客户筛选项未成功加载，当前客户选项可能不完整。" onRetry={() => void customersQuery.refetch()} />}
    {summaryQuery.isError && <QueryLoadErrorAlert message="出库汇总加载失败" description="出库汇总未成功加载，表格数据仍可继续查看。" onRetry={() => void summaryQuery.refetch()} />}
  </>
}
