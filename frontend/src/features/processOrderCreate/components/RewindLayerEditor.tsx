import { Button, InputNumber, Space, Tag } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { FinishLayerDTO, RewindLayoutItemPlanDTO } from '../../../types/processOrder'

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
    <Space direction="vertical" size={6} className="create-editor-layer-stack">
      {layers.map((layer, index) => (
        <Space key={`layer-${index}`} wrap>
          <Tag color="blue">第 {index + 1} 层</Tag>
          <InputNumber addonBefore="外径" min={1} value={layer.outDiameter} onChange={(value) => updateLayers(patchLayer(layers, index, { outDiameter: value ?? undefined }))} />
          <InputNumber addonBefore="纸芯" min={1} value={layer.coreDiameter} onChange={(value) => updateLayers(patchLayer(layers, index, { coreDiameter: value ?? undefined }))} />
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
        </Space>
      ))}
      <Button size="small" icon={<PlusOutlined />} onClick={() => updateLayers([...layers, defaultLayer(defaultOutDiameter, defaultCoreDiameter)])}>
        添加分层
      </Button>
    </Space>
  )
}

function defaultLayer(outDiameter?: number, coreDiameter?: number): FinishLayerDTO {
  return { outDiameter, coreDiameter: coreDiameter ?? 3 }
}

function patchLayer(layers: FinishLayerDTO[], index: number, patch: Partial<FinishLayerDTO>) {
  return layers.map((layer, layerIndex) => (layerIndex === index ? { ...layer, ...patch } : layer))
}
