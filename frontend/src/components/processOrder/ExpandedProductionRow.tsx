import { Descriptions, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { DisplayRow } from './shared/types'
import {
  buildConditionText,
  calcTrimWidth,
  fmt,
  fmtDiameter,
  groupFinishes,
  REWIND_MODE,
} from './shared/detailHelpers'

const { Text } = Typography

interface Props {
  row: DisplayRow
}

interface FinishRow {
  finishWidth: number
  count: number
  totalEstimate: number
}

const finishCols: ColumnsType<FinishRow> = [
  { title: '门幅', dataIndex: 'finishWidth', width: 80, render: (v: number) => fmt(v, 'mm') },
  { title: '数量', dataIndex: 'count', width: 60 },
  { title: '预估重', dataIndex: 'totalEstimate', width: 90, render: (v: number) => (v > 0 ? `${v.toFixed(2)} kg` : '-') },
]

export default function ExpandedProductionRow({ row }: Props) {
  const p = row.mainProduction
  const params = row.rewindParams
  const mode = params[0]?.paramMode
  const isRewind = p.mainStepType === 2

  const groups = groupFinishes(row.finishes)
  const finishData: FinishRow[] = groups.map((g) => ({
    finishWidth: g.width,
    count: g.count,
    totalEstimate: g.totalEstimate,
  }))

  const trim = calcTrimWidth(p)
  const trimWeight = row.finishes.reduce((s, f) => s + (f.trimWeightShare ?? 0), 0)
  const allSources = row.finishes.flatMap((f) => f.sources ?? [])
  const uniqueSources = Array.from(
    new Map(allSources.filter((s) => s.originalUuid).map((s) => [s.originalUuid, s])).values(),
  )

  const additionalSteps = (row.steps ?? [])
    .filter((s) => s.isMain !== 1)
    .sort((a, b) => (a.stepSort ?? 0) - (b.stepSort ?? 0))

  /* ----- 直发 ----- */
  if (row.isDirectShip || p.processMode === 3) {
    return (
      <div style={{ padding: '8px 0' }}>
        <Descriptions size="small" column={2} colon={false}>
          <Descriptions.Item label="加工方式">直发（不加工，原纸直接入库）</Descriptions.Item>
        </Descriptions>
      </div>
    )
  }

  /* ----- 未配置 ----- */
  if (!row.hasConfig && groups.length === 0) {
    return (
      <div style={{ padding: '8px 0' }}>
        <Text type="secondary">未配置加工方案</Text>
      </div>
    )
  }

  /* ----- 已配置 ----- */
  return (
    <div style={{ padding: '8px 0' }}>
      {/* 工艺参数 */}
      <Descriptions size="small" column={3} colon={false}>
        {isRewind && mode != null && (
          <Descriptions.Item label="复卷模式">{REWIND_MODE[mode] ?? `模式${mode}`}</Descriptions.Item>
        )}
        {isRewind && params[0]?.outDiameter != null && (
          <Descriptions.Item label="成品直径">≤{fmtDiameter(params[0].outDiameter)}</Descriptions.Item>
        )}
        {isRewind && params[0]?.coreDiameter != null && (
          <Descriptions.Item label="纸芯">{params[0].coreDiameter}"</Descriptions.Item>
        )}
        {isRewind && (mode === 1 || mode === 3 || mode === 4) && (
          <Descriptions.Item label="分段条件">{buildConditionText(p)}</Descriptions.Item>
        )}
        {!isRewind && (
          <Descriptions.Item label="工艺">锯纸 · {buildConditionText(p)}</Descriptions.Item>
        )}
        {isRewind && mode === 5 && uniqueSources.length > 0 && (
          <Descriptions.Item label="来源">
            {uniqueSources.map((s) => `${s.rollNo || s.paperName || '-'}(${s.shareRatio ?? 0}%)`).join(', ')}
          </Descriptions.Item>
        )}
      </Descriptions>

      {/* 成品明细表 */}
      {finishData.length > 0 && (
        <>
          <Table
            size="small"
            rowKey="finishWidth"
            columns={finishCols}
            dataSource={finishData}
            pagination={false}
            style={{ marginTop: 8, marginBottom: 0 }}
            locale={{ emptyText: null }}
          />
          {(trim > 0 || trimWeight > 0) && (
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 2 }}>
              修边: {trim}mm{trimWeight > 0 ? ` · ${trimWeight.toFixed(2)} kg` : ''}
            </Text>
          )}
        </>
      )}

      {/* 追加工序 */}
      {additionalSteps.length > 0 && (
        <div style={{ marginTop: 6 }}>
          <Text type="secondary" style={{ fontSize: 11 }}>
            追加工序:{' '}
            {additionalSteps.map((s) => {
              const parts = [s.stepName || '工序']
              if (s.processWeight != null) parts.push(`${s.processWeight.toFixed(3)}吨`)
              if (s.knifeCount != null && s.stepType === 1) parts.push(`${s.knifeCount}刀`)
              return parts.join(' ')
            }).join(' | ')}
          </Text>
        </div>
      )}

      {/* 备品 */}
      {row.finishes.filter((f) => f.isSpare === 1).length > 0 && (
        <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
          备品: {row.finishes.filter((f) => f.isSpare === 1).length}卷
        </Text>
      )}
    </div>
  )
}
