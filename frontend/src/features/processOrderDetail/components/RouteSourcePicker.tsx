import { Button, Select, Space, Tag, Tooltip, Typography } from 'antd'
import { CheckCircleOutlined } from '@ant-design/icons'
import type { DetailRouteOutputRow } from '../routeConfigDetail'
import { formatKg } from '../../../utils/numberFormatters'

interface Props {
  inputs: DetailRouteOutputRow[]
  selectedKeys: string[]
  onChange: (keys: string[]) => void
}

export default function RouteSourcePicker({ inputs, selectedKeys, onChange }: Props) {
  const selectedSet = new Set(selectedKeys)
  const selectedRows = inputs.filter((row) => selectedSet.has(row.outputKey))
  const batchKeys = selectedRows[0] ? sameSpecKeys(inputs, selectedRows[0]) : []

  return (
    <div className="process-route-source-picker">
      <div className="process-route-source-picker__head">
        <div className="process-route-source-picker__title">
          <Typography.Text strong>来源产物</Typography.Text>
          <Typography.Text type="secondary">
            已选 {selectedRows.length || 0} 件 / 合计 {formatKg(totalWeight(selectedRows))}
          </Typography.Text>
        </div>
        <Select
          showSearch
          mode="multiple"
          maxTagCount="responsive"
          value={selectedKeys}
          optionFilterProp="label"
          options={inputs.map((row) => ({ value: row.outputKey, label: routeOutputLabel(row) }))}
          className="process-route-source-picker__select"
          placeholder="选择一个或多个来源产物"
          onChange={onChange}
        />
        <Space size={8} className="process-route-source-picker__tools">
          <Tooltip title="按当前已选的第一件产物，批量选择同阶段、同父级来源、同品名、同克重、同门幅、同直径和同纸芯的产物">
            <Button
              size="small"
              disabled={batchKeys.length <= 1 || sameKeys(batchKeys, selectedKeys)}
              onClick={() => onChange(batchKeys)}
            >
              同段同规格全选
            </Button>
          </Tooltip>
          <Button
            size="small"
            disabled={selectedRows.length <= 1}
            onClick={() => selectedRows[0] && onChange([selectedRows[0].outputKey])}
          >
            只保留首件
          </Button>
        </Space>
      </div>
      <div className="process-route-source-picker__flow">
        {groupByStage(inputs).map((group) => (
          <section key={group.stageLevel} className="process-route-source-picker__stage">
            <div className="process-route-source-picker__stage-title">
              <span>{stageName(group.stageLevel)}</span>
              <Tag>{group.rows.length} 件</Tag>
            </div>
            <div className="process-route-source-picker__nodes">
              {group.rows.map((row) => (
                <SourceNode
                  key={row.outputKey}
                  row={row}
                  selected={selectedSet.has(row.outputKey)}
                  onToggle={() => toggleSource(row.outputKey, selectedKeys, onChange)}
                />
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  )
}

function SourceNode({ row, selected, onToggle }: SourceNodeProps) {
  return (
    <Button
      className={selected ? 'process-route-source-node process-route-source-node--selected' : 'process-route-source-node'}
      aria-pressed={selected}
      onClick={onToggle}
    >
      <span className="process-route-source-node__top">
        <span>
          <strong>{sourceNo(row)}</strong>
          <small>{stageName(row.stageLevel)}</small>
        </span>
        <Tag color={selected ? 'blue' : 'default'}>{row.outputKey}</Tag>
      </span>
      <span className="process-route-source-node__meta">
        <Meta label="品名" value={row.paperName || '-'} />
        <Meta label="规格" value={specText(row)} />
        <Meta label="直径/纸芯" value={diameterText(row)} />
        <Meta label="预估重量" value={formatKg(row.estimateWeight)} strong />
        <Meta label="父级来源" value={parentSourceText(row)} />
      </span>
      {selected && (
        <span className="process-route-source-node__selected">
          <CheckCircleOutlined />
          已选为本段来源
        </span>
      )}
    </Button>
  )
}

function Meta({ label, value, strong = false }: MetaProps) {
  return (
    <span className="process-route-source-node__field">
      <small>{label}</small>
      <Tooltip title={value} mouseEnterDelay={0.5}>
        <b className={strong ? 'process-route-source-node__value process-route-source-node__value--strong' : 'process-route-source-node__value'}>
          {value}
        </b>
      </Tooltip>
    </span>
  )
}

function toggleSource(outputKey: string, selectedKeys: string[], onChange: (keys: string[]) => void) {
  if (selectedKeys.includes(outputKey)) {
    if (selectedKeys.length <= 1) return
    onChange(selectedKeys.filter((key) => key !== outputKey))
    return
  }
  onChange([...selectedKeys, outputKey])
}

function groupByStage(inputs: DetailRouteOutputRow[]): StageGroup[] {
  const groups = new Map<number, DetailRouteOutputRow[]>()
  inputs.forEach((row) => {
    const level = row.stageLevel || 1
    groups.set(level, [...(groups.get(level) ?? []), row])
  })
  return Array.from(groups.entries())
    .sort(([a], [b]) => a - b)
    .map(([stageLevel, rows]) => ({ stageLevel, rows }))
}

function routeOutputLabel(row: DetailRouteOutputRow) {
  return `${stageName(row.stageLevel)}｜${row.outputKey}｜${sourceNo(row)}｜${row.paperName || '-'}｜${row.gramWeight ?? '-'}g / ${row.finishWidth ?? '-'}mm｜${formatKg(row.estimateWeight)}`
}

function stageName(stageLevel?: number) {
  if (!stageLevel || stageLevel <= 1) return '首道产物'
  return `第${stageLevel}段产物`
}

function sourceNo(row: DetailRouteOutputRow) {
  return row.finishRollNo || row.sourceRollNo || row.sourceOutputKey || row.outputKey || '未编号产物'
}

function parentSourceText(row: DetailRouteOutputRow) {
  if (row.parentOutputKey) return row.parentOutputKey
  if (row.parentOutputUuid) return row.parentOutputUuid
  if (!row.sourceOutputKey || row.sourceOutputKey === row.outputKey) return '原卷/首道'
  return row.sourceOutputKey
}

function specText(row: DetailRouteOutputRow) {
  return `${row.gramWeight ?? '-'}g / ${row.finishWidth ?? '-'}mm`
}

function diameterText(row: DetailRouteOutputRow) {
  return `${row.finishDiameter ?? '-'} / ${row.finishCoreDiameter ?? '-'}`
}

function sameSpecKeys(inputs: DetailRouteOutputRow[], source: DetailRouteOutputRow) {
  const signature = sourceSignature(source)
  return inputs.filter((row) => sourceSignature(row) === signature).map((row) => row.outputKey)
}

function sourceSignature(row: DetailRouteOutputRow) {
  return [
    row.stageLevel || 1,
    row.parentOutputKey || row.parentOutputUuid || row.sourceOutputKey || '',
    row.sourceStepType ?? '',
    row.paperName || '',
    row.gramWeight ?? '',
    row.finishWidth ?? '',
    row.finishDiameter ?? '',
    row.finishCoreDiameter ?? '',
  ].join('|')
}

function sameKeys(a: string[], b: string[]) {
  if (a.length !== b.length) return false
  const keys = new Set(a)
  return b.every((key) => keys.has(key))
}

function totalWeight(rows: DetailRouteOutputRow[]) {
  return rows.reduce((sum, row) => sum + Number(row.estimateWeight ?? 0), 0)
}

interface StageGroup {
  rows: DetailRouteOutputRow[]
  stageLevel: number
}

interface SourceNodeProps {
  row: DetailRouteOutputRow
  selected: boolean
  onToggle: () => void
}

interface MetaProps {
  label: string
  value: string
  strong?: boolean
}
