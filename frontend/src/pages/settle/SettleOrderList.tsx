import { useState } from 'react'
import { Form, message } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import DocumentListShell from '../../components/biz/DocumentListShell'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import { useDocumentRowSelection } from '../../components/biz/useDocumentRowSelection'
import { PERMISSIONS } from '../../constants/permissions'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import SettleOrderTable from '../../features/settle/components/SettleOrderTable'
import { useSettleOrders } from '../../features/settle/hooks/useSettleOrders'
import { useSettleListSummary } from '../../features/settle/hooks/useSettleListSummary'
import { useSettleCollectionSummary } from '../../features/settle/hooks/useSettleCollectionSummary'
import SettleCollectionQueueBar from '../../features/settle/components/SettleCollectionQueueBar'
import { useHasPermission } from '../../stores/authStore'
import type { SettleCollectionQueue, SettleOrder } from '../../types/settle'
import ReceiveModal from './ReceiveModal'
import CollectionReminderModal from './CollectionReminderModal'
import SettleSearchBar, { type SettleSearchFormValues } from './SettleSearchBar'
import SettleListSummary from './SettleListSummary'
import SettleSelectionBar from './SettleSelectionBar'
import SettleListErrors from './SettleListErrors'
import SettleListModeActions from './SettleListModeActions'
import { collectionQueueOptions, documentQueueOptions } from './settleQueueOptions'
import { settleListLocation } from './settleListNavigation'
import { type QueueFilter } from './settleListUrlState'
import { useSettleListPageState } from './useSettleListPageState'
import './SettleOrderList.css'

