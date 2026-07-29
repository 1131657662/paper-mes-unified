import { Button, InputNumber, Select } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { FinishLayerDTO, RewindLayoutItemPlanDTO, RewindSegmentPlanDTO } from '../../../types/processOrder'
import { formatMm } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'
import RewindCustomerSpecificationFields from './RewindCustomerSpecificationFields'
import RewindLayerEditor from './RewindLayerEditor'

interface Props {
  mode: number
  roll: RollDraft
  segment: RewindSegmentPlanDTO
  onChange: (segment: RewindSegmentPlanDTO) => void
}

const itemTypes = [{ label: '成品', value: 'FINISH' }, { label: '切边余料', value: 'TRIM' }]

export default function RewindLayoutEditor({ mode, roll, segment, onChange }: Props) {
  const items = segment.layoutItems ?? []
  const update = (next: RewindLayoutItemPlanDTO[]) => onChange({ ...segment, layoutItems: next })
  return (
    <div className="rewind-layout-list">
      {items.map((item, index) => <LayoutItem key={index} mode={mode} roll={roll}
        index={index} item={item} onChange={(patch) => update(patchItem(items, index, patch))}
        onDelete={mode === 2 || items.length <= 1 ? undefined
          : () => update(items.filter((_, itemIndex) => itemIndex !== index))} />)}
      {mode !== 2 && <Button size="small" icon={<PlusOutlined />}
        onClick={() => update([...items, newLayoutItem(mode, roll)])}>添加排布</Button>}
    </div>
  )
}

function LayoutItem({ mode, roll, index, item, onChange, onDelete }: LayoutItemProps) {
  const finish = (item.itemType ?? 'FINISH') === 'FINISH'
  return (
    <section className="rewind-layout-item">
      <div className="rewind-layout-grid">
        <label className="rewind-field">
          <span className="rewind-field__label">类型</span>
          {mode === 2 ? <span className="rewind-field__readonly">成品</span>
            : <Select aria-label={`排布 ${index + 1} 类型`} value={item.itemType ?? 'FINISH'}
              options={itemTypes} onChange={(itemType) => onChange({ itemType })} />}
        </label>
        <label className="rewind-field">
          <span className="rewind-field__label">物理门幅</span>
          {mode === 2 ? <span className="rewind-field__readonly">{formatMm(item.width)}</span>
            : <InputNumber aria-label={`排布 ${index + 1} 门幅`} min={1} suffix="mm"
              value={item.width} onChange={(width) => onChange({ width: width ?? 1 })} />}
        </label>
        <label className="rewind-field">
          <span className="rewind-field__label">数量</span>
          {mode === 2 ? <span className="rewind-field__readonly">1 件</span>
            : <InputNumber aria-label={`排布 ${index + 1} 数量`} min={1} suffix="件"
              value={item.quantity ?? 1} onChange={(quantity) => onChange({ quantity: quantity ?? 1 })} />}
        </label>
        {onDelete && <MesTooltip title="删除排布"><Button danger className="rewind-layout-item__delete"
          aria-label="删除复卷排布" size="small" icon={<DeleteOutlined />} onClick={onDelete} /></MesTooltip>}
      </div>
      {finish && <RewindCustomerSpecificationFields index={index} item={item} roll={roll}
        onChange={onChange} />}
      {mode === 4 && finish && <RewindLayerEditor item={item}
        defaultCoreDiameter={roll.coreDiameter} defaultOutDiameter={roll.originalDiameter}
        onChange={(next) => onChange(next)} />}
    </section>
  )
}

function newLayoutItem(mode: number, roll: RollDraft): RewindLayoutItemPlanDTO {
  const layers: FinishLayerDTO[] | undefined = mode === 4
    ? [{ outDiameter: roll.originalDiameter, coreDiameter: roll.coreDiameter ?? 3 }] : undefined
  return { width: Math.max(1, Math.floor(roll.originalWidth / 2)), quantity: 1, itemType: 'FINISH', layers }
}

function patchItem(items: RewindLayoutItemPlanDTO[], index: number, patch: Partial<RewindLayoutItemPlanDTO>) {
  return items.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item)
}

interface LayoutItemProps {
  index: number
  item: RewindLayoutItemPlanDTO
  mode: number
  roll: RollDraft
  onChange: (patch: Partial<RewindLayoutItemPlanDTO>) => void
  onDelete?: () => void
}
