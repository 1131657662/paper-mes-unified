import { useRef, useState } from 'react'
import { Button } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import { getOperationLogs } from '../../api/operationLog'
import { mesTablePagination } from '../../components/biz/mesPaginationUtils'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderCompatibleTableOptions } from '../../components/biz/tableToolbarOptionsRender'
import TooltipText from '../../components/biz/TooltipText'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import type { OperationLog } from '../../types/operationLog'
import { ACTION_TYPES, BIZ_TYPES } from '../../constants/operationLog'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import OperationLogDetailDrawer from './OperationLogDetailDrawer'
import { actionTag, logText } from './operationLogDisplay'
import './OperationLogPage.css'

const BIZ_ROUTE_PREFIX: Record<string, string> = {
  加工单: '/process-orders',
  出库单: '/delivery-orders',
  结算单: '/settle-orders',
  系统用户: '/users',
  系统配置: '/system-config',
  数据安全: '/system-config',
}

export default function OperationLogPage() {
  const actionRef = useRef<ActionType>()
  const columnsState = useTableColumnsState('table-columns-operation-log')
  const navigate = useNavigate()
  const [selectedLog, setSelectedLog] = useState<OperationLog>()

  const columns: ProColumns<OperationLog>[] = [
    { title: '操作时间', dataIndex: 'operateTime', valueType: 'dateTime', width: 170, hideInSearch: true },
    {
      title: '操作日期',
      dataIndex: 'dateRange',
      valueType: 'dateRange',
      hideInTable: true,
      search: { transform: (value) => ({ dateFrom: value[0], dateTo: value[1] }) },
    },
    { title: '业务类型', dataIndex: 'bizType', width: 110, valueType: 'select', valueEnum: BIZ_TYPES },
    {
      title: '业务单号',
      dataIndex: 'bizNo',
      width: 150,
      render: (_, record) => bizNoCell(record, navigate),
    },
    {
      title: '动作类型',
      dataIndex: 'actionType',
      width: 120,
      valueType: 'select',
      valueEnum: ACTION_TYPES,
      render: (_, record) => actionTag(record.actionType),
    },
    { title: '操作人', dataIndex: 'operator', width: 110, render: (_, record) => textCell(record.operator) },
    { title: '字段名', dataIndex: 'fieldName', width: 130, render: (_, record) => textCell(record.fieldName) },
    {
      title: '变更内容',
      dataIndex: 'change',
      width: 260,
      hideInSearch: true,
      render: (_, record) => <ChangeCell log={record} />,
    },
    { title: '备注', dataIndex: 'remark', width: 220, render: (_, record) => textCell(record.remark) },
    {
      title: '操作',
      key: 'actions',
      valueType: 'option',
      width: 90,
      render: (_, record) => (
        <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => setSelectedLog(record)}>
          查看
        </Button>
      ),
    },
  ]
  const resizable = useResizableTableColumns<OperationLog, ProColumns<OperationLog>>(columns, 'operation-log')

  return (
    <>
      <ProTable<OperationLog>
        className="mes-pro-table-page operation-log-page"
        columns={resizable.columns}
        columnsState={columnsState}
        components={resizable.components}
        actionRef={actionRef}
        headerTitle="操作日志"
        rowKey="uuid"
        request={async (params) => {
          const res = await getOperationLogs({
            actionType: params.actionType,
            bizNo: params.bizNo,
            bizType: params.bizType,
            current: params.current,
            dateFrom: params.dateFrom,
            dateTo: params.dateTo,
            fieldName: params.fieldName,
            operator: params.operator,
            remark: params.remark,
            size: params.pageSize,
          })
          return { data: res.records || [], total: res.total || 0, success: true }
        }}
        bordered
        pagination={mesTablePagination(20)}
        scroll={{ x: resizable.scrollX, y: '100%' }}
        search={{ defaultCollapsed: false, labelWidth: 'auto' }}
        options={mesProTableOptions()}
        optionsRender={renderCompatibleTableOptions}
        tableLayout="fixed"
        dateFormatter="string"
      />

      <OperationLogDetailDrawer
        log={selectedLog}
        businessPath={selectedLog ? businessPath(selectedLog) : undefined}
        onClose={() => setSelectedLog(undefined)}
        onOpenBusiness={(path) => {
          setSelectedLog(undefined)
          navigate(path)
        }}
      />
    </>
  )
}

function ChangeCell({ log }: { log: OperationLog }) {
  if (!log.fieldName && !log.oldValue && !log.newValue) {
    return <span className="operation-log-muted">-</span>
  }
  return (
    <div className="operation-log-change-cell">
      <span>
        <em>前</em>
        <TooltipText value={logText(log.oldValue)} />
      </span>
      <span>
        <em>后</em>
        <TooltipText value={logText(log.newValue)} />
      </span>
    </div>
  )
}

function bizNoCell(record: OperationLog, navigate: (path: string) => void) {
  if (!record.bizNo) return '-'
  const path = businessPath(record)
  if (!path) return <TooltipText value={logText(record.bizNo)} />
  return (
    <Button className="operation-log-biz-link" type="link" size="small" onClick={() => navigate(path)}>
      {logText(record.bizNo)}
    </Button>
  )
}

function businessPath(record: OperationLog): string | undefined {
  const prefix = BIZ_ROUTE_PREFIX[record.bizType]
  if (!prefix) return undefined
  if (record.bizType === '系统配置' || record.bizType === '数据安全') return prefix
  return record.bizUuid ? `${prefix}/${record.bizUuid}` : undefined
}

function textCell(text?: unknown) {
  if (typeof text === 'string' || typeof text === 'number') {
    return <TooltipText value={logText(text)} />
  }
  return '-'
}
