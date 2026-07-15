import { WarningOutlined } from '@ant-design/icons'
import { Tag, Tooltip, Typography } from 'antd'
import { formatKg } from '../../features/delivery/utils/deliveryFormatters'
import { formatGram, formatMm } from '../../utils/numberFormatters'
import type { AvailableFinishSourceMotherRoll, AvailableFinishVO } from '../../types/delivery'

interface Props {
  finish: AvailableFinishVO
}

export default function DeliveryMotherRollCell({ finish }: Props) {
  const sources = finish.sourceMotherRolls ?? []
  if (finish.sourceType === 2) return <DirectSource finish={finish} sources={sources} />
  if (sources.length === 0) {
    return (
      <div className="delivery-cell-stack mes-cell-stack delivery-source-missing">
        <Tag color="warning" icon={<WarningOutlined />}>关联待补</Tag>
        <span>未找到来源母卷</span>
      </div>
    )
  }
  const singleSource = sources[0]
  if (sources.length === 1 && singleSource) return <SingleSource source={singleSource} />
  return <MultipleSources sources={sources} />
}

function DirectSource({ finish, sources }: Props & { sources: AvailableFinishSourceMotherRoll[] }) {
  const source = sources[0]
  const identity = source ? sourceIdentity(source) : finish.originalRollNos
  const missingIdentity = source
    ? !source.rollNo && !source.extraNo
    : !finish.originalRollNos
  return (
    <div className="delivery-cell-stack mes-cell-stack delivery-source-cell">
      <Typography.Text strong>
        母卷直发 / {missingIdentity ? <MissingIdentityLabel /> : identity}
      </Typography.Text>
      <span className="delivery-source-detail">
        {source ? sourceDetailText(source) : '原纸直接出库'}
      </span>
    </div>
  )
}

function SingleSource({ source }: { source: AvailableFinishSourceMotherRoll }) {
  const missingIdentity = !source.rollNo && !source.extraNo
  return (
    <div className="delivery-cell-stack mes-cell-stack delivery-source-cell">
      <Typography.Text strong>
        {sourceLabel(source)} / {missingIdentity ? <MissingIdentityLabel /> : sourceIdentity(source)}
      </Typography.Text>
      <span className="delivery-source-detail">{sourceDetailText(source)}</span>
    </div>
  )
}

function MultipleSources({ sources }: { sources: AvailableFinishSourceMotherRoll[] }) {
  const details = sources.map(sourceDescription).join('；')
  const missingCount = sources.filter((source) => !source.rollNo && !source.extraNo).length
  return (
    <Tooltip title={details} placement="topLeft">
      <div className="delivery-cell-stack mes-cell-stack delivery-source-cell delivery-source-multiple">
        <Typography.Text strong>
          多母卷（{sources.length} 卷）
          {missingCount > 0 && <MissingIdentityLabel text={`${missingCount} 卷号待补`} />}
        </Typography.Text>
        <span className="delivery-source-detail">{sources.map(sourceShortText).join('、')}</span>
      </div>
    </Tooltip>
  )
}

function MissingIdentityLabel({ text = '未记录卷号' }: { text?: string }) {
  return <span className="delivery-source-identity-missing"><WarningOutlined />{text}</span>
}

function sourceLabel(source: AvailableFinishSourceMotherRoll) {
  return source.rowSort ? `母卷 ${source.rowSort}` : '来源母卷'
}

function sourceIdentity(source: AvailableFinishSourceMotherRoll) {
  if (source.rollNo) return `卷号 ${source.rollNo}`
  if (source.extraNo) return `编号 ${source.extraNo}`
  return '未记录卷号'
}

function sourceShortText(source: AvailableFinishSourceMotherRoll) {
  return `${sourceLabel(source)} / ${source.paperName || '-'} / ${source.gramWeight ? formatGram(source.gramWeight) : '-'}`
}

function sourceDescription(source: AvailableFinishSourceMotherRoll) {
  const allocation = source.allocationWeight ? `，分配 ${formatKg(source.allocationWeight)}` : ''
  return `${sourceLabel(source)}：${sourceIdentity(source)} / ${sourceDetailText(source)}${allocation}`
}

function sourceDetailText(source: AvailableFinishSourceMotherRoll) {
  const parts = [
    source.paperName,
    source.gramWeight ? formatGram(source.gramWeight) : undefined,
    source.originalWidth ? formatMm(source.originalWidth) : undefined,
    source.actualWeight ? formatKg(source.actualWeight) : undefined,
  ].filter(Boolean)
  return parts.length ? parts.join(' / ') : '未记录母卷规格'
}
