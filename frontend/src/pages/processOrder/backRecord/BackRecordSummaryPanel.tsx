import { Alert, Tag, Typography } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { formatKg } from '../../../utils/numberFormatters'
import type { BackRecordFormValues } from './backRecordUtils'
import { buildBackRecordMetrics } from './backRecordMetrics'

interface Props {
  detail: ProcessOrderDetailVO | null
  values: BackRecordFormValues
}

export default function BackRecordSummaryPanel({ detail, values }: Props) {
  const metrics = buildBackRecordMetrics(detail, values)
  const missingCount = metrics.missingRollWeight + metrics.missingOfficialFinishWeight
    + metrics.missingOnSiteFinishWidth + metrics.missingTrimData

  return (
    <div className="back-record-summary">
      <div className="back-record-summary__metrics">
        <Metric label="原纸复称" value={metrics.originalWeightPending ? '待称重' : formatKg(metrics.originalActualTotal)} />
        <Metric label="成品实重" value={formatKg(metrics.finishActualTotal)} />
        <Metric label="余料实重" value={formatKg(metrics.trimActualTotal)} />
        <Metric label="工序损耗" value={formatKg(metrics.lossTotal)} />
        <Metric label="报废重量" value={formatKg(metrics.scrapTotal)} />
        <Metric label="直发卷" value={`${metrics.directShipCount} 卷`} />
        {metrics.serviceOnlyCount > 0 && <Metric label="整理卷" value={`${metrics.serviceOnlyCount} 卷`} />}
        <StatusTag missingCount={missingCount} />
      </div>
      {missingCount > 0 && (
        <Alert
          showIcon
          type="warning"
          className="back-record-summary__alert"
          message={`还有 ${missingCount} 项关键数据未填写，提交前需要补齐门幅、重量或确认备用号未使用。`}
        />
      )}
      {metrics.optionalPendingRollWeight > 0 && (
        <Alert
          showIcon
          type="info"
          className="back-record-summary__alert"
          message={`${metrics.optionalPendingRollWeight} 卷母卷尚未复称；当前计价模式不依赖重量，完成后闭合状态将记为“未核验”。`}
        />
      )}
      <Typography.Text type="secondary" className="back-record-summary__hint">
        已复称母卷按实际重量逐卷校验倒挤尾差；非吨位计价且未复称的母卷不会伪造闭合结论。
      </Typography.Text>
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="back-record-summary-metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function StatusTag({ missingCount }: { missingCount: number }) {
  if (missingCount > 0) return <Tag color="warning">待补数据</Tag>
  return <Tag color="success">可提交校验</Tag>
}
