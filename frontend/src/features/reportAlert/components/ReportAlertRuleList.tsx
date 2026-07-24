import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { Badge, Button, Empty, Popconfirm, Table, Tag, Tooltip, Typography } from 'antd'
import dayjs from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import type { Customer } from '../../../types/customer'
import type { Paper } from '../../../types/paper'
import type { ReportAlertRule } from '../types'
import {
  operatorOptions,
  optionLabel,
  processOptions,
  scopeOptions,
  signalOptions,
} from './reportAlertRuleLabels'

interface Props {
  customers: Customer[]
  deleting: boolean
  items: ReportAlertRule[]
  onDelete: (item: ReportAlertRule) => void
  onEdit: (item: ReportAlertRule) => void
  papers: Paper[]
}

export default function ReportAlertRuleList(props: Props) {
  if (props.items.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无阈值规则" />
  }
  return <Table<ReportAlertRule> className="report-alert-rule-table" size="small"
    columns={columns(props)} dataSource={props.items} pagination={false} rowKey="uuid"
    scroll={{ x: 820 }} />
}

function columns(props: Props): ColumnsType<ReportAlertRule> {
  return [
    { title: '规则名称', dataIndex: 'ruleName', width: 170, ellipsis: { showTitle: false },
      render: (value: string) => <Typography.Text ellipsis={{ tooltip: value }}>{value}</Typography.Text> },
    { title: '指标', dataIndex: 'signalCode', width: 112,
      render: (value) => optionLabel(signalOptions, value) },
    { title: '生效范围', width: 176, render: (_, item) => <ScopeCell item={item} {...props} /> },
    { title: '触发阈值', width: 128, align: 'right',
      render: (_, item) => `${optionLabel(operatorOptions, item.comparisonOperator)} ${item.thresholdValue}%` },
    { title: '级别', dataIndex: 'severity', width: 76, align: 'center',
      render: (value: 1 | 2) => <Tag color={value === 2 ? 'error' : 'warning'}>
        {value === 2 ? '严重' : '预警'}
      </Tag> },
    { title: '状态', dataIndex: 'isEnabled', width: 82, align: 'center',
      render: (value: 0 | 1) => <Badge status={value === 1 ? 'processing' : 'default'}
        text={value === 1 ? '启用' : '停用'} /> },
    { title: '更新时间', dataIndex: 'updateTime', width: 140,
      render: (value: string) => dayjs(value).format('YYYY-MM-DD HH:mm') },
    { title: '操作', fixed: 'right', width: 82, align: 'center',
      render: (_, item) => <RuleActions deleting={props.deleting} item={item}
        onDelete={props.onDelete} onEdit={props.onEdit} /> },
  ]
}

function ScopeCell({ customers, item, papers }: Pick<Props, 'customers' | 'papers'> & { item: ReportAlertRule }) {
  const target = scopeTarget(item, customers, papers)
  return <div className="report-alert-rule-scope">
    <strong>{optionLabel(scopeOptions, item.scopeType)}</strong>
    {target && <Typography.Text type="secondary" ellipsis={{ tooltip: target }}>{target}</Typography.Text>}
  </div>
}

function RuleActions(props: Pick<Props, 'deleting' | 'onDelete' | 'onEdit'> & { item: ReportAlertRule }) {
  return <div className="report-alert-rule-actions">
    <Tooltip title="编辑"><Button aria-label={`编辑规则 ${props.item.ruleName}`} type="text"
      icon={<EditOutlined />} onClick={() => props.onEdit(props.item)} /></Tooltip>
    <Popconfirm title="删除此阈值规则？" description="删除后对应范围将回退到下一优先级规则"
      onConfirm={() => props.onDelete(props.item)}>
      <Tooltip title="删除"><Button aria-label={`删除规则 ${props.item.ruleName}`} danger type="text"
        loading={props.deleting} icon={<DeleteOutlined />} /></Tooltip>
    </Popconfirm>
  </div>
}

function scopeTarget(item: ReportAlertRule, customers: Customer[], papers: Paper[]) {
  if (item.scopeType === 2) {
    return customers.find((customer) => customer.uuid === item.customerUuid)?.customerName ?? item.customerUuid
  }
  if (item.scopeType === 3) {
    return papers.find((paper) => paper.uuid === item.paperUuid)?.paperName ?? item.paperUuid
  }
  if (item.scopeType === 4 && item.processType) return optionLabel(processOptions, item.processType)
  return undefined
}
