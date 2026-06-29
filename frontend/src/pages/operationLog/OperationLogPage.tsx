import { useRef } from 'react'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { Tag } from 'antd'
import { getOperationLogs } from '../../api/operationLog'
import type { OperationLog } from '../../types/operationLog'
import { ACTION_TYPES, BIZ_TYPES } from '../../constants/operationLog'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'

export default function OperationLogPage() {
  const actionRef = useRef<ActionType>()
  const columnsState = useTableColumnsState('table-columns-operation-log')

  const columns: ProColumns<OperationLog>[] = [
    {
      title: '操作时间',
      dataIndex: 'operateTime',
      valueType: 'dateTime',
      width: 160,
      hideInSearch: true,
      sorter: true,
    },
    {
      title: '操作日期',
      dataIndex: 'dateRange',
      valueType: 'dateRange',
      hideInTable: true,
      search: {
        transform: (value) => ({
          dateFrom: value[0],
          dateTo: value[1],
        }),
      },
    },
    {
      title: '业务类型',
      dataIndex: 'bizType',
      width: 100,
      valueType: 'select',
      valueEnum: BIZ_TYPES,
    },
    {
      title: '业务单号',
      dataIndex: 'bizNo',
      width: 140,
    },
    {
      title: '动作类型',
      dataIndex: 'actionType',
      width: 100,
      valueType: 'select',
      valueEnum: ACTION_TYPES,
      render: (_, record) => {
        const colorMap: Record<string, string> = {
          回退: 'warning',
          超差放行: 'error',
          作废卷号: 'default',
          回录: 'processing',
          补打: 'default',
          出库确认: 'success',
          结算: 'success',
          收款: 'success',
          字段修改: 'default',
        }
        return <Tag className="mes-data-tag" color={colorMap[record.actionType] || 'default'}>{record.actionType}</Tag>
      },
    },
    {
      title: '操作人',
      dataIndex: 'operator',
      width: 100,
    },
    {
      title: '字段名',
      dataIndex: 'fieldName',
      width: 120,
      hideInSearch: true,
      render: (text) => text || '-',
    },
    {
      title: '修改前',
      dataIndex: 'oldValue',
      width: 120,
      hideInSearch: true,
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: '修改后',
      dataIndex: 'newValue',
      width: 120,
      hideInSearch: true,
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: '备注',
      dataIndex: 'remark',
      ellipsis: true,
      hideInSearch: true,
      render: (text) => text || '-',
    },
  ]

  return (
    <ProTable<OperationLog>
      className="mes-pro-table-page"
      columns={columns}
      columnsState={columnsState}
      actionRef={actionRef}
      headerTitle="操作日志"
      rowKey="uuid"
      request={async (params) => {
        const res = await getOperationLogs({
          current: params.current,
          size: params.pageSize,
          bizType: params.bizType,
          bizNo: params.bizNo,
          actionType: params.actionType,
          operator: params.operator,
          dateFrom: params.dateFrom,
          dateTo: params.dateTo,
        })
        return {
          data: res.records || [],
          total: res.total || 0,
          success: true,
        }
      }}
      bordered
      pagination={{
        defaultPageSize: 20,
        showSizeChanger: true,
        showQuickJumper: true,
        pageSizeOptions: [10, 20, 50, 100, 200, 500, 1000],
      }}
      scroll={{ x: 'max-content' }}
      search={{
        labelWidth: 'auto',
        defaultCollapsed: false,
      }}
      options={{
        reload: true,
        density: true,
        setting: true,
      }}
      dateFormatter="string"
    />
  )
}
