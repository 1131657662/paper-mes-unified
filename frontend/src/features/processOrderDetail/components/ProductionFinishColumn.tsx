import { Tag } from 'antd'
import type { FinishGroup } from '../../../components/processOrder/shared/types'
import type { FinishProductionVO, RollProductionVO } from '../../../types/processOrder'
import { formatKg } from '../orderDetailUtils'

interface Props {
  estimateWeight: number
  finishes: FinishProductionVO[]
  finishCount: number
  groups: FinishGroup[]
  production: RollProductionVO
  spareCount: number
}

export default function ProductionFinishColumn({
  estimateWeight,
  finishes,
  finishCount,
  groups,
  spareCount,
}: Props) {
  const rollNos = finishes.filter((f) => f.isSpare !== 1).slice(0, 6)
  const sources = collectSources(finishes)

  return (
    <div>
      <div className="production-roll__head">
        <span className="production-roll__title">成品 {finishCount} 件</span>
        {spareCount > 0 && <Tag color="orange">备用 {spareCount}</Tag>}
      </div>
      <div className="production-roll__line">
        预估重量合计：{formatKg(estimateWeight)}
      </div>
      <div className="production-roll__group production-roll__spaced">
        {groups.map((group) => (
          <span className="production-pill" key={group.width}>{group.width}mm × {group.count}</span>
        ))}
      </div>
      {rollNos.length > 0 && (
        <div className="production-roll__line production-roll__spaced">
          卷号：{rollNos.map((f) => f.finishRollNo).join('、')}
          {finishCount > rollNos.length ? ` 等 ${finishCount} 件` : ''}
        </div>
      )}
      {sources.length > 1 && <SourcePills sources={sources} />}
    </div>
  )
}

function SourcePills({ sources }: { sources: Array<{ key: string; label: string; ratio: number }> }) {
  return (
    <div className="production-roll__group production-roll__spaced">
      {sources.map((source) => (
        <span className="production-pill production-pill--source" key={source.key}>
          {source.label} {source.ratio}%
        </span>
      ))}
    </div>
  )
}

function collectSources(finishes: FinishProductionVO[]) {
  const map = new Map<string, { key: string; label: string; ratio: number }>()
  for (const finish of finishes) {
    for (const source of finish.sources ?? []) {
      if (!source.originalUuid || map.has(source.originalUuid)) continue
      map.set(source.originalUuid, {
        key: source.originalUuid,
        label: source.rollNo || source.paperName || source.originalUuid,
        ratio: source.shareRatio ?? 0,
      })
    }
  }
  return Array.from(map.values())
}
