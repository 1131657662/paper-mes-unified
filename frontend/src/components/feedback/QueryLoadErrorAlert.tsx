import { Alert, Button } from 'antd'
import './QueryLoadErrorAlert.css'

interface Props {
  description: string
  message: string
  onRetry: () => void
}

export default function QueryLoadErrorAlert({ description, message, onRetry }: Props) {
  return (
    <Alert
      action={<Button danger size="small" onClick={onRetry}>重新加载</Button>}
      className="query-load-error-alert"
      description={description}
      message={message}
      showIcon
      type="error"
    />
  )
}
