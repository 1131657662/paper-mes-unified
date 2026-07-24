import { useLocation, useNavigate, useParams } from 'react-router-dom'
import OrderDetailPanel from '../../features/processOrderDetail/components/OrderDetailPanel'
import { processOrderReturnTarget } from './processOrderNavigation'

export default function OrderDetailPage() {
  const { uuid } = useParams<{ uuid: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const returnTo = processOrderReturnTarget(location.state, '/process-orders')

  return (
    <OrderDetailPanel
      uuid={uuid}
      mode="page"
      onBack={() => navigate(returnTo)}
    />
  )
}
