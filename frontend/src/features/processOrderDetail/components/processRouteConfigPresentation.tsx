import { Space, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { OriginalRoll, ProcessRouteOutputVO, ProcessRouteStageLineVO } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm, formatTon } from '../../../utils/numberFormatters'
import type { DetailRouteOutputRow } from '../routeConfigDetail'
import { formatMoney } from '../orderDetailUtils'

export const outputColumns: ColumnsType<DetailRouteOutputRow> = [
  { title: '产物', dataIndex: 'label', width: 150, render: (value, row) => <Space size={4}>{value}<Tag color={row.isRemain === 1 ? 'orange' : undefined}>{row.outputKey}</Tag></Space> },
  { title: '来源编号', width: 180, render: (_, row) => row.finishRollNo || row.sourceRollNo || row.sourceOutputKey || '-' },
  { title: '品名', dataIndex: 'paperName', width: 140 },
  { title: '规格', width: 150, render: (_, row) => `${formatGram(row.gramWeight)} / ${formatMm(row.finishWidth)}` },
  { title: '直径/纸芯', width: 130, render: (_, row) => `${row.finishDiameter ?? '-'} / ${row.finishCoreDiameter ?? '-'}` },
  { title: '预估重量', dataIndex: 'estimateWeight', width: 120, align: 'right', render: formatWeight },
]

export const stageColumns: ColumnsType<ProcessRouteStageLineVO> = [
  { title: '阶段', dataIndex: 'stageLevel', width: 70, render: (value) => `第${value ?? '-'}段` },
  { title: '工艺', dataIndex: 'stepName', width: 90 },
  { title: '来源产物', dataIndex: 'inputOutputKeys', width: 130, render: (value) => Array.isArray(value) && value.length ? value.join('、') : '原卷' },
  { title: '刀数', dataIndex: 'knifeCount', width: 76, render: (value) => value ?? '-' },
  { title: '吨位', dataIndex: 'processWeight', width: 100, render: (value) => value == null ? '-' : formatTon(Number(value)) },
  { title: '单价', dataIndex: 'unitPrice', width: 96, render: formatMoney },
  { title: '费用', dataIndex: 'stepAmount', width: 96, render: formatMoney },
]

export const previewOutputColumns: ColumnsType<ProcessRouteOutputVO> = [
  { title: '产出', dataIndex: 'outputKey', width: 90 },
  { title: '阶段', dataIndex: 'stageLevel', width: 70, render: (value) => `第${value ?? '-'}段` },
  { title: '状态', width: 100, render: (_, row) => routeOutputStatus(row) },
  { title: '门幅', dataIndex: 'finishWidth', width: 90, render: (value) => formatMm(value) },
  { title: '预估重', dataIndex: 'estimateWeight', width: 110, render: formatWeight },
  { title: '备注', dataIndex: 'remark', width: 180 },
]

export function formatWeight(value?: number): string {
  return formatKg(value)
}

export function rollLabel(roll: OriginalRoll): string {
  const no = [roll.rollNo && `卷号:${roll.rollNo}`, roll.extraNo && `编号:${roll.extraNo}`]
    .filter(Boolean).join(' / ')
  return `${roll.rowSort ?? '-'} | ${no || '未编号'} | ${roll.paperName || '-'} | ${formatGram(roll.gramWeight)} / ${formatMm(roll.originalWidth)} | ${formatWeight(rollTotalWeight(roll))}`
}

export function rollTotalWeight(roll: OriginalRoll): number {
  return Number(roll.actualWeight ?? roll.totalWeight
    ?? (Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)))
}

export function stageRowKey(record: ProcessRouteStageLineVO): string {
  return `${record.stageLevel ?? '-'}-${record.stepType ?? '-'}-${record.stepName ?? '-'}`
}

export function stageName(stageLevel?: number): string {
  if (!stageLevel || stageLevel <= 1) return '首道产物'
  return `第${stageLevel}段产物`
}

export function sourceStageText(rows: DetailRouteOutputRow[]): string {
  return Array.from(new Set(rows.map((row) => stageName(row.stageLevel)))).join('、')
}

export function sourceNoText(rows: DetailRouteOutputRow[]): string {
  return rows.map((row) => row.finishRollNo || row.sourceRollNo
    || row.sourceOutputKey || row.outputKey).join('、')
}

export function sourcePaperText(rows: DetailRouteOutputRow[]): string {
  return Array.from(new Set(rows.map((row) => row.paperName || '-'))).join('、')
}

export function sourceSpecText(rows: DetailRouteOutputRow[]): string {
  return rows.map((row) => `${formatGram(row.gramWeight)} / ${formatMm(row.finishWidth)}`).join('、')
}

export function totalSourceWeight(rows: DetailRouteOutputRow[]): number {
  return rows.reduce((sum, row) => sum + Number(row.estimateWeight ?? 0), 0)
}

function routeOutputStatus(row: ProcessRouteOutputVO) {
  if (row.isRemain === 1) return <Tag color="orange">修边</Tag>
  return row.consumedByNextStage ? <Tag color="orange">进入下道</Tag> : <Tag color="green">最终成品</Tag>
}
