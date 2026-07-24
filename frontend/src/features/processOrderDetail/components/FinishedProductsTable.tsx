import { Segmented, Space, Switch, Typography } from 'antd'
import { useState } from 'react'
import CustomerSpecificationDetailView from '../../processOrderCustomerSpec/CustomerSpecificationDetailView'
import type { FinishCustomerSpec } from '../../processOrderCustomerSpec/customerSpecTypes'
import type { FinishedProductRow } from './finishedProductRows'
import PhysicalSpecificationDetailView from './PhysicalSpecificationDetailView'

type ProductView = 'customer' | 'physical'

interface Props {
  customerSpecsError?: boolean
  rows: FinishedProductRow[]
  specs?: FinishCustomerSpec[]
}

export default function FinishedProductsTable({ customerSpecsError = false, rows, specs }: Props) {
  const [view, setView] = useState<ProductView>('customer')
  const [showTrim, setShowTrim] = useState(false)
  const activeView = customerSpecsError ? 'physical' : view
  const visibleRows = showTrim ? rows : rows.filter(({ finish }) => finish.isRemain !== 1)

  return (
    <div className="finished-products-view">
      <div className="finished-products-toolbar">
        <Segmented<ProductView>
          aria-label="成品数据视图"
          options={[
            { disabled: customerSpecsError, label: '客户口径', value: 'customer' },
            { label: '实物明细', value: 'physical' },
          ]}
          value={activeView}
          onChange={setView}
        />
        <Space size={7}>
          <Switch aria-label="显示切边" checked={showTrim} disabled={activeView !== 'physical'} size="small" onChange={setShowTrim} />
          <Typography.Text disabled={activeView !== 'physical'}>显示切边</Typography.Text>
        </Space>
      </div>
      {activeView === 'physical'
        ? <PhysicalSpecificationDetailView rows={visibleRows} />
        : <CustomerSpecificationDetailView rows={visibleRows} specs={specs} />}
    </div>
  )
}
