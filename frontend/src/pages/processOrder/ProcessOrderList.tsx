import { useRef } from 'react'
import type { ActionType } from '@ant-design/pro-components'
import { buildProcessOrderColumns } from './processOrderListColumns'
import ProcessOrderListHeader from './ProcessOrderListHeader'
import ProcessOrderListDialogs from './ProcessOrderListDialogs'
import ProcessOrderListTable from './ProcessOrderListTable'
import ProcessOrderSearchBar from './ProcessOrderSearchBar'
import { useProcessOrderCustomerEnum } from './useProcessOrderCustomerEnum'
import { useProcessOrderRowSelection } from './useProcessOrderRowSelection'
import { useProcessOrderSearchShortcut } from './useProcessOrderSearchShortcut'
import { useProcessOrderListDialogs } from './useProcessOrderListDialogs'
import { useProcessOrderListPageState } from './useProcessOrderListPageState'
import { useProcessOrderListCommands } from './useProcessOrderListCommands'
import { useProcessOrderListCapabilities } from './useProcessOrderListCapabilities'
import './ProcessOrderList.css'

export default function ProcessOrderList() {
  const actionRef = useRef<ActionType>()
  const rowSelection = useProcessOrderRowSelection()
  const dialogs = useProcessOrderListDialogs()
  const customerEnum = useProcessOrderCustomerEnum()
  const listState = useProcessOrderListPageState()
  const capabilities = useProcessOrderListCapabilities()
  const commands = useProcessOrderListCommands({
    actionRef,
    capabilities,
    clearSelection: rowSelection.clear,
    customerEnum,
    dialogs,
  })
  useProcessOrderSearchShortcut()

  const handleQuickStatusChange = (value: Parameters<typeof listState.setQuickStatus>[0]) => {
    listState.setQuickStatus(value)
    rowSelection.clear()
  }

  const handleSearch = (filters: Parameters<typeof listState.setFilters>[0]) => {
    listState.setFilters(filters)
    rowSelection.clear()
  }

  const handlePageChange = (current: number, pageSize: number) => {
    listState.changePage(current, pageSize)
    rowSelection.clear()
  }

  const columns = buildProcessOrderColumns(commands.columnOptions)

  return (
    <>
      <ProcessOrderListHeader
        actions={commands.batchActions}
        capabilities={capabilities}
        onCreate={commands.onCreate}
        onQuickStatusChange={handleQuickStatusChange}
        quickStatus={listState.quickStatus}
        search={(
          <ProcessOrderSearchBar
            key={[
              listState.filters.keyword,
              listState.filters.customerUuid,
              listState.filters.dateFrom,
              listState.filters.dateTo,
            ].join('|')}
            customerEnum={customerEnum}
            filters={listState.filters}
            onSearch={handleSearch}
          />
        )}
        selectedRows={rowSelection.selectedRows}
      >
        <ProcessOrderListTable
          actionRef={actionRef}
          columns={columns}
          listState={listState}
          onPageChange={handlePageChange}
          rowSelection={rowSelection}
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
