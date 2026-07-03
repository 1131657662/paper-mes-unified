import { Tag } from 'antd'
import type { Customer } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type { Paper } from '../../../types/paper'
import type { ReportQuery } from '../../../types/report'

interface Props {
  customers: Customer[]
  machines: Machine[]
  papers: Paper[]
  query: ReportQuery
}

export default function ReportFilterSummary({ customers, machines, papers, query }: Props) {
  const tags = buildTags({ customers, machines, papers, query })

  return (
    <div className="report-filter-summary">
      <span>当前口径</span>
      <div>
        {tags.map((tag) => (
          <Tag key={tag}>{tag}</Tag>
        ))}
      </div>
    </div>
  )
}

function buildTags({ customers, machines, papers, query }: Props) {
  return [
    `制单日期：${query.dateFrom ?? '-'} 至 ${query.dateTo ?? '-'}`,
    '金额：应收按加工单制单日期归属，已收仅统计有效收款',
    `状态：${statusText(query.orderStatus)}`,
    labelByUuid('客户', query.customerUuid, customers, 'customerName'),
    labelByUuid('机台', query.machineUuid, machines, 'machineName'),
    `产品：${paperText(query.paperName, papers)}`,
    `工艺：${stepText(query.mainStepType)}`,
    `方式：${modeText(query.processMode)}`,
    `结算：${settleText(query.settleType)}`,
    `开票：${invoiceText(query.isInvoice)}`,
  ].filter(Boolean)
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
