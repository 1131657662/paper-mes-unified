import { Button, Empty, Segmented, Typography } from 'antd'
import { CompressOutlined, ExpandAltOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import type { RollProductionVO } from '../../../types/processOrder'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'
import FinishedProductsTable from './FinishedProductsTable'
import { buildFinishedProductRows } from './finishedProductRows'
import ProductionRollCard from './ProductionRollCard'
import './FinishedProductsTable.css'
import './ProductionTree.css'

const { Text } = Typography
type OutputView = 'route' | 'products'

interface Props {
  canEditRemark?: boolean
  canManageOrder?: boolean
  orderStatus?: number
  onConfigureRoute?: (target: ProcessRouteConfigTarget) => void
  onEditRollRemark?: (roll: RollProductionVO) => void
  productions?: RollProductionVO[]
}

export default function ProductionTree({
  canEditRemark,
  canManageOrder = false,
  orderStatus,
  onConfigureRoute,
  onEditRollRemark,
  productions = [],
}: Props) {
  const rows = buildDisplayRows(productions)
  const productRows = buildFinishedProductRows(productions)
  const [selectedView, setSelectedView] = useState<OutputView>()
  const [expandedKeys, setExpandedKeys] = useState<Set<string> | null>(null)
  const defaultView = orderStatus === 4 || orderStatus === 5 ? 'products' : 'route'
  const activeView = selectedView ?? defaultView
  const expanded = expandedKeys ?? new Set(rows.slice(0, 1).map((row) => row.key))
  const allExpanded = rows.length > 0 && expanded.size === rows.length
  const canEditPending = canManageOrder && (orderStatus == null || orderStatus === 1)
  const canAppendRoute = canManageOrder && (orderStatus == null || orderStatus === 1 || orderStatus === 3)

  const toggleRow = (key: string) => {
    setExpandedKeys((current) => {
      const next = new Set(current ?? rows.slice(0, 1).map((row) => row.key))
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  const toggleAll = () => {
    setExpandedKeys(allExpanded ? new Set() : new Set(rows.map((row) => row.key)))
  }

  return (
    <section className="order-detail-section">
      <div className="order-detail-section__header">
        <h2 className="order-detail-section__title">母卷加工产出</h2>
        <div className="production-output-switch">
          <Text type="secondary">
            {activeView === 'route' ? `${rows.length} 组方案` : `${productRows.length} 条产出记录`}
          </Text>
          {activeView === 'route' && rows.length > 1 && (
            <Button
              icon={allExpanded ? <CompressOutlined /> : <ExpandAltOutlined />}
              size="small"
              type="text"
              onClick={toggleAll}
            >
              {allExpanded ? '收起全部' : '展开全部'}
            </Button>
          )}
          <Segmented<OutputView>
            aria-label="母卷产出显示方式"
            options={[
              { label: '加工路线', value: 'route' },
              { label: '成品明细', value: 'products' },
            ]}
            value={activeView}
            onChange={setSelectedView}
          />
        </div>
      </div>
      <div className="order-detail-section__body">
        {activeView === 'products' ? (
          <FinishedProductsTable rows={productRows} />
        ) : rows.length === 0 ? (
          <Empty description="暂无母卷加工数据" />
        ) : (
          <div className="production-tree">
            {rows.map((row) => (
              <ProductionRollCard
                key={row.key}
                canAppendRoute={canAppendRoute}
                canEditPending={canEditPending}
                canEditRemark={canEditRemark}
                onConfigureRoute={onConfigureRoute}
                onEditRollRemark={onEditRollRemark}
                row={row}
                collapse={{ expanded: expanded.has(row.key), onToggle: () => toggleRow(row.key) }}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
