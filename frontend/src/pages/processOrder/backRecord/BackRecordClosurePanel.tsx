import { Alert, Progress, Tag, Typography } from 'antd'
import type { BackRecordFormValues } from './backRecordUtils'
import { buildWorkItemMetrics } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import { formatOptionalKg } from '../../../utils/numberFormatters'

interface Props {
  item: BackRecordWorkItem
  items: BackRecordWorkItem[]
  values: BackRecordFormValues
}

export default function BackRecordClosurePanel({ item, items, values }: Props) {
  const active = buildWorkItemMetrics(item, values)
  const totalMissing = items.reduce((sum, current) => {
    const metrics = buildWorkItemMetrics(current, values)
    return sum + (metrics.missingRoll ? 1 : 0) + metrics.missingFinishes + metrics.missingFinishWidths
  }, 0)

  return (
    <aside className="back-record-close">
      <Typography.Text strong>闭合预览</Typography.Text>
      <div className="back-record-close__numbers">
        <Metric label="原纸复称" value={formatKg(active.rollActual)} />
        <Metric label="成品实重" value={formatKg(active.productActual)} />
        <Metric label="余料实重" value={formatKg(active.trimActual)} />
        <Metric label="工序损耗" value={formatKg(active.loss)} />
        <Metric label="报废重量" value={formatKg(active.scrap)} />
        <Metric label="倒挤尾差" value={formatOptionalKg(active.diff)} />
      </div>
      <Progress
        percent={active.diffRatio == null ? 0 : Math.min(100, active.diffRatio * 100)}
        showInfo={false}
        status={progressStatus(active.diffRatio)}
      />
      <Typography.Text type="secondary" className="back-record-close__hint">
        倒挤尾差 = 原纸复称 - 成品实重 - 工序损耗 - 报废重量。
      </Typography.Text>
      <div className="back-record-close__tags">
        {active.missingRoll && <Tag color="warning">母卷复称未填</Tag>}
        {active.missingFinishes > 0 && <Tag color="warning">成品 {active.missingFinishes} 项未填</Tag>}
        {active.missingFinishWidths > 0 && <Tag color="warning">门幅 {active.missingFinishWidths} 项未填</Tag>}
        {!active.missingRoll && active.missingFinishes === 0 && active.missingFinishWidths === 0 && <Tag color="success">当前项已填完</Tag>}
      </div>
      <Alert
        showIcon
        type={item.sourceMode === 'linked' ? 'info' : 'warning'}
        message={closureMessage(item)}
      />
      <div className="back-record-close__footer">
        <span>整单待补</span>
        <strong>{totalMissing}</strong>
      </div>
    </aside>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="back-record-close-metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function closureMessage(item: BackRecordWorkItem) {
  if (item.kind === 'pool') return '成品池没有来源母卷，前端不做逐卷闭合，只提交实重给后端。'
  if (item.sourceMode === 'linked') return '当前成品已有来源母卷，后端会按该关系执行逐卷闭合与倒挤尾差校验。'
  if (item.sourceMode === 'inferred') return '当前归属为辅助匹配，建议补齐来源关系后再作为正式闭合依据。'
  return '当前母卷没有成品明细，可能是直发、未配置或旧数据缺失。'
}

function progressStatus(diffRatio?: number) {
  if (diffRatio == null || diffRatio <= 0.02) return 'success'
  if (diffRatio <= 0.05) return 'normal'
  return 'exception'
}

function formatKg(value?: number) {
  return formatOptionalKg(value)
}