export default function SettleOrderList() {
  const [form] = Form.useForm<SettleSearchFormValues>()
  const location = useLocation()
  const navigate = useNavigate()
  const customersQuery = useCustomers()
  const pageState = useSettleListPageState()
  const { collectionQueue, filters, page, pageSize, queueFilter, viewMode } = pageState
  const [receiveRecord, setReceiveRecord] = useState<SettleOrder | null>(null)
  const [reminderRecord, setReminderRecord] = useState<SettleOrder | null>(null)
  const canManageSettle = useHasPermission(PERMISSIONS.settleManage)
  const canReceiveSettle = useHasPermission(PERMISSIONS.settleReceive)
  const rowSelection = useDocumentRowSelection<SettleOrder>()
  const query = {
    ...filters,
    current: page,
    size: pageSize,
    ...(viewMode === 'collection'
      ? { collectionQueue }
      : { settleStatus: settleStatus(queueFilter) }),
  }
  const ordersQuery = useSettleOrders(query)
  const summaryQuery = useSettleListSummary(filters, viewMode === 'documents')
  const collectionSummaryQuery = useSettleCollectionSummary(filters, viewMode === 'collection')
  const summary = summaryQuery.data
  const orders = ordersQuery.data?.records ?? []
  const activeDocumentCount = summary
    ? summary.pendingDocumentCount + summary.partialDocumentCount + summary.paidDocumentCount
    : '-'
  const tableDensity = tableDensityMode(orders.length, pageSize, ordersQuery.isLoading)
  const selectedReceivable = rowSelection.selectedRows.filter((record) => [1, 2].includes(record.settleStatus))

  const handleSearch = (values: SettleSearchFormValues) => {
    pageState.setFilters({
      customerUuid: values.customerUuid,
      dateFrom: values.dateRange?.[0]?.format('YYYY-MM-DD'),
      dateTo: values.dateRange?.[1]?.format('YYYY-MM-DD'),
      keyword: values.keyword?.trim() || undefined,
      settleType: values.settleType,
    })
    rowSelection.clear()
  }

  const handleReset = () => {
    form.setFieldValue('customerUuid', undefined)
    form.setFieldValue('dateRange', null)
    form.setFieldValue('keyword', undefined)
    form.setFieldValue('settleType', undefined)
    pageState.setFilters({})
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
      queue={viewMode === 'collection' ? collectionQueue : queueFilter}
      queueOptions={viewMode === 'collection'
        ? collectionQueueOptions(collectionSummaryQuery.data)
        : documentQueueOptions(summary, activeDocumentCount)}
      search={(
        <SettleSearchBar
          form={form}
          customers={customersQuery.data?.records ?? []}
          initialValues={pageState.formInitialValues}
          loadingCustomers={customersQuery.isLoading}
          onReset={handleReset}
          onSearch={handleSearch}
        />
      )}
      leftActions={<SettleListModeActions canReceive={canReceiveSettle}
        receiveDisabled={selectedReceivable.length !== 1} value={viewMode} onReceive={handleReceiveSelected}
        onChange={(value) => {
          pageState.setViewMode(value)
          rowSelection.clear()
        }} />}
      loading={ordersQuery.isLoading}
      onCreate={() => navigate('/settle-orders/create', {
        state: { from: settleListLocation(location.pathname, location.search) },
      })}
      onQueueChange={(value) => {
        if (viewMode === 'collection') pageState.setCollectionQueue(value as SettleCollectionQueue)
        else pageState.setQueueFilter(value as QueueFilter)
        rowSelection.clear()
      }}
    >
      <SettleListErrors ordersError={ordersQuery.isError} customersError={customersQuery.isError}
        summaryError={viewMode === 'collection' ? collectionSummaryQuery.isError : summaryQuery.isError}
        onRetryOrders={() => void ordersQuery.refetch()} onRetryCustomers={() => void customersQuery.refetch()}
        onRetrySummary={() => void (viewMode === 'collection' ? collectionSummaryQuery.refetch() : summaryQuery.refetch())} />
      {viewMode === 'collection' ? (
        <SettleCollectionQueueBar active={collectionQueue} summary={collectionSummaryQuery.data}
          onChange={(value) => {
            pageState.setCollectionQueue(value)
            rowSelection.clear()
          }} />
      ) : <SettleListSummary summary={summary} />}
      <div className="document-page-table" data-table-density={tableDensity}>
        <SettleOrderTable
          canReceiveSettle={canReceiveSettle}
          collectionMode={viewMode === 'collection'}
          data={orders}
          loading={ordersQuery.isLoading || ordersQuery.isFetching}
          onReload={() => ordersQuery.refetch()}
          rowClassName={rowSelection.rowClassName}
          rowSelection={{
            ...rowSelection.rowSelection,
            getCheckboxProps: (record) => ({ disabled: ![1, 2].includes(record.settleStatus) }),
          }}
          onDetail={(record) => navigate(`/settle-orders/${record.uuid}`, {
            state: { from: settleListLocation(location.pathname, location.search) },
          })}
          fixedHeader={tableDensity === 'fill'}
          onRow={(record) => [1, 2].includes(record.settleStatus) ? rowSelection.onRow(record) : {}}
          onReceive={(record) => setReceiveRecord(record)}
          onRemind={canReceiveSettle ? (record) => setReminderRecord(record) : undefined}
        />
      </div>
      <DocumentPaginationBar
        current={page}
        pageSize={pageSize}
        total={ordersQuery.data?.total ?? 0}
        onChange={(nextPage, nextPageSize) => {
          if (nextPageSize !== pageSize) pageState.setPageSize(nextPageSize)
          else pageState.setPage(nextPage)
          rowSelection.clear()
        }}
      />
      <SettleSelectionBar
        selectedRows={rowSelection.selectedRows}
        selectedReceivable={selectedReceivable}
        onClear={rowSelection.clear}
        onReceive={handleReceiveSelected}
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
      <CollectionReminderModal record={reminderRecord} onClose={() => setReminderRecord(null)} />
    </DocumentListShell>
  )
}

function tableDensityMode(rowCount: number, pageSize: number, loading: boolean) {
  if (loading) return 'fill'
  if (rowCount === 0) return 'empty'
  return rowCount < pageSize ? 'short' : 'fill'
}

function settleStatus(filter: QueueFilter) {
  if (filter === 'pending') return 1
  if (filter === 'partial') return 2
  if (filter === 'paid') return 3
  if (filter === 'void') return 4
  return undefined
}
