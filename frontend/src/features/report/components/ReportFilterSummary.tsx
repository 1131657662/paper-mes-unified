import { Tag } from 'antd'
import type { Customer } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type { Paper } from '../../../types/paper'
import type { ReportQuery } from '../../../types/report'
import type { ReportOperationalTopicCode } from '../../../types/reportOperational'

interface Props {
  customers: Customer[]
  machines: Machine[]
  papers: Paper[]
  query: ReportQuery
  mode?: 'overview' | 'production' | 'quality-loss' | 'explorer' | ReportOperationalTopicCode
}

export default function ReportFilterSummary(props: Props) {
  const tags = buildTags(props)

  return (
    <div className="report-filter-summary">
      <span>当前口径</span>
      <div aria-label={tags.join('；')} title={tags.join('；')}>
        {tags.map((tag) => (
          <Tag key={tag}>{tag}</Tag>
        ))}
      </div>
    </div>
  )
}

function buildTags({ customers, machines, mode = 'overview', papers, query }: Props) {
  if (isOperationalMode(mode)) return operationalTags(mode, customers, papers, query)
  const common = [
    `归属日期：${query.dateFrom ?? '-'} 至 ${query.dateTo ?? '-'}`,
    `状态：${statusText(query.orderStatus)}`,
    labelByUuid('客户', query.customerUuid, customers, 'customerName'),
    `产品：${paperText(query.paperName, papers)}`,
    labelByUuid('机台', query.machineUuid, machines, 'machineName'),
    `工艺：${stepText(query.mainStepType)}`,
    `方式：${modeText(query.processMode)}`,
    '归属规则：优先使用回录完成日期，历史数据缺失时回退制单日期',
  ]
  if (mode !== 'overview') return common.filter(Boolean)
  return [...common, '金额：应收按加工单归属日期统计，已收仅统计有效收款',
    `结算：${settleText(query.settleType)}`, `开票：${invoiceText(query.isInvoice)}`].filter(Boolean)
}

function operationalTags(mode: ReportOperationalTopicCode, customers: Customer[], papers: Paper[], query: ReportQuery) {
  const tags = [
    `${operationalDateLabels[mode]}：${query.dateFrom ?? '-'} 至 ${query.dateTo ?? '-'}`,
    labelByUuid('客户', query.customerUuid, customers, 'customerName'),
  ]
  if (mode === 'inventory') tags.push(`产品：${paperText(query.paperName, papers)}`)
  if (mode === 'settlement' || mode === 'collection') {
    tags.push(`结算：${settleText(query.settleType)}`, `开票：${invoiceText(query.isInvoice)}`)
  }
  return tags
}

function isOperationalMode(mode: Props['mode']): mode is ReportOperationalTopicCode {
  return mode === 'settlement' || mode === 'collection' || mode === 'inventory' || mode === 'delivery'
}

function labelByUuid<T extends { uuid: string }>(
  label: string,
  uuid: string | undefined,
  list: T[],
  key: keyof T,
) {
  if (!uuid) return `${label}：全部`
  const item = list.find((record) => record.uuid === uuid)
  return `${label}：${String(item?.[key] ?? uuid)}`
}

function paperText(value: string | undefined, papers: Paper[]) {
  if (!value) return '全部'
  return papers.find((item) => item.paperName === value)?.paperName ?? value
}

function statusText(value?: number) {
  if (value == null) return '默认完成及已结算'
  return statusMap[value] ?? String(value)
}

function stepText(value?: number) {
  if (value == null) return '全部'
  return value === 1 ? '锯纸' : value === 2 ? '复卷' : String(value)
}

function modeText(value?: number) {
  if (value == null) return '全部'
  return value === 1 ? '标准加工' : value === 2 ? '现场定尺' : value === 3 ? '直发' : String(value)
}

function settleText(value?: number) {
  if (value == null) return '全部'
  return value === 1 ? '次结' : value === 2 ? '月结' : String(value)
}

function invoiceText(value?: number) {
  if (value == null) return '全部'
  return value === 1 ? '开票' : '不开票'
}

const statusMap: Record<number, string> = {
  0: '草稿',
  1: '待下发',
  2: '加工中',
  3: '待回录',
  4: '已完成',
  5: '已结算',
  6: '已作废',
}

const operationalDateLabels: Record<ReportOperationalTopicCode, string> = {
  settlement: '结算日期', collection: '到账日期', inventory: '入库日期', delivery: '出库日期',
}
