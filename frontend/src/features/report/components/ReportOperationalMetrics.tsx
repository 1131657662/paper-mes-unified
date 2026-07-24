import { BankOutlined, CheckCircleOutlined, InboxOutlined, LockOutlined,
  ReconciliationOutlined, WarningOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'
import type { ReportOperationalAnalysisVO } from '../../../types/reportOperational'
import { formatMoney, formatNumber, formatTonFromKg } from '../utils/reportFormatters'

export default function ReportOperationalMetrics({ analysis }: { analysis: ReportOperationalAnalysisVO }) {
  return <section className="report-topic-metrics" aria-label="关键指标">
    {metrics(analysis).map((item) => <article
      className={'report-topic-metric report-topic-metric--' + item.tone} key={item.label}
      title={`${item.label}：${item.value}，${item.hint}`}>
      <span className="report-topic-metric__icon">{item.icon}</span>
      <div><span>{item.label}</span><strong>{item.value}</strong><small>{item.hint}</small></div>
    </article>)}
  </section>
}

function metrics(value: ReportOperationalAnalysisVO): Metric[] {
  if (value.topicCode === 'settlement') return settlementMetrics(value.overview)
  if (value.topicCode === 'collection') return collectionMetrics(value.overview)
  if (value.topicCode === 'inventory') return inventoryMetrics(value.overview)
  return deliveryMetrics(value.overview)
}

function settlementMetrics(v: Extract<ReportOperationalAnalysisVO, { topicCode: 'settlement' }>['overview']): Metric[] {
  return [
    metric('有效结算单', formatNumber(v.totalDocuments), '不含作废单据', <ReconciliationOutlined />),
    metric('应收金额', formatMoney(v.totalAmount), '结算单确认口径', <BankOutlined />, 'primary'),
    metric('已结清金额', formatMoney(v.receivedAmount), '含现金、抵扣与核销', <CheckCircleOutlined />),
    metric('未收余额', formatMoney(v.unreceivedAmount), '待收 ' + v.pendingDocuments + ' / 部分 ' + v.partialDocuments,
      <WarningOutlined />, 'warning'),
    metric('逾期金额', formatMoney(v.overdueAmount), v.overdueDocuments + ' 张逾期单据',
      <WarningOutlined />, 'danger'),
  ]
}

function collectionMetrics(v: Extract<ReportOperationalAnalysisVO, { topicCode: 'collection' }>['overview']): Metric[] {
  return [
    metric('有效回款流水', formatNumber(v.recordCount), '不含已撤销记录', <ReconciliationOutlined />),
    metric('结清金额', formatMoney(v.settledAmount), '现金 + 抵扣 + 核销', <CheckCircleOutlined />, 'primary'),
    metric('现金到账', formatMoney(v.cashAmount), v.cashRecordCount + ' 笔含现金', <BankOutlined />),
    metric('废纸抵扣', formatMoney(v.scrapOffsetAmount), formatTonFromKg(v.scrapWeight),
      <InboxOutlined />, 'warning'),
    metric('优惠核销', formatMoney(v.discountAmount), v.discountRecordCount + ' 笔含优惠',
      <WarningOutlined />, 'danger'),
  ]
}

function inventoryMetrics(v: Extract<ReportOperationalAnalysisVO, { topicCode: 'inventory' }>['overview']): Metric[] {
  return [
    metric('当前库存', formatNumber(v.rollCount) + ' 卷', formatTonFromKg(v.totalWeight), <InboxOutlined />, 'primary'),
    metric('可用库存', formatNumber(v.availableRollCount) + ' 卷', '未被待出库单占用', <CheckCircleOutlined />),
    metric('锁定库存', formatNumber(v.lockedRollCount) + ' 卷', formatTonFromKg(v.lockedWeight),
      <LockOutlined />, 'warning'),
    metric('库存净可用', formatTonFromKg(v.totalWeight - v.lockedWeight), '当前重量 - 锁定重量', <InboxOutlined />),
    metric('异常待处理', formatNumber(v.exceptionRollCount) + ' 卷', '缺仓库/入库时间或异常',
      <WarningOutlined />, 'danger'),
  ]
}

function deliveryMetrics(v: Extract<ReportOperationalAnalysisVO, { topicCode: 'delivery' }>['overview']): Metric[] {
  return [
    metric('有效出库单', formatNumber(v.documentCount), formatNumber(v.rollCount) + ' 卷', <ReconciliationOutlined />),
    metric('计划出库重量', formatTonFromKg(v.totalWeight), '待出库 + 已签收', <InboxOutlined />, 'primary'),
    metric('待出库', formatTonFromKg(v.pendingWeight), v.pendingDocuments + ' 张单据', <LockOutlined />, 'warning'),
    metric('已出库签收', formatTonFromKg(v.completedWeight), v.completedDocuments + ' 张单据', <CheckCircleOutlined />),
    metric('完成率', percentage(v.completedWeight, v.totalWeight), '按重量计算', <CheckCircleOutlined />, 'primary'),
  ]
}

function percentage(value: number, total: number) {
  return total > 0 ? ((value / total) * 100).toFixed(1) + '%' : '0.0%'
}

function metric(label: string, value: string, hint: string, icon: ReactNode,
  tone: Metric['tone'] = 'default'): Metric { return { label, value, hint, icon, tone } }

interface Metric { label: string; value: string; hint: string; icon: ReactNode;
  tone: 'danger' | 'default' | 'primary' | 'warning' }
