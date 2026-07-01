import { Empty, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { DisplayRow } from './shared/types'
import {
  buildLayoutText,
  calcTrimWidth,
  dict,
  fmt,
  fmtKg,
  groupFinishes,
} from './shared/detailHelpers'
import { buildDisplayRows } from './shared/displayRowBuilder'
import ExpandedProductionRow from './ExpandedProductionRow'
import TooltipText from '../biz/TooltipText'
import type { RollProductionVO } from '../../types/processOrder'
import { ROLL_STATUS } from '../../constants/processOrder'

const { Text } = Typography

interface Props {
  productions: RollProductionVO[]
  compact?: boolean
}

/* ========== 完整版主行列 ========== */

const fullColumns: ColumnsType<DisplayRow> = [
  {
    title: '#', dataIndex: 'seq', width: 40, align: 'center',
  },
  {
    title: '母卷号', width: 115,
    render: (_, row) => {
      if (row.isMergeGroup) {
        return <Text strong>合并 ({row.rollProductions.length}卷)</Text>
      }
      return <Text>{row.mainProduction.rollNo || '-'}</Text>
    },
  },
  {
    title: '规格', width: 165,
    render: (_, row) => {
      const p = row.mainProduction
      return (
        <Space direction="vertical" size={0}>
          <Text style={{ fontSize: 12 }}>{p.paperName || '-'}</Text>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {fmt(p.gramWeight, 'g')} / {fmt(p.originalWidth, 'mm')}
          </Text>
        </Space>
      )
    },
  },
  {
    title: '卷重 / 件', width: 100, align: 'right',
    render: (_, row) => {
      const totalWeight = row.rollProductions.reduce((s, p) => s + (p.rollWeight ?? 0), 0)
      const totalPieces = row.rollProductions.reduce((s, p) => s + (p.pieceNum ?? 1), 0)
      return (
        <Space direction="vertical" size={0}>
          <Text>{fmtKg(totalWeight)}</Text>
          <Text type="secondary" style={{ fontSize: 11 }}>× {totalPieces}件</Text>
        </Space>
      )
    },
  },
  {
    title: '类型', width: 90,
    render: (_, row) => {
      if (row.isDirectShip) return <Tag style={{ margin: 0 }}>直发</Tag>
      const isRewind = row.mainProduction.mainStepType === 2
      return (
        <Space direction="vertical" size={0}>
          <Tag color={isRewind ? 'blue' : 'green'} style={{ margin: 0 }}>
            {isRewind ? '复卷' : '锯纸'}
          </Tag>
        </Space>
      )
    },
  },
  {
    title: '产出规格', width: 175,
    render: (_, row) => {
      const layoutText = buildLayoutText(row.mainProduction)
      const trim = calcTrimWidth(row.mainProduction)
      if (row.isDirectShip) return <Text type="secondary">直发</Text>
      if (layoutText === '-') return <Text type="secondary">-</Text>
      return (
        <Space direction="vertical" size={0}>
          <Text style={{ fontSize: 12 }}>{layoutText}</Text>
          {trim > 0 && (
            <Text type="secondary" style={{ fontSize: 11 }}>修边 {trim}mm</Text>
          )}
        </Space>
      )
    },
  },
  {
    title: '产量', width: 100, align: 'right',
    render: (_, row) => {
      const groups = groupFinishes(row.finishes)
      const spareCount = row.finishes.filter((f) => f.isSpare === 1).length
      const totalCount = groups.reduce((s, g) => s + g.count, 0) + spareCount
      const totalWeight = groups.reduce((s, g) => s + g.totalEstimate, 0)
      if (!totalCount) return <Text type="secondary">-</Text>
      return (
        <Space direction="vertical" size={0}>
          <Text>{totalCount}件</Text>
          {totalWeight > 0 && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              {(totalWeight / 1000).toFixed(3)}t
            </Text>
          )}
        </Space>
      )
    },
  },
  {
    title: '状态', width: 70,
    render: (_, row) => {
      const status = row.mainProduction.rollStatus
      return <Tag style={{ margin: 0 }}>{dict(ROLL_STATUS, status)}</Tag>
    },
  },
]

/* ========== 精简版主行列（抽屉用） ========== */

