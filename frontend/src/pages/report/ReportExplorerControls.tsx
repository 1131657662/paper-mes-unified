import { Select, Tag } from 'antd'
import type { ReportDimension, ReportMetricItemVO } from '../../types/report'
import { explorerDimensions, metricLabel } from './reportExplorerModel'

interface Props {
  dimension: ReportDimension
  metricCodes: string[]
  metrics: ReportMetricItemVO[]
  onChange: (dimension: ReportDimension, metrics: string[]) => void
}

export default function ReportExplorerControls(props: Props) {
  return <div className="report-explorer-controls">
    <div><span className="report-explorer-controls__label">分组维度</span>
      <Select aria-label="分组维度" value={props.dimension} options={explorerDimensions}
        onChange={(value) => props.onChange(value, props.metricCodes)} /></div>
    <div className="report-explorer-controls__metrics"><span className="report-explorer-controls__label">展示指标</span>
      <Select aria-label="展示指标" mode="multiple" maxCount={8} maxTagCount="responsive" value={props.metricCodes}
        options={props.metrics.map((metric) => ({ value: metric.metricCode, label: metricLabel(metric) }))}
        onChange={(values) => props.onChange(props.dimension, values)} /></div>
    <Tag color="default">最多 8 项</Tag>
  </div>
}
