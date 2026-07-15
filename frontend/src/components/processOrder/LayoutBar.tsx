import { Typography } from 'antd'
import type { RewindLayoutItemDTO } from '../../types/processOrder'
import MesTooltip from '../biz/MesTooltip'
import { formatMm } from '../../utils/numberFormatters'
import './ProcessOrderShared.css'

interface Props {
  layoutItems: RewindLayoutItemDTO[]
  originalWidth: number | undefined
}

export default function LayoutBar({ layoutItems, originalWidth }: Props) {
  if (!originalWidth || originalWidth <= 0 || layoutItems.length === 0) {
    return (
      <Typography.Text className="layout-bar__empty" type="secondary">
        暂无排布数据
      </Typography.Text>
    )
  }

  let start = 0
  const segments = layoutItems.map((item) => {
    const segment = { item, start }
    start += item.width
    return segment
  })

  return (
    <div className="layout-bar">
      {segments.map(({ item, start: segmentStart }) => (
        <LayoutSegment key={`${item.itemType}-${segmentStart}-${item.width}`} item={item} originalWidth={originalWidth} />
      ))}
    </div>
  )
}

function LayoutSegment({ item, originalWidth }: { item: RewindLayoutItemDTO; originalWidth: number }) {
  const pct = (item.width / originalWidth) * 100
  const isFinish = item.itemType === 'FINISH'
  const label = isFinish
    ? `成品 ${formatMm(item.width)} × ${item.quantity ?? 1}`
    : `修边 ${formatMm(item.width)}`

  return (
    <MesTooltip title={label}>
      <div
        className={`layout-bar__segment layout-bar__segment--${isFinish ? 'finish' : 'trim'}`}
        style={{ width: `${pct}%` }}
      >
        {pct > 8 ? item.width : ''}
      </div>
    </MesTooltip>
  )
}
