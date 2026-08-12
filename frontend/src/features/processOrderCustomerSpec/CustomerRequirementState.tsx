import { Alert, Button } from 'antd'
import { HistoryOutlined } from '@ant-design/icons'
import { DISPLAY_TERMS } from '../../constants/displayTerms'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import type { FinishCustomerRevisionPreview } from './customerSpecTypes'
import CustomerRequirementStrip from './CustomerRequirementStrip'

interface Props {
  canEdit: boolean
  editHint?: string
  data?: FinishCustomerRevisionPreview
  isError: boolean
  loading: boolean
  onEdit: () => void
  onHistory: () => void
  onRetry: () => void
}

export default function CustomerRequirementState(props: Props) {
  if (!props.isError) {
    return <>
      {props.editHint && <Alert className="customer-requirement-state__hint" showIcon type="info" message={props.editHint} />}
      {props.data?.pendingDeliveryCount ? <Alert className="customer-requirement-state__hint" showIcon type="warning"
        message={`本次商业规格修正将影响 ${props.data.pendingDeliveryCount} 张待出库单的后续展示；已确认出库单不会改变。`} /> : null}
      <CustomerRequirementStrip canEdit={props.canEdit} data={props.data}
        loading={props.loading} onEdit={props.onEdit} onHistory={props.onHistory} />
    </>
  }
  return (
    <div className="customer-requirement-error">
      <QueryLoadErrorAlert message={`${DISPLAY_TERMS.customerSpecification}加载失败`}
        description="客户规格未成功加载，不能据此判断客户要求与实物一致。"
        onRetry={props.onRetry} />
      <Button icon={<HistoryOutlined />} onClick={props.onHistory}>查看历史版本</Button>
    </div>
  )
}
