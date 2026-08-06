import { Segmented, Space, Switch, Typography } from 'antd'
import { useState } from 'react'
import SortClearControl from '../../../components/biz/SortClearControl'
import { DISPLAY_TERMS } from '../../../constants/displayTerms'
import CustomerSpecificationDetailView from '../../processOrderCustomerSpec/CustomerSpecificationDetailView'
import type { FinishCustomerSpec } from '../../processOrderCustomerSpec/customerSpecTypes'
import type { FinishedProductRow } from './finishedProductRows'
import PhysicalSpecificationDetailView from './PhysicalSpecificationDetailView'
import { useFinishedProductsSortState } from './useFinishedProductsSortState'

type ProductView = 'customer' | 'physical'

interface Props {
  customerSpecsError?: boolean
  rows: FinishedProductRow[]
  specs?: FinishCustomerSpec[]
}

export default function FinishedProductsTable({ customerSpecsError = false, rows, specs }: Props) {
  const [view, setView] = useState<ProductView>('customer')
  const [showTrim, setShowTrim] = useState(false)
  const sortState = useFinishedProductsSortState()
  const activeView = customerSpecsError ? 'physical' : view
  const visibleRows = showTrim ? rows : rows.filter(({ finish }) => finish.isRemain !== 1)

  return (
    <div className="finished-products-view">
      <div className="finished-products-toolbar">
        <Segmented<ProductView>
          aria-label="成品数据视图"
          options={[
            { disabled: customerSpecsError, label: DISPLAY_TERMS.customerSpecification, value: 'customer' },
            { label: '实物明细', value: 'physical' },
          ]}
          value={activeView}
          onChange={setView}
        />
        <Space size={7}>
          <Switch aria-label="显示切边" checked={showTrim} disabled={activeView !== 'physical'} size="small" onChange={setShowTrim} />
          <Typography.Text disabled={activeView !== 'physical'}>显示切边</Typography.Text>
        </Space>
        <div className="finished-products-sort-toolbar">
          <SortClearControl activeCount={sortCount(sortState, activeView)} onClear={sortState.clearAll} />
        </div>
      </div>
      {activeView === 'physical'
        ? <PhysicalSpecificationDetailView rows={visibleRows} sortState={sortState} />
        : <CustomerSpecificationDetailView rows={visibleRows} specs={specs} sortState={sortState} />}
    </div>
  )
}

function sortCount(state: ReturnType<typeof useFinishedProductsSortState>, view: ProductView) {
  return view === 'physical'
    ? state.physicalSummary.sortChain.length + state.physicalItems.sortChain.length
    : state.customerSummary.sortChain.length + state.customerItems.sortChain.length
}
