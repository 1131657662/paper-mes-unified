import { Alert, Tag, Typography } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { formatKg } from '../../../utils/numberFormatters'
import type { BackRecordFormValues } from './backRecordUtils'
import { buildBackRecordMetrics } from './backRecordUtils'

interface Props {
  detail: ProcessOrderDetailVO | null
  values: BackRecordFormValues
}

export default function BackRecordSummaryPanel({ detail, values }: Props) {
  const metrics = buildBackRecordMetrics(detail, values)
  const missingCount = metrics.missingRollWeight + metrics.missingOfficialFinishWeight

  return (
    <div className="back-record-summary">
      <div className="back-record-summary__metrics">
        <Metric label="原纸复称" value={formatKg(metrics.originalActualTotal)} />
        <Metric label="成品实重" value={formatKg(metrics.finishActualTotal)} />
        <Metric label="工序损耗" value={formatKg(metrics.lossTotal)} />
        <Metric label="报废重量" value={formatKg(metrics.scrapTotal)} />
        <Metric label="直发卷" value={`${metrics.directShipCount} 卷`} />
        <StatusTag missingCount={missingCount} />
      </div>
      {missingCount > 0 && (
        <Alert
          showIcon
          type="warning"
          className="back-record-summary__alert"
          message={`还有 ${missingCount} 项关键重量未填写，提交前需要补齐或确认备用号未使用。`}
        />
      )}
      <Typography.Text type="secondary" className="back-record-summary__hint">
        闭合以原纸复称重量为基准，后端会按母卷逐卷校验倒挤尾差；偏差超过 5% 需要授权放行。
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
  if (missingCount > 0) return <Tag color="warning">待补重量</Tag>
  return <Tag color="success">可提交校验</Tag>
}
