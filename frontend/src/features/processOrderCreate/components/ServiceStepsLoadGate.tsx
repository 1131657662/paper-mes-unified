import { Spin } from 'antd'
import type { ReactNode } from 'react'
import QueryLoadErrorAlert from '../../../components/feedback/QueryLoadErrorAlert'

interface Props {
  children: ReactNode
  isError: boolean
  isLoading: boolean
  onRetry: () => void
}

export default function ServiceStepsLoadGate({ children, isError, isLoading, onRetry }: Props) {
  if (isError) {
    return <QueryLoadErrorAlert message="附加工艺配置加载失败"
      description="已暂停附加工艺编辑，避免把未加载的数据误认为没有配置。" onRetry={onRetry} />
  }
  return <Spin spinning={isLoading}>{isLoading ? <div className="service-step-loading" /> : children}</Spin>
}
