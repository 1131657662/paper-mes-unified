import { CalendarOutlined, DeleteOutlined } from '@ant-design/icons'
import { Button, Drawer, Empty, Tag, Tooltip, Typography } from 'antd'
import {
  finishSpecText,
  formatTon,
} from '../../features/delivery/utils/deliveryFormatters'
import type { AvailableFinishVO } from '../../types/delivery'
import DeliveryMotherRollCell from './DeliveryMotherRollCell'
import DeliveryOutWeightInput from './DeliveryOutWeightInput'
import {
  summarizeDeliverySelection,
  type DeliveryLineEdit,
} from './deliverySelectionModel'
import { buildDeliverySelectionReviewGroups } from './deliverySelectionReviewModel'

interface Props {
  edits: Record<string, DeliveryLineEdit>
  items: AvailableFinishVO[]
  open: boolean
  onClose: () => void
  onEditChange: (finishUuid: string, value: DeliveryLineEdit) => void
  onRemove: (finishUuid: string) => void
}

export default function DeliverySelectionReviewDrawer(props: Props) {
  const summary = summarizeDeliverySelection(props.items, props.edits)
  const groups = buildDeliverySelectionReviewGroups(props.items, props.edits)
  return (
    <Drawer
      className="delivery-selection-review-drawer"
      title="已选出库清单"
      width="min(920px, 100vw)"
      open={props.open}
      onClose={props.onClose}
    >
      <ReviewSummary summary={summary} />
      {groups.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂未选择成品或余料" />
      ) : (
        <div className="delivery-selection-review-groups">
          {groups.map((group) => (
            <section className="delivery-selection-review-group" key={group.key}>
              <header className="delivery-selection-review-group__header">
                <div>
                  <Typography.Text strong>{group.orderNo}</Typography.Text>
                  <span><CalendarOutlined /> {group.orderDate || '未记录日期'}</span>
                </div>
                <span>{group.items.length} 卷 / <strong>{formatTon(group.totalWeight)}</strong></span>
              </header>
              <div className="delivery-selection-review-group__items">
                {group.items.map((item) => (
                  <ReviewItem
                    edit={props.edits[item.finishUuid]}
                    item={item}
                    key={item.finishUuid}
                    onEditChange={props.onEditChange}
                    onRemove={props.onRemove}
                  />
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </Drawer>
  )
}

function ReviewSummary({ summary }: { summary: ReturnType<typeof summarizeDeliverySelection> }) {
  return (
    <div className="delivery-selection-review-summary">
      <span>共 <strong>{summary.totalCount}</strong> 卷</span>
      <span>合计 <strong>{formatTon(summary.totalWeight)}</strong></span>
      <span>成品 <strong>{summary.productCount}</strong> 卷 / {formatTon(summary.productWeight)}</span>
      <span>余料 <strong>{summary.remainCount}</strong> 卷 / {formatTon(summary.remainWeight)}</span>
      {summary.riskCount > 0 && <Tag color="warning">{summary.riskCount} 卷需放行确认</Tag>}
    </div>
  )
}

interface ReviewItemProps {
  edit?: DeliveryLineEdit
  item: AvailableFinishVO
  onEditChange: (finishUuid: string, value: DeliveryLineEdit) => void
  onRemove: (finishUuid: string) => void
}

function ReviewItem({ edit, item, onEditChange, onRemove }: ReviewItemProps) {
  return (
    <div className="delivery-selection-review-item">
      <div className="delivery-selection-review-item__identity">
        <div><Typography.Text strong>{item.finishRollNo}</Typography.Text> <FinishKindTag item={item} /></div>
        <span>{item.paperName} / {finishSpecText(item)}</span>
      </div>
      <DeliveryMotherRollCell finish={item} />
      <DeliveryOutWeightInput
        edit={edit}
        finish={item}
        selected
        onChange={(value) => onEditChange(item.finishUuid, value)}
      />
      <Tooltip title="移出已选清单">
        <Button
          danger
          type="text"
          aria-label={`移除 ${item.finishRollNo}`}
          icon={<DeleteOutlined />}
          onClick={() => onRemove(item.finishUuid)}
        />
      </Tooltip>
    </div>
  )
}

function FinishKindTag({ item }: { item: AvailableFinishVO }) {
  const remain = item.isRemain === 1
  return <Tag color={remain ? 'orange' : 'green'}>{remain ? '余料' : '成品'}</Tag>
}