const compactColumns: ColumnsType<DisplayRow> = [
  {
    title: '#', dataIndex: 'seq', width: 36, align: 'center',
  },
  {
    title: '母卷', width: 150,
    render: (_, row) => {
      if (row.isMergeGroup) {
        return <Text strong>合并复卷 ({row.rollProductions.length}卷)</Text>
      }
      const p = row.mainProduction
      return (
        <Space direction="vertical" size={0}>
          <Text strong>{p.rollNo || p.paperName || '-'}</Text>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {p.paperName} / {fmt(p.gramWeight, 'g')} / {fmt(p.originalWidth, 'mm')}
          </Text>
        </Space>
      )
    },
  },
  {
    title: '类型', width: 55,
    render: (_, row) => {
      if (row.isDirectShip) return <Tag style={{ margin: 0, fontSize: 11 }}>直发</Tag>
      const isRewind = row.mainProduction.mainStepType === 2
      return (
        <Tag color={isRewind ? 'blue' : 'green'} style={{ margin: 0, fontSize: 11 }}>
          {isRewind ? '复卷' : '锯纸'}
        </Tag>
      )
    },
  },
  {
    title: '产出规格', width: 130,
    render: (_, row) => {
      const layoutText = buildLayoutText(row.mainProduction)
      const trim = calcTrimWidth(row.mainProduction)
      if (row.isDirectShip) return <Text type="secondary" style={{ fontSize: 11 }}>直发</Text>
      if (layoutText === '-') return <Text type="secondary" style={{ fontSize: 11 }}>-</Text>
      const suffix = trim > 0 ? ` · 修${trim}` : ''
      return <TooltipText value={`${layoutText}${suffix}`} />
    },
  },
  {
    title: '产量', width: 85, align: 'right',
    render: (_, row) => {
      const groups = groupFinishes(row.finishes)
      const spareCount = row.finishes.filter((f) => f.isSpare === 1).length
      const totalCount = groups.reduce((s, g) => s + g.count, 0) + spareCount
      const totalWeight = groups.reduce((s, g) => s + g.totalEstimate, 0)
      if (!totalCount) return <Text type="secondary" style={{ fontSize: 11 }}>-</Text>
      return (
        <Space direction="vertical" size={0}>
          <Text style={{ fontSize: 11 }}>{totalCount}件</Text>
          {totalWeight > 0 && (
            <Text type="secondary" style={{ fontSize: 10 }}>{(totalWeight / 1000).toFixed(2)}t</Text>
          )}
        </Space>
      )
    },
  },
  {
    title: '状态', width: 60,
    render: (_, row) => {
      const status = row.mainProduction.rollStatus
      return <Tag style={{ margin: 0, fontSize: 11 }}>{dict(ROLL_STATUS, status)}</Tag>
    },
  },
]

/* ========== 组件 ========== */

export default function ProductionFlowTable({ productions, compact }: Props) {
  const rows = buildDisplayRows(productions)
  const columns = compact ? compactColumns : fullColumns

  // 汇总
  const allFinishes = rows.flatMap((r) => r.finishes)
  const finishGroups = groupFinishes(allFinishes)
  const totalSpareCount = allFinishes.filter((f) => f.isSpare === 1).length
  const totalFinishCount = finishGroups.reduce((s, g) => s + g.count, 0) + totalSpareCount
  const totalEstimateWeight = finishGroups.reduce((s, g) => s + g.totalEstimate, 0)

  const hasSummary = rows.length > 0

  return (
    <Table
      rowKey="key"
      size="small"
      columns={columns}
      dataSource={rows}
      pagination={false}
      scroll={{ x: 'max-content' }}
      locale={{ emptyText: <Empty description="暂无母卷数据" /> }}
      expandable={{
        expandedRowRender: (row) => <ExpandedProductionRow row={row} />,
        rowExpandable: (row) => {
          if (row.isDirectShip) return false
          return row.hasConfig || row.finishes.length > 0
        },
        defaultExpandAllRows: true,
      }}
      onRow={(record) => ({
        style: record.isMergeGroup ? { backgroundColor: '#f0f5ff' } : undefined,
      })}
      summary={
        !compact && hasSummary
          ? () => (
              <Table.Summary.Row style={{ fontWeight: 500, backgroundColor: '#fafafa' }}>
                <Table.Summary.Cell index={0} align="center">
                  <Text type="secondary" style={{ fontSize: 11 }}>∑</Text>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={1}>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {rows.reduce((s, r) => s + r.rollProductions.length, 0)} 卷
                  </Text>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={2} />
                <Table.Summary.Cell index={3} align="right" />
                <Table.Summary.Cell index={4} />
                <Table.Summary.Cell index={5} />
                <Table.Summary.Cell index={6} align="right">
                  <Text style={{ fontSize: 12 }}>
                    {totalFinishCount}件
                    {totalEstimateWeight > 0 && (
                      <Text type="secondary" style={{ fontSize: 11, marginLeft: 4 }}>
                        {(totalEstimateWeight / 1000).toFixed(3)}t
                      </Text>
                    )}
                  </Text>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={7} />
              </Table.Summary.Row>
            )
          : undefined
      }
    />
  )
}
