import { SaveOutlined } from '@ant-design/icons'
import { Button, Space } from 'antd'
import MesTooltip from '../../components/biz/MesTooltip'

interface Props {
  disabledReason?: string
  onCancel: () => void
  onSaveAll: () => void
  saving: boolean
}

export default function ConfigFinishHeaderActions(props: Props) {
  return (
    <Space>
      <Button onClick={props.onCancel}>取消</Button>
      <MesTooltip title={props.disabledReason}>
        <span className="config-finish-action-slot">
          <Button
            aria-label={props.disabledReason ? `保存全部并完成：${props.disabledReason}` : '保存全部并完成'}
            disabled={Boolean(props.disabledReason)}
            icon={<SaveOutlined />}
            loading={props.saving}
            type="primary"
            onClick={props.onSaveAll}
          >保存全部并完成</Button>
        </span>
      </MesTooltip>
    </Space>
  )
}
