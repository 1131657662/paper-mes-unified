import Empty from 'antd/es/empty'
import TooltipText from '../../../components/biz/TooltipText'

export interface ReportBarItem {
  key: string
  label: string
  meta: string
  value: number
  valueText: string
}

interface Props {
  emptyText: string
  items: ReportBarItem[]
}

export default function ReportBarList({ emptyText, items }: Props) {
  const max = Math.max(...items.map((item) => item.value), 0)

  return (
    <div className="report-bars">
      {items.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyText} />
      ) : (
        items.map((item, index) => (
          <div className="report-bar-row" key={item.key}>
            <span className="report-bar-row__index">{index + 1}</span>
            <div className="report-bar-row__label">
              <strong><TooltipText value={item.label} /></strong>
              <span>{item.meta}</span>
            </div>
            <div className="report-bar-row__track" aria-hidden="true">
              <i style={{ width: `${barWidth(item.value, max)}%` }} />
            </div>
            <b>{item.valueText}</b>
          </div>
        ))
      )}
    </div>
  )
}

function barWidth(value: number, max: number) {
  if (max <= 0) return 0
  return Math.max(5, (value / max) * 100)
}
