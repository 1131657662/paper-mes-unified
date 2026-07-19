import { Tabs } from 'antd'
import { useSearchParams } from 'react-router-dom'
import DeliveryInventoryPage from './DeliveryInventoryPage'
import DeliveryOrderList from './DeliveryOrderList'
import './DeliveryManagementPage.css'

type DeliveryView = 'orders' | 'inventory'

export default function DeliveryManagementPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const view = deliveryView(searchParams.get('view'))

  const changeView = (key: string) => {
    const nextView = deliveryView(key)
    const nextParams = new URLSearchParams(searchParams)
    if (nextView === 'orders') nextParams.delete('view')
    else nextParams.set('view', nextView)
    setSearchParams(nextParams)
  }

  return (
    <div className="delivery-management-page">
      <Tabs
        activeKey={view}
        className="delivery-management-page__tabs"
        items={[
          { key: 'orders', label: '出库单' },
          { key: 'inventory', label: '成品库存' },
        ]}
        onChange={changeView}
      />
      <div className="delivery-management-page__content">
        {view === 'orders' ? <DeliveryOrderList /> : <DeliveryInventoryPage />}
      </div>
    </div>
  )
}

function deliveryView(value: string | null): DeliveryView {
  return value === 'inventory' ? 'inventory' : 'orders'
}
