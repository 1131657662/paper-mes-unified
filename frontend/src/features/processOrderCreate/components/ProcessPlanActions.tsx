import { CopyOutlined, SaveOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import MesTooltip from '../../../components/biz/MesTooltip'

interface Props {
  batchTargetCount: number
  checkedCount: number
  onlyCurrentTarget: boolean
  saved: boolean
  onApply: () => void
  onSave: () => void
  saving: boolean
}

export default function ProcessPlanActions(props: Props) {
  const batchReason = batchDisabledReason(props.batchTargetCount, props.onlyCurrentTarget)
  const saveReason = props.saved ? '当前加工方案已保存，修改参数后才需要再次保存' : undefined
  return (
    <div className="process-plan-actions" aria-label="加工方案操作">
      <div className="process-plan-actions__scope">
        批量范围：已选 {props.checkedCount} 卷，可应用 {props.batchTargetCount} 卷
      </div>
      <div className="process-plan-actions__buttons">
        <MesTooltip title={saveReason}>
          <span className="process-plan-actions__tooltip" title={saveReason}>
            <Button icon={<SaveOutlined />} loading={props.saving} disabled={props.saved}
              onClick={props.onSave}>
              保存本卷加工方案
            </Button>
          </span>
        </MesTooltip>
        <MesTooltip title={batchReason}>
          <span className="process-plan-actions__tooltip" title={batchReason}>
            <Button
              icon={<CopyOutlined />}
              disabled={Boolean(batchReason)}
              loading={props.saving}
              onClick={props.onApply}
            >
              批量应用加工方案（{props.batchTargetCount} 卷）
            </Button>
          </span>
        </MesTooltip>
      </div>
    </div>
  )
}

function batchDisabledReason(targetCount: number, onlyCurrentTarget: boolean) {
  if (targetCount === 0) return '选中母卷中没有兼容且已保存的批量目标'
  if (onlyCurrentTarget) return '当前只有本卷，请直接保存本卷加工方案'
  return undefined
}
