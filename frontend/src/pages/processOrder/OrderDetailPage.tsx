import { useNavigate, useParams } from 'react-router-dom'
import OrderDetailPanel from '../../features/processOrderDetail/components/OrderDetailPanel'

export default function OrderDetailPage() {
  const { uuid } = useParams<{ uuid: string }>()
  const navigate = useNavigate()

  return (
    <OrderDetailPanel
      uuid={uuid}
      mode="page"
      onBack={() => navigate('/process-orders')}
    />
  )
}
