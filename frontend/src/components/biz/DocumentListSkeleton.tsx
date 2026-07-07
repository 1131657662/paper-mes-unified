import './DocumentListSkeleton.css'

const COLUMN_WIDTHS = [
  { key: 'document', width: '18%' },
  { key: 'customer', width: '16%' },
  { key: 'date', width: '14%' },
  { key: 'amount', width: '12%' },
  { key: 'status', width: '11%' },
  { key: 'actions', width: '13%' },
]

const ROW_KEYS = ['row-a', 'row-b', 'row-c', 'row-d', 'row-e', 'row-f']

export default function DocumentListSkeleton() {
  return (
    <div className="document-list-skeleton" aria-hidden="true">
      <div className="document-list-skeleton__head">
        {COLUMN_WIDTHS.map((column) => (
          <span key={column.key} style={{ width: column.width }} />
        ))}
      </div>
      <div className="document-list-skeleton__body">
        {ROW_KEYS.map((rowKey) => (
          <div className="document-list-skeleton__row" key={rowKey}>
            {COLUMN_WIDTHS.map((column) => (
              <span key={`${rowKey}-${column.key}`} style={{ width: column.width }} />
            ))}
          </div>
        ))}
      </div>
      <div className="document-list-skeleton__footer">
        <span />
        <span />
      </div>
    </div>
  )
}
