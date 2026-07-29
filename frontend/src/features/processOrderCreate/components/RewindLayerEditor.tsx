import { Button, InputNumber, Tag } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { FinishLayerDTO, RewindLayoutItemPlanDTO } from '../../../types/processOrder'
import { storedCoreDiameterUnit, storedDiameterUnit } from '../../../utils/numberFormatters'

interface Props {
  item: RewindLayoutItemPlanDTO
  defaultCoreDiameter?: number
  defaultOutDiameter?: number
  onChange: (item: RewindLayoutItemPlanDTO) => void
}

export default function RewindLayerEditor({ item, defaultCoreDiameter, defaultOutDiameter, onChange }: Props) {
  const layers = item.layers?.length ? item.layers : [defaultLayer(defaultOutDiameter, defaultCoreDiameter)]

  const updateLayers = (next: FinishLayerDTO[]) => onChange({ ...item, layers: next })

  return (
    <div className="rewind-layer-list">
      {layers.map((layer, index) => (
        <div className="rewind-layer-row" key={`layer-${index}`}>
          <Tag color="blue">第 {index + 1} 层</Tag>
          <label className="rewind-field">
            <span className="rewind-field__label">外径</span>
            <InputNumber aria-label={`第 ${index + 1} 层外径`} min={1}
              suffix={storedDiameterUnit(layer.outDiameter)} value={layer.outDiameter}
              onChange={(value) => updateLayers(patchLayer(layers, index, { outDiameter: value ?? undefined }))} />
          </label>
          <label className="rewind-field">
            <span className="rewind-field__label">纸芯</span>
            <InputNumber aria-label={`第 ${index + 1} 层纸芯`} min={1}
              suffix={storedCoreDiameterUnit(layer.coreDiameter)} value={layer.coreDiameter}
              onChange={(value) => updateLayers(patchLayer(layers, index, { coreDiameter: value ?? undefined }))} />
          </label>
          <MesTooltip title="删除分层">
            <Button
              danger
              aria-label="删除复卷分层"
              disabled={layers.length <= 1}
              size="small"
              icon={<DeleteOutlined />}
              onClick={() => updateLayers(layers.filter((_, layerIndex) => layerIndex !== index))}
            />
          </MesTooltip>
        </div>
      ))}
      <Button size="small" icon={<PlusOutlined />} onClick={() => updateLayers([...layers, defaultLayer(defaultOutDiameter, defaultCoreDiameter)])}>
        添加分层
      </Button>
    </div>
  )
}

function defaultLayer(outDiameter?: number, coreDiameter?: number): FinishLayerDTO {
  return { outDiameter, coreDiameter: coreDiameter ?? 3 }
}

function patchLayer(layers: FinishLayerDTO[], index: number, patch: Partial<FinishLayerDTO>) {
  return layers.map((layer, layerIndex) => (layerIndex === index ? { ...layer, ...patch } : layer))
}
