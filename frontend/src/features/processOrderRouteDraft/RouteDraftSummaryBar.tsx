import { Statistic, Tag } from 'antd'
import type { OriginalRoll, ProcessRoutePreviewVO } from '../../types/processOrder'
import { formatKg, formatMoney, formatNumber, formatTon } from '../../utils/numberFormatters'
import { routeTotals } from './routeDraftModel'
import type { RouteDraftStage } from './routeDraftModel'

interface Props {
  preview?: ProcessRoutePreviewVO
  roll: OriginalRoll
  stages: RouteDraftStage[]
}

export default function RouteDraftSummaryBar({ preview, roll, stages }: Props) {
  const totals = routeTotals(roll, stages)

  return (
    <div className="route-draft-summary">
      <Statistic title="工序数" value={formatNumber(totals.stageCount)} suffix="道" />
      <Statistic title="最终成品" value={formatNumber(totals.finishCount)} suffix="件" />
      <Statistic title="预估重量" value={formatKg(totals.finishWeight)} />
      <Statistic title="锯纸刀数" value={formatNumber(totals.knifeCount)} suffix="刀" />
      <Statistic title="复卷吨位" value={formatTon(totals.rewindWeight / 1000)} />
      <Statistic
        title={preview ? <span>加工费 <Tag color="success">已校验</Tag></span> : '加工费'}
        value={preview ? formatMoney(preview.totalAmount) : '待校验'}
      />
    </div>
  )
}
