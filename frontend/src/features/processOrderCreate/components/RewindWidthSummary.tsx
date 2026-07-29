import { Alert, Button, Progress, Space, Tag, Typography } from 'antd'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { RewindLayoutItemPlanDTO, RewindSegmentPlanDTO, WidthDifferencePolicy } from '../../../types/processOrder'
import { formatMm } from '../../../utils/numberFormatters'
import { calcRewindWidthUsage, rewindWidthPolicy } from '../rewindWidthUsage'
import './RewindWidthSummary.css'

interface Props {
  mode: number
  originalWidth?: number
  segment: RewindSegmentPlanDTO
  widthDifferencePolicy: WidthDifferencePolicy
  onFillTrim: () => void
}

export default function RewindWidthSummary({ mode, originalWidth, segment, widthDifferencePolicy, onFillTrim }: Props) {
  const policy = rewindWidthPolicy(mode)
  if (!policy.enabled) return <ModeNote note={policy.note} />

  const usage = calcRewindWidthUsage(segment, originalWidth)
  const overflow = usage.remainingWidth < 0
  const complete = usage.originalWidth > 0 && usage.remainingWidth === 0

  return (
    <div className="rewind-width-summary">
      <Space wrap size={8}>
        <Tag color="blue">成品 {formatMm(usage.finishWidth)} / {usage.finishCount}件</Tag>
        <Tag color={usage.trimWidth > 0 ? 'orange' : 'default'}>修边 {formatMm(usage.trimWidth)}</Tag>
        {usage.remainingWidth > 0 && <Tag color="gold">未分配 {formatMm(usage.remainingWidth)}</Tag>}
        <Typography.Text type={overflow ? 'danger' : 'secondary'}>
          门幅 {usage.usedWidth}/{usage.originalWidth || '-'} mm
          {overflow ? `，超出 ${formatMm(Math.abs(usage.remainingWidth))}` : `，剩余 ${formatMm(Math.max(0, usage.remainingWidth))}`}
        </Typography.Text>
        {widthDifferencePolicy === 'REMAINDER' && (
          <Button size="small" disabled={usage.remainingWidth <= 0} onClick={onFillTrim}>剩余转余料</Button>
        )}
      </Space>
      {usage.originalWidth > 0 && (
        <Progress
          percent={usage.usedPercent}
          size="small"
          status={overflow ? 'exception' : complete ? 'success' : 'active'}
        />
      )}
      <LayoutStrip items={segment.layoutItems ?? []} originalWidth={usage.originalWidth} />
      {widthDifferencePolicy === 'REMAINDER' && usage.remainingWidth > 0 && (
        <Alert showIcon type="warning" message={`还有 ${formatMm(usage.remainingWidth)} 未分配`}
          description="留余料要求每个分段都与母卷门幅闭合，请补齐余料后再保存。" />
      )}
      {widthDifferencePolicy !== 'REMAINDER' && usage.remainingWidth > 0 && (
        <Typography.Text type="secondary" className="rewind-width-summary__note">
          {widthDifferencePolicy === 'ALLOCATE'
            ? `未分配的 ${formatMm(usage.remainingWidth)} 对应重量将均匀分摊到本段成品和余料。`
            : `未分配的 ${formatMm(usage.remainingWidth)} 将计入计划损耗，不生成库存。`}
        </Typography.Text>
      )}
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
  return `${type} ${formatMm(item.width)} × ${item.quantity ?? 1}`
}
