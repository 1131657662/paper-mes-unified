import { Button, Popover } from 'antd'
import { formatKg } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryInventoryFinish } from '../../types/deliveryInventory'

interface Props {
  items: DeliveryInventoryFinish[]
  onClear: () => void
}

export default function DeliveryInventorySelectionBar({ items, onClear }: Props) {
  if (items.length === 0) return null
  const totalWeight = items.reduce((total, item) => total + item.remainingWeight, 0)
  return (
    <div className="delivery-inventory-selection-bar">
      <span>已选 <strong>{items.length}</strong> 卷，合计 <strong>{formatKg(totalWeight)}</strong></span>
      <div>
        <Popover placement="topRight" title="已选成品卷" content={<SelectedRolls items={items} />}>
          <Button type="link">查看已选</Button>
        </Popover>
        <Button type="link" danger onClick={onClear}>清空</Button>
      </div>
    </div>
  )
}

function SelectedRolls({ items }: { items: DeliveryInventoryFinish[] }) {
  return (
    <div className="delivery-inventory-selected-rolls">
      {items.slice(0, 30).map((item) => <span key={item.finishUuid}>{item.finishRollNo}</span>)}
      {items.length > 30 && <span>另有 {items.length - 30} 卷</span>}
    </div>
  )
}
