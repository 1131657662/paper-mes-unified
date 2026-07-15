import { Space, Tag, Typography } from 'antd'
import DeliveryOrderGroupCell from './DeliveryOrderGroupCell'
import {
  isDeliveryGroupRow,
  type DeliveryOrderGroupRow,
  type DeliverySelectionTableRow,
} from './deliveryFinishGrouping'

interface Props {
  row: DeliverySelectionTableRow
  value?: string
  onClearGroup: (group: DeliveryOrderGroupRow) => void
  onSelectGroup: (group: DeliveryOrderGroupRow) => void
}

export default function DeliveryFinishIdentityCell(props: Props) {
  if (isDeliveryGroupRow(props.row)) {
    return (
      <DeliveryOrderGroupCell
        group={props.row}
        onClear={() => props.onClearGroup(props.row as DeliveryOrderGroupRow)}
        onSelect={() => props.onSelectGroup(props.row as DeliveryOrderGroupRow)}
      />
    )
  }
  return (
    <Space size={5} wrap>
      <Typography.Text strong>{props.value}</Typography.Text>
      <Tag color={props.row.isRemain === 1 ? 'orange' : 'green'}>
        {props.row.isRemain === 1 ? '余料' : '成品'}
      </Tag>
    </Space>
  )
}
