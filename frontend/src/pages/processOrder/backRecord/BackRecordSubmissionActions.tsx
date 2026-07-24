import { Button } from 'antd'
import { CheckOutlined, SaveOutlined } from '@ant-design/icons'
import MesTooltip from '../../../components/biz/MesTooltip'

interface Props {
  allRemainingSelected: boolean
  selectedCount: number
  submitting: boolean
  onComplete: () => void
  onSaveBatch: () => void
}

export default function BackRecordSubmissionActions({
  allRemainingSelected,
  selectedCount,
  submitting,
  onComplete,
  onSaveBatch,
}: Props) {
  const saveReason = selectedCount === 0
    ? '请至少选择一个未回录母卷组'
    : allRemainingSelected ? '已选中全部未回录母卷，请直接完成整单' : undefined
  const completeReason = allRemainingSelected ? undefined : '需选中全部未回录母卷组'

  return (
    <>
      <MesTooltip title={saveReason}>
        <span className="back-record-action-tooltip" title={saveReason}>
          <Button
            icon={<SaveOutlined />}
            disabled={Boolean(saveReason)}
            loading={submitting}
            onClick={onSaveBatch}
          >
            保存选中批次
          </Button>
        </span>
      </MesTooltip>
      <MesTooltip title={completeReason}>
        <span className="back-record-action-tooltip" title={completeReason}>
          <Button
            type="primary"
            icon={<CheckOutlined />}
            disabled={Boolean(completeReason)}
            loading={submitting}
            onClick={onComplete}
          >
            完成整单
          </Button>
        </span>
      </MesTooltip>
    </>
  )
}
