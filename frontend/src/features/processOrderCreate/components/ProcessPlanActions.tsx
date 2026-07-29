import { CheckOutlined, CopyOutlined, SaveOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import MesTooltip from '../../../components/biz/MesTooltip'

interface Props {
  batchTargetCount: number
  previewReady: boolean
  saved: boolean
  onExecute: () => void
  saving: boolean
}

export default function ProcessPlanActions(props: Props) {
  const dirty = !props.saved
  const hasTargets = props.batchTargetCount > 0
  const previewBlocked = dirty && !props.previewReady
  const disabled = previewBlocked || (!dirty && !hasTargets)
  const reason = previewBlocked ? '当前方案尚未通过后端预览，请按错误提示修正'
    : disabled ? '当前方案已保存；选择兼容母卷后可批量应用' : undefined
  const label = disabled && !dirty ? '当前方案已保存' : dirty && hasTargets
    ? `保存并应用到 ${props.batchTargetCount} 卷`
    : hasTargets ? `应用到 ${props.batchTargetCount} 卷` : '保存本卷加工方案'
  return (
    <div className="process-plan-actions" aria-label="加工方案操作">
      <div className="process-plan-actions__scope">
        {hasTargets ? `已选择 ${props.batchTargetCount} 卷兼容母卷` : '未选择批量目标'}
      </div>
      <div className="process-plan-actions__buttons">
        <MesTooltip title={reason}>
          <span className="process-plan-actions__tooltip" title={reason}>
            <Button icon={dirty ? <SaveOutlined /> : hasTargets ? <CopyOutlined /> : <CheckOutlined />}
              loading={props.saving} disabled={disabled} onClick={props.onExecute}>
              {label}
            </Button>
          </span>
        </MesTooltip>
      </div>
    </div>
  )
}
