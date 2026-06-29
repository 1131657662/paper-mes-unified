interface QueueOption {
  label: string
  value: QueueStatus
}

interface Props {
  value: QueueStatus
  onChange: (value: QueueStatus) => void
}

export type QueueStatus = 'all' | '0' | '1' | '2' | '3' | '4'

const queueOptions: QueueOption[] = [
  { label: '全部', value: 'all' },
  { label: '草稿', value: '0' },
  { label: '待下发', value: '1' },
  { label: '加工中', value: '2' },
  { label: '待回录', value: '3' },
  { label: '已完成', value: '4' },
]

export default function ProcessOrderQueueBar({ value, onChange }: Props) {
  return (
    <div className="process-order-queue">
      <div className="process-order-queue__tabs">
        {queueOptions.map((option) => (
          <button
            key={option.value}
            type="button"
            className={option.value === value ? 'is-active' : undefined}
            onClick={() => onChange(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  )
}
