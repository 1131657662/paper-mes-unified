import { Empty, Typography } from 'antd'
import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import type { RollProductionVO } from '../../../types/processOrder'
import ProductionRollCard from './ProductionRollCard'
import './ProductionTree.css'

const { Text } = Typography

interface Props {
  productions?: RollProductionVO[]
}

export default function ProductionTree({ productions = [] }: Props) {
  const rows = buildDisplayRows(productions)

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
            {rows.map((row) => <ProductionRollCard key={row.key} row={row} />)}
          </div>
        )}
      </div>
    </section>
  )
}
