import { useState, type RefObject } from 'react'
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import { pageProcessOrders } from '../../api/processOrder'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderTableToolbarPortal } from '../../components/biz/tableToolbarPortalUtils'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import type { ProcessOrder } from '../../types/processOrder'
import ProcessOrderPaginationBar from './ProcessOrderPaginationBar'
import { useResizableProcessColumns } from './useResizableProcessColumns'
import type { useProcessOrderListPageState } from './useProcessOrderListPageState'
import type { useProcessOrderRowSelection } from './useProcessOrderRowSelection'

interface Props {
  actionRef: RefObject<ActionType | undefined>
  columns: ProColumns<ProcessOrder>[]
  listState: ReturnType<typeof useProcessOrderListPageState>
  onPageChange: (page: number, pageSize: number) => void
  rowSelection: ReturnType<typeof useProcessOrderRowSelection>
}

export default function ProcessOrderListTable({ actionRef, columns, listState, onPageChange, rowSelection }: Props) {
  const [visibleRowCount, setVisibleRowCount] = useState(0)
  const [total, setTotal] = useState(0)
  const [loadError, setLoadError] = useState(false)
  const columnsState = useTableColumnsState('table-columns-process-order')
  const resizableTable = useResizableProcessColumns(columns)
  const density = tableDensityMode(visibleRowCount, listState.pageSize)

  return (
    <>
      {loadError && (
        <QueryLoadErrorAlert
          message="加工单加载失败"
          description="请检查网络或服务状态后重新加载。当前筛选条件已保留。"
          onRetry={() => actionRef.current?.reload()}
        />
      )}
      <ProTable<ProcessOrder>
        actionRef={actionRef}
        bordered
        cardProps={false}
        className={`process-order-table process-order-table--${density}`}
        columns={resizableTable.columns}
        columnsState={columnsState}
        components={resizableTable.components}
        headerTitle={false}
        locale={{ emptyText: '暂无加工单' }}
        onRow={rowSelection.onRow}
        options={mesProTableOptions()}
        optionsRender={renderTableToolbarPortal}
        pagination={false}
        params={{ ...listState.filters, page: listState.page, pageSize: listState.pageSize, quickStatus: listState.quickStatus }}
        request={(params) => loadOrders({ listState, params, setLoadError, setTotal, setVisibleRowCount })}
        rowClassName={rowSelection.rowClassName}
        rowKey="uuid"
        rowSelection={rowSelection.rowSelection}
        scroll={{ x: resizableTable.scrollX, ...(density === 'fill' ? { y: '100%' } : {}) }}
        search={false}
        tableAlertOptionRender={false}
        tableAlertRender={false}
        tableLayout="fixed"
        toolBarRender={() => []}
      />
      <ProcessOrderPaginationBar
        current={listState.page}
        pageSize={listState.pageSize}
        total={total}
        onChange={onPageChange}
      />
    </>
  )
}

interface LoadParams {
  listState: ReturnType<typeof useProcessOrderListPageState>
  params: Record<string, unknown>
  setLoadError: (value: boolean) => void
  setTotal: (value: number) => void
  setVisibleRowCount: (value: number) => void
}

async function loadOrders({ listState, params, setLoadError, setTotal, setVisibleRowCount }: LoadParams) {
  setLoadError(false)
  try {
    const result = await pageProcessOrders({
      current: listState.page,
      size: listState.pageSize,
      keyword: textParam(params.keyword),
      orderStatus: listState.quickStatus === 'all' ? undefined : Number(listState.quickStatus),
      customerUuid: textParam(params.customerUuid),
      dateFrom: textParam(params.dateFrom),
      dateTo: textParam(params.dateTo),
    })
    setVisibleRowCount(result.records?.length ?? 0)
    setTotal(result.total ?? 0)
    return { data: result.records ?? [], total: result.total ?? 0, success: true }
  } catch {
    setVisibleRowCount(0)
    setTotal(0)
    setLoadError(true)
    return { data: [], total: 0, success: false }
  }
}

function textParam(value: unknown) {
  return typeof value === 'string' ? value : undefined
}

function tableDensityMode(rowCount: number, pageSize: number) {
  if (rowCount === 0) return 'empty'
  return rowCount < pageSize ? 'short' : 'fill'
}
