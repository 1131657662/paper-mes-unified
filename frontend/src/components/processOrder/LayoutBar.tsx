import { Typography } from 'antd'
import type { RewindLayoutItemDTO } from '../../types/processOrder'
import MesTooltip from '../biz/MesTooltip'
import { formatMm } from '../../utils/numberFormatters'

interface Props {
  layoutItems: RewindLayoutItemDTO[]
  originalWidth: number | undefined
}

const FINISH_COLOR = '#1677ff'
const TRIM_COLOR = '#ff4d4f'

export default function LayoutBar({ layoutItems, originalWidth }: Props) {
  if (!originalWidth || originalWidth <= 0 || layoutItems.length === 0) {
    return (
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        暂无排布数据
      </Typography.Text>
    )
  }

  return (
    <div style={{ display: 'flex', height: 24, borderRadius: 4, overflow: 'hidden', width: '100%', maxWidth: 400, border: '1px solid #d9d9d9' }}>
      {layoutItems.map((item, index) => {
        const pct = (item.width / originalWidth) * 100
        const isFinish = item.itemType === 'FINISH'
        const color = isFinish ? FINISH_COLOR : TRIM_COLOR
        const label = isFinish ? `成品 ${formatMm(item.width)} × ${item.quantity ?? 1}` : `修边 ${formatMm(item.width)}`
        return (
          <MesTooltip key={index} title={label}>
            <div
              style={{
                width: `${pct}%`,
                height: '100%',
                backgroundColor: color,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                fontSize: 10,
                fontWeight: 500,
                minWidth: pct > 0 ? 0 : 0,
                transition: 'background-color 0.2s',
                cursor: 'pointer',
              }}
            >
              {pct > 8 ? `${item.width}` : ''}
            </div>
          </MesTooltip>
        )
      })}
    </div>
  )
}
