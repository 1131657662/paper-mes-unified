import type { DeliveryInventoryFinish } from '../../types/deliveryInventory'

export function formatSpecification(item: DeliveryInventoryFinish) {
  return [
    item.gramWeight && `${item.gramWeight}g`,
    item.finishWidth && `${item.finishWidth}mm`,
    item.finishDiameter && `Φ${item.finishDiameter}mm`,
    item.finishCoreDiameter && `纸芯${item.finishCoreDiameter}mm`,
  ]
    .filter(Boolean).join(' / ') || '-'
}
