import { Button, Tag } from 'antd'
import { EditOutlined, ToolOutlined } from '@ant-design/icons'
import type { DisplayRow } from '../../../components/processOrder/shared/types'
import type { RollProductionVO } from '../../../types/processOrder'
import { ROLL_STATUS } from '../../../constants/processOrder'
import MesTooltip from '../../../components/biz/MesTooltip'
import ProtectedImage from '../../../components/biz/ProtectedImage'
import { formatGram, formatMm } from '../../../utils/numberFormatters'
import { formatProductionKg } from '../orderDetailUtils'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'

interface Props {
  canEditPending?: boolean
  canEditRemark?: boolean
  onConfigureRoute?: (target: ProcessRouteConfigTarget) => void
  onEditRollRemark?: (roll: RollProductionVO) => void
  originalUuid?: string
  row: DisplayRow
}

export default function ProductionRollSourceColumn({
  canEditPending,
  canEditRemark,
  onConfigureRoute,
  onEditRollRemark,
  originalUuid,
  row,
}: Props) {
  const production = row.mainProduction
  const title = row.isMergeGroup ? `合并复卷 ${row.rollProductions.length} 卷` : rollName(production, row.seq)

  return (
    <div>
      <div className="production-roll__head">
        <span className="production-roll__title">{title}</span>
        {row.isMergeGroup && <Tag color="geekblue">多母卷</Tag>}
        <Tag>{ROLL_STATUS[production.rollStatus ?? 1] ?? '-'}</Tag>
        <RouteActions
          canEditPending={canEditPending}
          canEditRemark={canEditRemark}
          originalUuid={originalUuid}
          onConfigureRoute={onConfigureRoute}
          onEditRollRemark={onEditRollRemark}
          production={production}
        />
      </div>
      <div className="production-roll__line">
        {production.paperName || '-'} / {formatGram(production.gramWeight)} / {formatMm(production.originalWidth)}
      </div>
      <div className="production-roll__line">
        来料 {formatProductionKg((production.rollWeight ?? 0) * (production.pieceNum ?? 1), production)}
      </div>
      <RollRemarkNotes row={row} />
      <RollDamageImages row={row} />
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

function RollDamageImages({ row }: { row: DisplayRow }) {
  const images = row.rollProductions.flatMap((production, index) => damageImages(production, index + 1))
  if (!images.length) return null

  return (
    <div className="production-roll__images">
      {images.map((image) => (
        <ProtectedImage
          key={image.key}
          className="production-roll__image"
          path={image.path}
          alt={image.alt}
        />
      ))}
    </div>
  )
}

function RollRemarkNotes({ row }: { row: DisplayRow }) {
  const notes = row.rollProductions.flatMap((production, index) => buildRollNotes(production, index + 1))
  if (!notes.length) return null

  return (
    <div className="production-roll__notes">
      {notes.map((note) => (
        <div className="production-roll__note" key={note.key}>
          <span>{note.label}</span>
          <strong>{note.value}</strong>
        </div>
      ))}
    </div>
  )
}

function buildRollNotes(production: RollProductionVO, seq: number) {
  const prefix = rollName(production, seq)
  return [
    { key: `${production.originalUuid}-batch`, label: `${prefix} 批次`, value: production.batchNo },
    { key: `${production.originalUuid}-damage`, label: `${prefix} 损伤`, value: production.damageDesc },
    { key: `${production.originalUuid}-remark`, label: `${prefix} 备注`, value: production.remark },
  ].filter((item): item is { key: string; label: string; value: string } => Boolean(item.value))
}

function damageImages(production: RollProductionVO, seq: number) {
  return (production.damageImages ?? []).map((path, index) => ({
    alt: `${rollName(production, seq)} 损伤图片 ${index + 1}`,
    key: `${production.originalUuid ?? seq}-${index}-${path}`,
    path,
  }))
}

function RouteActions({
  canEditPending,
  canEditRemark,
  originalUuid,
  onConfigureRoute,
  onEditRollRemark,
  production,
}: {
  canEditPending?: boolean
  canEditRemark?: boolean
  originalUuid?: string
  onConfigureRoute?: (target: ProcessRouteConfigTarget) => void
  onEditRollRemark?: (roll: RollProductionVO) => void
  production: RollProductionVO
}) {
  return (
    <>
      {canEditRemark && (
        <MesTooltip title="编辑原纸备注">
          <Button
            aria-label="编辑原纸备注"
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => onEditRollRemark?.(production)}
          />
        </MesTooltip>
      )}
      {canEditPending && originalUuid && (
        <MesTooltip title="重新配置这卷纸的完整工艺路线">
          <Button
            aria-label="重配整卷工艺"
            type="text"
            size="small"
            icon={<ToolOutlined />}
            onClick={() => onConfigureRoute?.({ mode: 'replace', originalUuid })}
          />
        </MesTooltip>
      )}
    </>
  )
}

function rollName(production: RollProductionVO, seq: number): string {
  return production.rollNo || production.extraNo || `母卷 ${seq}`
}
