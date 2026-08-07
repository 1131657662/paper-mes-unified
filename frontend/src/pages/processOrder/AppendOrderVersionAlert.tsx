import { Alert } from 'antd'
import type { ProcessOrderAppendSessionVO } from '../../types/processOrder'
import { hasAppendOrderVersionChange } from './appendOrderVersion'

interface Props {
  session: ProcessOrderAppendSessionVO
}

export default function AppendOrderVersionAlert({ session }: Props) {
  if (!hasAppendOrderVersionChange(session)) return null
  return (
    <Alert
      showIcon
      type="warning"
      message="已恢复未完成的追加会话"
      description={`加工单已从 V${session.baseOrderVersion} 更新到 V${session.currentOrderVersion}。当前追加草稿已保留，提交时将基于最新版本再次校验。`}
    />
  )
}
