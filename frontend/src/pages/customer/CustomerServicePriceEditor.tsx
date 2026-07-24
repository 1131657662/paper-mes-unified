import { Checkbox, InputNumber, Radio, Tag, Typography } from 'antd'
import type { ProcessCatalog } from '../../types/processCatalog'
import type {
  CustomerProcessPriceBasis,
  CustomerProcessPriceSaveDTO,
} from '../../types/customer'
import {
  findPrice,
  SERVICE_PRICE_BASES,
  servicePriceCatalogs,
  toggleCustomerPrice,
  updateCustomerPrice,
} from './customerProcessPriceModel'

interface Props {
  catalogs: ProcessCatalog[]
  value?: CustomerProcessPriceSaveDTO[]
  onChange?: (value: CustomerProcessPriceSaveDTO[]) => void
}

export default function CustomerServicePriceEditor({ catalogs, value = [], onChange }: Props) {
  return (
    <div className="customer-service-price-editor">
      {servicePriceCatalogs(catalogs).map((catalog) => (
        <section className="customer-service-price-process" key={catalog.uuid}>
          <div className="customer-service-price-process__title">
            <Typography.Text strong>{catalog.name}</Typography.Text>
            <Tag>{catalog.category === 'PACKAGING' ? '包装' : '服务'}</Tag>
            <Typography.Text type="secondary">可同时维护多种方案，下单时自动带出默认项</Typography.Text>
          </div>
          <div className="customer-service-price-options">
            {SERVICE_PRICE_BASES.map((option) => (
              <PriceOption key={option.basis} catalogUuid={catalog.uuid} basis={option.basis}
                label={option.label} unit={option.unit} values={value} onChange={onChange} />
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}

function PriceOption({ catalogUuid, basis, label, unit, values, onChange }: {
  catalogUuid: string
  basis: CustomerProcessPriceBasis
  label: string
  unit: string
  values: CustomerProcessPriceSaveDTO[]
  onChange?: (value: CustomerProcessPriceSaveDTO[]) => void
}) {
  const current = findPrice(values, catalogUuid, basis)
  const enabled = Boolean(current)
  return (
    <div className={enabled ? 'customer-service-price-option is-enabled' : 'customer-service-price-option'}>
      <Checkbox aria-label={`启用${label}价格`} checked={enabled}
        onChange={(event) => onChange?.(toggleCustomerPrice({ values, catalogUuid, basis, checked: event.target.checked }))} />
      <div className="customer-service-price-option__body">
        <strong>{label}</strong>
        <div className="customer-service-price-input">
          <InputNumber aria-label={`${label}价格`} disabled={!enabled} min={0.01} precision={2}
            value={current?.price || undefined}
            onChange={(price) => onChange?.(updateCustomerPrice({ values, catalogUuid, basis, changes: { price: price ?? 0 } }))} />
          <span>{unit}</span>
        </div>
      </div>
      <Radio aria-label={`${label}设为默认`} disabled={!enabled} checked={current?.isDefault === 1}
        onChange={() => onChange?.(updateCustomerPrice({ values, catalogUuid, basis, changes: { isDefault: 1 } }))}>
        默认
      </Radio>
    </div>
  )
}
