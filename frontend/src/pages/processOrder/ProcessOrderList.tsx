import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { message } from 'antd'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType } from '@ant-design/pro-components'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderTableToolbarPortal } from '../../components/biz/tableToolbarPortalUtils'
import {
  pageProcessOrders,
  changeOrderStatus,
  calcProcessOrderFee,
} from '../../api/processOrder'
import type { ProcessOrder } from '../../types/processOrder'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { buildProcessOrderColumns } from './processOrderListColumns'
import type { BatchActions } from './ProcessOrderBatchToolbar'
import ProcessOrderListHeader from './ProcessOrderListHeader'
import ProcessOrderListDialogs from './ProcessOrderListDialogs'
import ProcessOrderPaginationBar from './ProcessOrderPaginationBar'
import ProcessOrderSearchBar, { type ProcessOrderSearchFilters } from './ProcessOrderSearchBar'
import type { QueueStatus } from './ProcessOrderQueueBar'
import { useProcessOrderCustomerEnum } from './useProcessOrderCustomerEnum'
import { useResizableProcessColumns } from './useResizableProcessColumns'
import { useProcessOrderRowSelection } from './useProcessOrderRowSelection'
import { useProcessOrderSearchShortcut } from './useProcessOrderSearchShortcut'
import { useProcessOrderPagination } from './useProcessOrderPagination'
import { useProcessOrderListDialogs } from './useProcessOrderListDialogs'
import './ProcessOrderList.css'

export default function ProcessOrderList() {
  const navigate = useNavigate()
  const actionRef = useRef<ActionType>(null)
  const [quickStatus, setQuickStatus] = useState<QueueStatus>('all')
  const rowSelection = useProcessOrderRowSelection()
  const dialogs = useProcessOrderListDialogs()
  const customerEnum = useProcessOrderCustomerEnum()
  const [searchFilters, setSearchFilters] = useState<ProcessOrderSearchFilters>({})
  const orderPagination = useProcessOrderPagination(20)
  const columnsState = useTableColumnsState('table-columns-process-order')
  useProcessOrderSearchShortcut()

  const openDetail = (uuid: string) => {
    navigate(`/process-orders/${uuid}`)
  }

  const handleTransition = async (record: ProcessOrder, target: number) => {
    await changeOrderStatus(record.uuid, { targetStatus: target })
    message.success('状态已更新')
    rowSelection.clear()
    actionRef.current?.reload()
  }

  const openPrint = (record: ProcessOrder) => {
    dialogs.openPrint({
      uuid: record.uuid,
      orderNo: record.orderNo,
      printCount: record.printCount,
    })
  }

  const openRecord = (uuid: string) => {
    navigate(`/process-orders/${uuid}/back-record`)
  }

  const handleCalcFee = async (record: ProcessOrder) => {
    const res = await calcProcessOrderFee(record.uuid)
    message.success(`计费完成，总额 ¥${res.totalAmount ?? 0}`)
    actionRef.current?.reload()
  }

  const handleQuickStatusChange = (value: QueueStatus) => {
    setQuickStatus(value)
    orderPagination.resetPage()
    rowSelection.clear()
  }

  const handleSearch = (filters: ProcessOrderSearchFilters) => {
    setSearchFilters(filters)
    orderPagination.resetPage()
    rowSelection.clear()
  }

  const handlePageChange = (current: number, pageSize: number) => {
    orderPagination.changePage(current, pageSize)
    rowSelection.clear()
  }

  const columns = buildProcessOrderColumns({
    customerEnum,
    onBackRecord: openRecord,
    onCalcFee: handleCalcFee,
    onChangeStatus: handleTransition,
    onDetail: openDetail,
    onEditDraft: (uuid) => navigate(`/process-orders/create?draft=${uuid}`),
    onGoDelivery: () => navigate('/delivery-orders'),
    onGoSettle: () => navigate('/settle-orders'),
    onManageRolls: dialogs.openManageRoll,
    onPrint: openPrint,
    onSnapshotDiff: dialogs.openDiff,
  })
  const resizableTable = useResizableProcessColumns(columns)
  const batchActions: BatchActions = {
    onBackRecord: openRecord,
    onCalcFee: handleCalcFee,
    onChangeStatus: handleTransition,
    onGoDelivery: () => navigate('/delivery-orders'),
    onGoSettle: () => navigate('/settle-orders'),
    onManageRolls: dialogs.openManageRoll,
    onPrint: openPrint,
    onSnapshotDiff: dialogs.openDiff,
  }

  return (
    <>
      <ProcessOrderListHeader
        actions={batchActions}
        onCreate={() => navigate('/process-orders/create')}
        onQuickStatusChange={handleQuickStatusChange}
        quickStatus={quickStatus}
        search={<ProcessOrderSearchBar customerEnum={customerEnum} onSearch={handleSearch} />}
        selectedRows={rowSelection.selectedRows}
      >
        <ProTable<ProcessOrder>
          className="process-order-table"
          rowKey="uuid"
          actionRef={actionRef}
          columns={resizableTable.columns}
          components={resizableTable.components}
          columnsState={columnsState}
          bordered
          cardProps={false}
          headerTitle={false}
          toolBarRender={() => []}
          optionsRender={renderTableToolbarPortal}
          options={mesProTableOptions()}
          params={{
            quickStatus,
            ...searchFilters,
            pageCurrent: orderPagination.pagination.current,
            pageSizeValue: orderPagination.pagination.pageSize,
          }}
          rowSelection={rowSelection.rowSelection}
          rowClassName={rowSelection.rowClassName}
          onRow={rowSelection.onRow}
          scroll={{ x: resizableTable.scrollX, y: '100%' }}
          tableLayout="fixed"
          tableAlertRender={false}
          tableAlertOptionRender={false}
          request={async (params) => {
            const hasSearchStatus = params.orderStatus != null && params.orderStatus !== ''
            const statusFromSearch = hasSearchStatus ? Number(params.orderStatus) : undefined
            const statusFromQueue = quickStatus === 'all' ? undefined : Number(quickStatus)
            const res = await pageProcessOrders({
              current: orderPagination.pagination.current,
              size: orderPagination.pagination.pageSize,
              keyword: params.keyword,
              orderStatus: statusFromSearch ?? statusFromQueue,
              customerUuid: params.customerUuid,
              dateFrom: params.dateFrom,
              dateTo: params.dateTo,
            })
            orderPagination.updateTotal(res.total ?? 0)
            return { data: res.records ?? [], total: res.total ?? 0, success: true }
          }}
          pagination={false}
          search={false}
        />
        <ProcessOrderPaginationBar
          current={orderPagination.pagination.current}
          pageSize={orderPagination.pagination.pageSize}
          total={orderPagination.pagination.total}
          onChange={handlePageChange}
        />
      </ProcessOrderListHeader>

      <ProcessOrderListDialogs
        state={dialogs.state}
        actions={{
          onCloseDiff: dialogs.closeDiff,
          onCloseManageRoll: dialogs.closeManageRoll,
          onClosePrint: dialogs.closePrint,
          onRefresh: () => actionRef.current?.reload(),
        }}
      />
    </>
  )
}
