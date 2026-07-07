import { Button, Progress, Space, Tag, Typography } from 'antd'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { RewindLayoutItemPlanDTO, RewindSegmentPlanDTO } from '../../../types/processOrder'
import { calcRewindWidthUsage, rewindWidthPolicy } from '../rewindWidthUsage'
import './RewindWidthSummary.css'

interface Props {
  mode: number
  originalWidth?: number
  segment: RewindSegmentPlanDTO
  onFillTrim: () => void
}

export default function RewindWidthSummary({ mode, originalWidth, segment, onFillTrim }: Props) {
  const policy = rewindWidthPolicy(mode)
  if (!policy.enabled) return <ModeNote note={policy.note} />

  const usage = calcRewindWidthUsage(segment, originalWidth)
  const overflow = usage.remainingWidth < 0
  const complete = usage.originalWidth > 0 && usage.remainingWidth === 0

  return (
    <div className="rewind-width-summary">
      <Space wrap size={8}>
        <Tag color="blue">成品 {usage.finishWidth}mm / {usage.finishCount}件</Tag>
        <Tag color={usage.trimWidth > 0 ? 'orange' : 'default'}>修边 {usage.trimWidth}mm</Tag>
        {usage.implicitTrimWidth > 0 && <Tag color="gold">可转修边 {usage.implicitTrimWidth}mm</Tag>}
        <Typography.Text type={overflow ? 'danger' : 'secondary'}>
          门幅 {usage.usedWidth}/{usage.originalWidth || '-'}mm
          {overflow ? `，超出 ${Math.abs(usage.remainingWidth)}mm` : `，剩余 ${Math.max(0, usage.remainingWidth)}mm`}
        </Typography.Text>
        <Button size="small" disabled={usage.remainingWidth <= 0} onClick={onFillTrim}>
          剩余转修边
        </Button>
      </Space>
      {usage.originalWidth > 0 && (
        <Progress
          percent={usage.usedPercent}
          size="small"
          status={overflow ? 'exception' : complete ? 'success' : 'active'}
        />
      )}
      <LayoutStrip items={segment.layoutItems ?? []} originalWidth={usage.originalWidth} />
      <Typography.Text type="secondary" className="rewind-width-summary__note">
        {policy.note}
      </Typography.Text>
    </div>
  )
}

function ModeNote({ note }: { note: string }) {
  return <Typography.Text type="secondary" className="rewind-width-summary__note">{note}</Typography.Text>
}

function LayoutStrip({ items, originalWidth }: { items: RewindLayoutItemPlanDTO[]; originalWidth: number }) {
  if (!originalWidth || !items.length) return null
  return (
    <div className="rewind-width-summary__strip">
      {items.map((item, index) => (
        <MesTooltip key={`${item.width}-${index}`} title={labelForItem(item)}>
          <div
            className={item.itemType === 'TRIM' ? 'rewind-width-summary__strip-item--trim' : 'rewind-width-summary__strip-item'}
            style={{ width: `${Math.max(0, (item.width * (item.quantity ?? 1) / originalWidth) * 100)}%` }}
          >
            {item.width * (item.quantity ?? 1) / originalWidth > 0.08 ? item.width : ''}
          </div>
        </MesTooltip>
      ))}
    </div>
  )
}

function labelForItem(item: RewindLayoutItemPlanDTO) {
  const type = item.itemType === 'TRIM' ? '修边' : '成品'
  return `${type} ${item.width}mm × ${item.quantity ?? 1}`
}
