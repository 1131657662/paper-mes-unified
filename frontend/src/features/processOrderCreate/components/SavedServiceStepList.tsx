import { Typography } from 'antd'
import type { ProcessStep } from '../../../types/processOrder'
import DraftServiceStepRow from './DraftServiceStepRow'

interface Props {
  disabled: boolean
  onDelete: (step: ProcessStep) => void
  onEdit: (step: ProcessStep) => void
  steps: ProcessStep[]
}

export default function SavedServiceStepList({ disabled, onDelete, onEdit, steps }: Props) {
  return (
    <>
      <div className="draft-service-processes__list-header">
        <Typography.Text strong>当前卷已保存配置</Typography.Text>
        <span>{steps.length}</span>
      </div>
      {steps.length ? (
        <div className="draft-service-processes__list">
          {steps.map((step) => (
            <DraftServiceStepRow
              key={step.uuid}
              disabled={disabled}
              step={step}
              onDelete={() => onDelete(step)}
              onEdit={() => onEdit(step)}
            />
          ))}
        </div>
      ) : (
        <div className="draft-service-processes__empty">当前卷暂无附加工艺</div>
      )}
    </>
  )
}
