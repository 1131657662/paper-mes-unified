import { Segmented, Space, Switch, Typography } from 'antd'
import { useState } from 'react'
import CustomerFinishedProductDetailTable from './CustomerFinishedProductDetailTable'
import CustomerFinishedProductsTable from './CustomerFinishedProductsTable'
import type { FinishedProductRow } from './finishedProductRows'
import InternalFinishedProductsTable from './InternalFinishedProductsTable'

type ProductView = 'pickup' | 'internal'
type PickupView = 'summary' | 'items'

interface Props {
  rows: FinishedProductRow[]
}

export default function FinishedProductsTable({ rows }: Props) {
  const [view, setView] = useState<ProductView>('pickup')
  const [pickupView, setPickupView] = useState<PickupView>('summary')
  const [showTrim, setShowTrim] = useState(false)
  const visibleRows = showTrim ? rows : rows.filter(({ finish }) => finish.isRemain !== 1)

  return (
    <div className="finished-products-view">
      <div className="finished-products-toolbar">
        <Space size={8}>
          <Segmented<ProductView>
            aria-label="成品数据视图"
            options={[
              { label: '提货清单', value: 'pickup' },
              { label: '内部明细', value: 'internal' },
            ]}
            value={view}
            onChange={setView}
          />
          {view === 'pickup' && (
            <Segmented<PickupView>
              aria-label="提货清单显示方式"
              options={[
                { label: '规格汇总', value: 'summary' },
                { label: '逐件明细', value: 'items' },
              ]}
              value={pickupView}
              onChange={setPickupView}
            />
          )}
        </Space>
        <Space size={7}>
          <Switch aria-label="显示切边" checked={showTrim} size="small" onChange={setShowTrim} />
          <Typography.Text>显示切边</Typography.Text>
        </Space>
      </div>
      {view === 'internal' ? (
        <InternalFinishedProductsTable rows={visibleRows} />
      ) : pickupView === 'summary' ? (
        <CustomerFinishedProductsTable rows={visibleRows} />
      ) : (
        <CustomerFinishedProductDetailTable rows={visibleRows} />
      )}
    </div>
  )
}
