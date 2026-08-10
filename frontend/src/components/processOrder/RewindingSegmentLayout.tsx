import { Button, InputNumber, Select, Space, Typography } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import LayoutBar from './LayoutBar'
import type { LayoutItemForm, SegmentForm } from './rewindingConfigModel'

interface LayoutActions {
  onAdd: (itemType: LayoutItemForm['itemType']) => void
  onChange: (itemKey: string, patch: Partial<LayoutItemForm>) => void
  onRemove: (itemKey: string) => void
}

interface Props {
  actions: LayoutActions
  disabled: boolean
  index: number
  originalWidth?: number
  segment: SegmentForm
}

export default function RewindingSegmentLayout(props: Props) {
  const layoutWidth = props.segment.layoutItems.reduce(
    (sum, item) => sum + item.width * (item.quantity ?? 1),
    0,
  )
  const widthDanger = (props.originalWidth ?? 0) > 0 && layoutWidth > (props.originalWidth ?? 0)
  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      {props.segment.layoutItems.map((item) => (
        <LayoutItemFields {...props} item={item} key={item.key} />
      ))}
      <Space>
        <Button size="small" icon={<PlusOutlined />} disabled={props.disabled} onClick={() => props.actions.onAdd('FINISH')}>
          加成品门幅
        </Button>
        <Button size="small" disabled={props.disabled} onClick={() => props.actions.onAdd('TRIM')}>
          加修边
        </Button>
        <Typography.Text type={widthDanger ? 'danger' : 'secondary'}>
          已排布 {layoutWidth} / {props.originalWidth ?? '-'} mm
        </Typography.Text>
      </Space>
      <LayoutBar layoutItems={props.segment.layoutItems} originalWidth={props.originalWidth} />
    </Space>
  )
}

function LayoutItemFields({ actions, disabled, index, item }: Props & { item: LayoutItemForm }) {
  return (
    <Space wrap>
      <Select
        aria-label={`分段 ${index + 1} 排布类型`}
        value={item.itemType}
        onChange={(itemType) => actions.onChange(item.key, { itemType })}
        style={{ width: 96 }}
        options={[{ value: 'FINISH', label: '成品' }, { value: 'TRIM', label: '修边' }]}
        disabled={disabled}
      />
      <InputNumber
        aria-label={`分段 ${index + 1} 排布门幅`}
        min={1}
        value={item.width}
        onChange={(width) => actions.onChange(item.key, { width: width ?? 1 })}
        addonBefore="门幅"
        suffix="mm"
        disabled={disabled}
      />
      <InputNumber
        aria-label={`分段 ${index + 1} 排布数量`}
        min={1}
        value={item.quantity}
        onChange={(quantity) => actions.onChange(item.key, { quantity: quantity ?? 1 })}
        addonBefore="数量"
        disabled={disabled}
      />
      <Button
        type="text"
        danger
        aria-label={`删除${item.itemType === 'FINISH' ? '成品' : '切边'}排布`}
        icon={<DeleteOutlined />}
        onClick={() => actions.onRemove(item.key)}
        disabled={disabled}
      />
    </Space>
  )
}
