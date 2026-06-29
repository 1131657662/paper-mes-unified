import { Space, Tag } from 'antd'
import type { DisplayRow } from '../../../components/processOrder/shared/types'
import {
  buildConditionText,
  buildLayoutText,
  calcTrimWidth,
  fmt,
  groupFinishes,
  rewindModeLabel,
} from '../../../components/processOrder/shared/detailHelpers'
import type { FinishProductionVO, RollProductionVO } from '../../../types/processOrder'
import { PROCESS_MODE, ROLL_STATUS } from '../../../constants/processOrder'
import { formatKg } from '../orderDetailUtils'

interface Props {
  row: DisplayRow
}

export default function ProductionRollCard({ row }: Props) {
  const trimWidth = calcTrimWidth(row.mainProduction)
  const trimWeight = sumFinishValue(row.finishes, 'trimWeightShare')
  const spareCount = row.finishes.filter((f) => f.isSpare === 1).length
  const finishCount = row.finishes.filter((f) => f.isSpare !== 1).length

  return (
    <div className="production-roll">
      <RollSourceColumn row={row} />
      <PlanColumn row={row} trimWidth={trimWidth} trimWeight={trimWeight} />
      <FinishColumn
        groups={groupFinishes(row.finishes)}
        finishes={row.finishes}
        finishCount={finishCount}
        spareCount={spareCount}
      />
    </div>
  )
}

function RollSourceColumn({ row }: Props) {
  const production = row.mainProduction
  const title = row.isMergeGroup ? `合并复卷 ${row.rollProductions.length} 卷` : rollName(production, row.seq)

  return (
    <div>
      <div className="production-roll__head">
        <span className="production-roll__title">{title}</span>
        {row.isMergeGroup && <Tag color="geekblue">多母卷</Tag>}
        <Tag>{ROLL_STATUS[production.rollStatus ?? 1] ?? '-'}</Tag>
      </div>
      <div className="production-roll__line">
        {production.paperName || '-'} / {fmt(production.gramWeight, 'g')} / {fmt(production.originalWidth, 'mm')}
      </div>
      <div className="production-roll__line">
        来料 {formatKg((production.rollWeight ?? 0) * (production.pieceNum ?? 1))}
      </div>
      {row.isMergeGroup && (
        <div className="production-roll__group production-roll__spaced">
          {row.rollProductions.map((source, index) => (
            <span className="production-pill production-pill--source" key={source.originalUuid ?? index}>
              {rollName(source, index + 1)}
            </span>
          ))}
        </div>
      )}
    </div>
  )
}

function PlanColumn({ row, trimWidth, trimWeight }: Props & { trimWidth: number; trimWeight: number }) {
  const production = row.mainProduction
  const isRewind = production.mainStepType === 2

  if (row.isDirectShip) {
    return (
      <div>
        <PlanHead label="直发" color="default" />
        <div className="production-roll__line">不进入加工工序，回录后生成直发记录</div>
      </div>
    )
  }

  return (
    <div>
      <PlanHead label={(isRewind ? rewindModeLabel(production) : '锯纸') ?? '复卷'} color={isRewind ? 'blue' : 'green'} />
      <div className="production-roll__line">加工方式：{PROCESS_MODE[production.processMode ?? 1]}</div>
      <div className="production-roll__line">方案条件：{buildConditionText(production)}</div>
      <div className="production-roll__line">门幅排布：{buildLayoutText(production)}</div>
      {(trimWidth > 0 || trimWeight > 0) && (
        <div className="production-roll__line">
          修边 {trimWidth > 0 ? `${trimWidth}mm` : '-'}
          {trimWeight > 0 ? ` / ${trimWeight.toFixed(2)} kg` : ''}
        </div>
      )}
      <AdditionalSteps row={row} />
    </div>
  )
}

function FinishColumn({
  groups,
  finishes,
  finishCount,
  spareCount,
}: {
  groups: ReturnType<typeof groupFinishes>
  finishes: FinishProductionVO[]
  finishCount: number
  spareCount: number
}) {
  const rollNos = finishes.filter((f) => f.isSpare !== 1).slice(0, 6)
  const sources = collectSources(finishes)

  return (
    <div>
      <div className="production-roll__head">
        <span className="production-roll__title">成品 {finishCount} 件</span>
        {spareCount > 0 && <Tag color="orange">备用 {spareCount}</Tag>}
      </div>
      <div className="production-roll__line">
        预估重量：{formatKg(sumFinishValue(finishes, 'estimateWeight'))}
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

function PlanHead({ label, color }: { label: string; color: string }) {
  return (
    <div className="production-roll__head">
      <span className="production-roll__title">加工方案</span>
      <Tag color={color}>{label}</Tag>
    </div>
  )
}

function AdditionalSteps({ row }: Props) {
  const steps = row.steps.filter((step) => step.isMain !== 1)
  if (steps.length === 0) return null

  return (
    <Space wrap size={[4, 4]} className="production-roll__spaced">
      {steps.map((step) => <Tag key={step.uuid}>{step.stepName || '追加工序'}</Tag>)}
    </Space>
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

function rollName(production: RollProductionVO, seq: number): string {
  return production.rollNo || production.extraNo || `母卷 ${seq}`
}

function sumFinishValue(finishes: FinishProductionVO[], key: 'estimateWeight' | 'trimWeightShare'): number {
  return finishes.reduce((sum, finish) => sum + (finish[key] ?? 0), 0)
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
