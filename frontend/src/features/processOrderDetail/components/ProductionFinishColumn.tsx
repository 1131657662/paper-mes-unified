import { Tag } from 'antd'
import {
  calcTrimWidth,
  isDeliverableProductionFinish,
  trimFinishes,
  trimWeightFromFinishes,
} from '../../../components/processOrder/shared/detailHelpers'
import { buildFinishLayers } from '../../../components/processOrder/shared/layeredRewindView'
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
  production,
  spareCount,
}: Props) {
  const deliverableFinishes = finishes.filter(isDeliverableProductionFinish)
  const rollNos = deliverableFinishes.slice(0, 6)
  const trimRows = trimFinishes(finishes)
  const trimWeight = trimWeightFromFinishes(finishes)
  const fallbackTrimWidth = trimRows.length ? 0 : calcTrimWidth(production)
  const sources = collectSources(deliverableFinishes)
  const layers = buildFinishLayers(production, finishes)

  return (
    <div>
      <div className="production-roll__head">
        <span className="production-roll__title">成品 {finishCount} 件</span>
        {spareCount > 0 && <Tag color="orange">备用 {spareCount}</Tag>}
      </div>
      <div className="production-roll__line">
        预估重量合计：{formatKg(estimateWeight)}
      </div>
      {layers.length > 0
        ? <LayerPills layers={layers} />
        : (
          <div className="production-roll__group production-roll__spaced">
            {groups.map((group) => (
              <span className="production-pill" key={group.width}>{group.width}mm × {group.count}</span>
            ))}
          </div>
        )}
      {rollNos.length > 0 && (
        <div className="production-roll__line production-roll__spaced">
          卷号：{rollNos.map((f) => f.finishRollNo).join('、')}
          {finishCount > rollNos.length ? ` 等 ${finishCount} 件` : ''}
        </div>
      )}
      {(trimRows.length > 0 || fallbackTrimWidth > 0 || trimWeight > 0) && (
        <div className="production-roll__group production-roll__spaced">
          {trimRows.length > 0
            ? trimRows.map((finish) => (
              <span className="production-pill production-pill--trim" key={finish.uuid}>
                修边 {finish.finishWidth ? `${finish.finishWidth}mm` : '-'}
                {trimWeight > 0 && trimRows.length === 1 ? ` / ${formatKg(trimWeight)}` : ''}
              </span>
            ))
            : (
              <span className="production-pill production-pill--trim">
                修边 {fallbackTrimWidth > 0 ? `${fallbackTrimWidth}mm` : '-'}
                {trimWeight > 0 ? ` / ${formatKg(trimWeight)}` : ''}
              </span>
            )}
        </div>
      )}
      {trimRows.length > 1 && trimWeight > 0 && (
        <div className="production-roll__line">修边重量合计：{formatKg(trimWeight)}</div>
      )}
      {sources.length > 1 && <SourcePills sources={sources} />}
    </div>
  )
}

function LayerPills({ layers }: { layers: ReturnType<typeof buildFinishLayers> }) {
  return (
    <div className="production-roll__group production-roll__spaced">
      {layers.map((layer) => (
        <span className="production-pill production-pill--layer" key={layer.key}>
          {layer.summary}
        </span>
      ))}
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
