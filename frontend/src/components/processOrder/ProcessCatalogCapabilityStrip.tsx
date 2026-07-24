import { Space, Tag } from 'antd'
import type { ProcessCatalog } from '../../types/processCatalog'

const CATEGORY_NAMES: Record<ProcessCatalog['category'], string> = {
  PRODUCTION: '生产工序',
  SERVICE: '服务工序',
  QUALITY: '质检工序',
  PACKAGING: '包装工序',
  LOGISTICS: '物流工序',
}

const PRICING_NAMES: Record<ProcessCatalog['pricingStrategy'], string> = {
  SAW_KNIFE: '按刀计费',
  REWIND_WEIGHT: '按吨计费',
  SERVICE_QUANTITY: '按服务数量计费',
}

export default function ProcessCatalogCapabilityStrip({ catalog }: { catalog: ProcessCatalog }) {
  return (
    <Space size={[4, 4]} wrap aria-label={`${catalog.name}工艺能力`}>
      <Tag>{CATEGORY_NAMES[catalog.category]}</Tag>
      <Tag>{PRICING_NAMES[catalog.pricingStrategy]}</Tag>
      <Tag color={catalog.producesInventoryOutput ? 'green' : 'default'}>
        {catalog.producesInventoryOutput ? '产生库存成品' : '不产生库存'}
      </Tag>
      {catalog.allowsLossRecording && <Tag>支持损耗回录</Tag>}
    </Space>
  )
}
