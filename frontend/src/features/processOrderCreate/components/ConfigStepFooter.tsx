import { LeftOutlined, RightOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import type { ConfigStepProgress } from '../configStepProgress'
import { configStepProgressText } from '../configStepProgress'

interface Props {
  autoFinishConfigEnabled?: boolean
  configurationLoading?: boolean
  hasUnsavedServiceChanges: boolean
  onNext: () => void
  onPrev: () => void
  progress: ConfigStepProgress
  saving: boolean
  serviceWritePending: boolean
}

export default function ConfigStepFooter(props: Props) {
  const nextLabel = props.hasUnsavedServiceChanges
    ? '请先保存当前修改'
    : props.progress.pendingCount > 0
      ? props.autoFinishConfigEnabled
        ? `保存待处理方案并继续（${props.progress.pendingCount} 卷）`
        : `定位下一待办（${props.progress.pendingCount} 卷）`
      : '下一步：预览确认'
  return (
    <div className="config-step-footer">
      <Button icon={<LeftOutlined />} disabled={props.saving || props.serviceWritePending
        || props.hasUnsavedServiceChanges} onClick={props.onPrev}>
        上一步
      </Button>
      <div className="config-step-footer__summary" aria-live="polite">
        <span>{configStepProgressText(props.progress)}</span>
        {props.hasUnsavedServiceChanges && (
          <strong>当前卷附加工艺有未保存修改</strong>
        )}
        {props.serviceWritePending && <strong>附加工艺正在保存或删除</strong>}
        {props.configurationLoading && <strong>正在读取附加工艺配置</strong>}
      </div>
      <Button
        type="primary"
        icon={<RightOutlined />}
        iconPosition="end"
        disabled={props.saving || props.serviceWritePending || props.configurationLoading
          || props.hasUnsavedServiceChanges}
        loading={props.saving || props.serviceWritePending || props.configurationLoading}
        onClick={props.onNext}
      >
        {nextLabel}
      </Button>
    </div>
  )
}
