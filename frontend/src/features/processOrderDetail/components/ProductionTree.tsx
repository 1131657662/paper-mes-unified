import { Empty, Typography } from 'antd'
import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import type { RollProductionVO } from '../../../types/processOrder'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'
import ProductionRollCard from './ProductionRollCard'
import './ProductionTree.css'

const { Text } = Typography

interface Props {
  canEditRemark?: boolean
  orderStatus?: number
  onConfigureRoute?: (target: ProcessRouteConfigTarget) => void
  onEditRollRemark?: (roll: RollProductionVO) => void
  productions?: RollProductionVO[]
}

export default function ProductionTree({
  canEditRemark,
  orderStatus,
  onConfigureRoute,
  onEditRollRemark,
  productions = [],
}: Props) {
  const rows = buildDisplayRows(productions)
  const canEditPending = orderStatus == null || orderStatus === 1
  const canAppendRoute = orderStatus == null || orderStatus === 1 || orderStatus === 3

  return (
    <section className="order-detail-section">
      <div className="order-detail-section__header">
        <h2 className="order-detail-section__title">母卷加工产出</h2>
        <Text type="secondary">{rows.length} 组方案</Text>
      </div>
      <div className="order-detail-section__body">
        {rows.length === 0 ? (
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
              />
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
