import { Navigate, useSearchParams } from 'react-router-dom'
import DeliveryOrderList from './DeliveryOrderList'

export default function DeliveryOrderEntryPage() {
  const [searchParams] = useSearchParams()
  if (searchParams.get('view') === 'inventory') return <Navigate to="/delivery-orders/inventory" replace />
  return <DeliveryOrderList />
}
