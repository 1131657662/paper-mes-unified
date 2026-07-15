import { InputNumber } from 'antd'
import type { AvailableFinishVO } from '../../types/delivery'
import {
  deliveryWeightFeedback,
  type DeliveryLineEdit,
} from './deliverySelectionModel'

interface Props {
  edit?: DeliveryLineEdit
  finish: AvailableFinishVO
  selected: boolean
  onChange: (value: DeliveryLineEdit) => void
}

export default function DeliveryOutWeightInput(props: Props) {
  const feedback = deliveryWeightFeedback(props.finish, props.edit)
  return (
    <div className="delivery-out-weight-cell">
      <InputNumber
        aria-label={`${props.finish.finishRollNo || '成品'} 出库重量`}
        min={0}
        precision={3}
        status={props.selected ? feedback.status : undefined}
        disabled={!props.selected || props.finish.sourceType === 2}
        value={feedback.value}
        onChange={(value) => props.onChange({ outWeight: value ?? 0 })}
        onClick={(event) => event.stopPropagation()}
        style={{ width: '100%' }}
      />
      {props.selected && feedback.message && (
        <span className={`delivery-out-weight-cell__feedback is-${feedback.status}`}>
          {feedback.message}
        </span>
      )}
    </div>
  )
}
