import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Input, Modal, message } from 'antd'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType } from '@ant-design/pro-components'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderTableToolbarPortal } from '../../components/biz/tableToolbarPortalUtils'
import {
  pageProcessOrders,
  changeOrderStatus,
  calcProcessOrderFee,
  rollbackProcessOrderToDraft,
  voidProcessOrder,
} from '../../api/processOrder'
import type { ProcessOrder } from '../../types/processOrder'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { buildProcessOrderColumns } from './processOrderListColumns'
import type { BatchActions } from './ProcessOrderBatchToolbar'
import ProcessOrderListHeader from './ProcessOrderListHeader'
import ProcessOrderListDialogs from './ProcessOrderListDialogs'
import ProcessOrderPaginationBar from './ProcessOrderPaginationBar'
import ProcessOrderSearchBar, { type ProcessOrderSearchFilters } from './ProcessOrderSearchBar'
import { confirmOrderStatusChange, isRollbackStatusChange } from '../../features/processOrderDetail/confirmOrderStatusChange'
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
  const [visibleRowCount, setVisibleRowCount] = useState(0)
  const orderPagination = useProcessOrderPagination(20)
  const columnsState = useTableColumnsState('table-columns-process-order')
  useProcessOrderSearchShortcut()

  const openDetail = (uuid: string) => {
    navigate(`/process-orders/${uuid}`)
  }

  const handleTransition = async (record: ProcessOrder, target: number, reason?: string) => {
    if (target === 0) {
      await rollbackProcessOrderToDraft(record.uuid, { reason: reason ?? '' })
    } else {
      await changeOrderStatus(record.uuid, { reason, targetStatus: target })
    }
    message.success('状态已更新')
    rowSelection.clear()
    actionRef.current?.reload()
    if (target === 0) {
      navigate(`/process-orders/create?draft=${record.uuid}`)
    }
  }

  const confirmTransition = (record: ProcessOrder, target: number, title: string) => {
    const currentStatus = record.orderStatus ?? 0
    const requireReason = isRollbackStatusChange(currentStatus, target)
    confirmOrderStatusChange({
      title,
      orderNo: record.orderNo,
      okText: requireReason ? '确认回退' : '确认',
      danger: requireReason,
      requireReason,
      reasonPlaceholder: '请填写回退原因，例如：客户改单、现场方案调整、下发前补充信息',
      onConfirm: (reason) => handleTransition(record, target, reason),
    })
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

  const handleVoidOrder = async (record: ProcessOrder) => {
    let reason = ''
    Modal.confirm({
      title: '作废加工单',
      content: (
        <Input.TextArea
          autoSize={{ minRows: 3, maxRows: 5 }}
          maxLength={255}
          placeholder="请填写作废原因"
          showCount
          onChange={(event) => {
            reason = event.target.value
          }}
        />
      ),
      okButtonProps: { danger: true },
      okText: '确认作废',
      cancelText: '取消',
      onOk: async () => {
        const trimmed = reason.trim()
        if (!trimmed) {
          message.warning('请填写作废原因')
          throw new Error('作废原因不能为空')
        }
        await voidProcessOrder(record.uuid, { reason: trimmed })
        message.success('加工单已作废')
        rowSelection.clear()
        actionRef.current?.reload()
      },
    })
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
    onChangeStatus: confirmTransition,
    onDetail: openDetail,
    onEditDraft: (uuid) => navigate(`/process-orders/create?draft=${uuid}`),
    onGoDelivery: () => navigate('/delivery-orders'),
    onGoSettle: () => navigate('/settle-orders'),
    onManageRolls: dialogs.openManageRoll,
    onPrint: openPrint,
    onSnapshotDiff: dialogs.openDiff,
    onVoidOrder: handleVoidOrder,
  })
  const tableDensity = tableDensityMode(visibleRowCount, orderPagination.pagination.pageSize)
  const resizableTable = useResizableProcessColumns(columns)
  const tableScroll = {
    x: resizableTable.scrollX,
    ...(tableDensity === 'fill' ? { y: '100%' } : {}),
  }
  const batchActions: BatchActions = {
    onBackRecord: openRecord,
    onCalcFee: handleCalcFee,
    onChangeStatus: confirmTransition,
    onGoDelivery: () => navigate('/delivery-orders'),
    onGoSettle: () => navigate('/settle-orders'),
    onManageRolls: dialogs.openManageRoll,
    onPrint: openPrint,
    onSnapshotDiff: dialogs.openDiff,
    onVoidOrder: handleVoidOrder,
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
          className={`process-order-table process-order-table--${tableDensity}`}
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
          locale={{ emptyText: '暂无加工单' }}
          scroll={tableScroll}
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
            setVisibleRowCount(res.records?.length ?? 0)
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

function tableDensityMode(rowCount: number, pageSize: number) {
  if (rowCount === 0) return 'empty'
  return rowCount < pageSize ? 'short' : 'fill'
}
