interface BarItem {
  key: string
  label: string
  meta: string
  value: number
  valueText: string
}

interface Props {
  emptyText: string
  items: BarItem[]
  title: string
}

export default function ReportBarList({ emptyText, items, title }: Props) {
  const max = Math.max(...items.map((item) => item.value), 0)

  return (
    <section className="report-panel">
      <div className="report-panel__head">
        <div>
          <h3>{title}</h3>
        </div>
      </div>
      <div className="report-bars">
        {items.length === 0 ? (
          <div className="report-empty mes-empty">{emptyText}</div>
        ) : (
          items.map((item) => (
            <div className="report-bar-row" key={item.key}>
              <div className="report-bar-row__label">
                <strong>{item.label}</strong>
                <span>{item.meta}</span>
              </div>
              <div className="report-bar-row__track">
                <i style={{ width: `${max > 0 ? Math.max(6, (item.value / max) * 100) : 0}%` }} />
              </div>
              <b>{item.valueText}</b>
            </div>
          ))
        )}
      </div>
    </section>
  )
}
