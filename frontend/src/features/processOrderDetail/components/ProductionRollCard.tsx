import { Space, Tag } from 'antd'
import { DownOutlined, RightOutlined } from '@ant-design/icons'
import type { DisplayRow } from '../../../components/processOrder/shared/types'
import {
  buildConditionText,
  buildLayoutText,
  calcTrimWidth,
  groupFinishes,
  isActiveSpareProductionFinish,
  isDeliverableProductionFinish,
  rewindModeLabel,
  trimWeightFromFinishes,
} from '../../../components/processOrder/shared/detailHelpers'
import { PROCESS_MODE } from '../../../constants/processOrder'
import type { RollProductionVO } from '../../../types/processOrder'
import { formatGram, formatMm } from '../../../utils/numberFormatters'
import { formatProductionKg, sumProductionEstimateWeight } from '../orderDetailUtils'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'
import ProductionFinishColumn from './ProductionFinishColumn'
import ProductionRollSourceColumn from './ProductionRollSourceColumn'
import ProductionRouteOutputs from './ProductionRouteOutputs'

interface Props {
  canAppendRoute?: boolean
  canEditPending?: boolean
  canEditRemark?: boolean
  onConfigureRoute?: (target: ProcessRouteConfigTarget) => void
  onEditRollRemark?: (roll: RollProductionVO) => void
  row: DisplayRow
  collapse?: { expanded: boolean; onToggle: () => void }
}

export default function ProductionRollCard({
  canAppendRoute,
  canEditPending,
  canEditRemark,
  collapse,
  onConfigureRoute,
  onEditRollRemark,
  row,
}: Props) {
  const trimWidth = calcTrimWidth(row.mainProduction)
  const trimWeight = trimWeightFromFinishes(row.finishes)
  const spareCount = row.finishes.filter(isActiveSpareProductionFinish).length
  const finishCount = row.finishes.filter(isDeliverableProductionFinish).length
  const originalUuid = resolveOriginalUuid(row)

  if (collapse && !collapse.expanded) {
    return <CollapsedRoll row={row} finishCount={finishCount} onToggle={collapse.onToggle} />
  }

  return (
    <div className="production-roll">
      {collapse && <RollToggle expanded label={rollTitle(row)} onToggle={collapse.onToggle} />}
      <div className="production-roll__summary">
        <ProductionRollSourceColumn
          canEditPending={canEditPending}
          canEditRemark={canEditRemark}
          onConfigureRoute={onConfigureRoute}
          onEditRollRemark={onEditRollRemark}
          originalUuid={originalUuid}
          row={row}
        />
        <PlanColumn row={row} trimWidth={trimWidth} trimWeight={trimWeight} />
        <ProductionFinishColumn
          estimateWeight={sumProductionEstimateWeight(row.mainProduction)}
          groups={groupFinishes(row.finishes)}
          finishes={row.finishes}
          finishCount={finishCount}
          production={row.mainProduction}
          spareCount={spareCount}
        />
      </div>
      <ProductionRouteOutputs
        canAppendRoute={canAppendRoute}
        finishes={row.finishes}
        onConfigureRoute={onConfigureRoute}
        originalUuid={originalUuid}
        outputs={row.mainProduction.stageOutputs}
        production={row.mainProduction}
      />
    </div>
  )
}

function CollapsedRoll({ finishCount, onToggle, row }: { finishCount: number; onToggle: () => void; row: DisplayRow }) {
  const production = row.mainProduction
  return (
    <div className="production-roll production-roll--collapsed">
      <button className="production-roll__toggle" type="button" onClick={onToggle}>
        <RightOutlined />
        <strong>{rollTitle(row)}</strong>
        <span>{production.paperName || '-'} / {formatGram(production.gramWeight)} / {formatMm(production.originalWidth)}</span>
        <span>成品 {finishCount} 件</span>
      </button>
    </div>
  )
}

function rollTitle(row: DisplayRow): string {
  if (row.isMergeGroup) return `合并复卷 ${row.rollProductions.length} 卷`
  return row.mainProduction.rollNo || row.mainProduction.extraNo || `母卷 ${row.seq}`
}

function RollToggle({ expanded, label, onToggle }: { expanded: boolean; label: string; onToggle: () => void }) {
  return (
    <button className="production-roll__toggle production-roll__toggle--expanded" type="button" onClick={onToggle}>
      {expanded ? <DownOutlined /> : <RightOutlined />}
      <strong>{label}</strong>
      <span>收起</span>
    </button>
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
          修边 {trimWidth > 0 ? formatMm(trimWidth) : '-'}
          {trimWeight > 0 ? ` / ${formatProductionKg(trimWeight, production)}` : ''}
        </div>
      )}
      <AdditionalSteps row={row} />
    </div>
  )
}

function resolveOriginalUuid(row: DisplayRow) {
  return row.mainProduction.originalUuid || row.originalUuids[0]
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
