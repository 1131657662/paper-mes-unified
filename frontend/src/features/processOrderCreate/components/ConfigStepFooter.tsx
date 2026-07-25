import { LeftOutlined, RightOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import type { ConfigStepProgress } from '../configStepProgress'
import { configStepProgressText } from '../configStepProgress'

interface Props {
  hasUnsavedServiceChanges: boolean
  onNext: () => void
  onPrev: () => void
  progress: ConfigStepProgress
  saving: boolean
  serviceWritePending: boolean
}

export default function ConfigStepFooter(props: Props) {
  return (
    <div className="config-step-footer">
      <Button icon={<LeftOutlined />} disabled={props.serviceWritePending} onClick={props.onPrev}>上一步</Button>
      <div className="config-step-footer__summary" aria-live="polite">
        <span>{configStepProgressText(props.progress)}</span>
        {props.hasUnsavedServiceChanges && (
          <strong>当前卷附加工艺有未保存修改</strong>
        )}
        {props.serviceWritePending && <strong>附加工艺正在保存或删除</strong>}
      </div>
      <Button
        type="primary"
        icon={<RightOutlined />}
        iconPosition="end"
        disabled={props.saving || props.serviceWritePending}
        loading={props.saving || props.serviceWritePending}
        onClick={props.onNext}
      >
        下一步：预览确认
      </Button>
    </div>
  )
}
