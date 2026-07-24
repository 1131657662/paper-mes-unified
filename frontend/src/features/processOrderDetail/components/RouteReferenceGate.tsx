import { Spin } from 'antd'
import type { ReactNode } from 'react'
import QueryLoadErrorAlert from '../../../components/feedback/QueryLoadErrorAlert'

interface Props {
  children: ReactNode
  isError: boolean
  isLoading: boolean
  onRetry: () => void
}

export default function RouteReferenceGate({ children, isError, isLoading, onRetry }: Props) {
  if (isError) {
    return <QueryLoadErrorAlert message="工艺基础资料加载失败"
      description="客户价格或机台资料未成功加载，已暂停工艺配置，避免保存错误默认值。"
      onRetry={onRetry} />
  }
  if (isLoading) {
    return <div className="process-route-config__reference-loading" role="status" aria-label="正在加载客户价格与机台资料">
      <Spin size="large" />
    </div>
  }
  return children
}
