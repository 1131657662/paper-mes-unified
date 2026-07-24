import type { ProcessCatalog } from '../../types/processCatalog'
import type {
  CustomerProcessPriceBasis,
  CustomerProcessPriceSaveDTO,
} from '../../types/customer'

export const SERVICE_PRICE_BASES: Array<{
  basis: CustomerProcessPriceBasis
  label: string
  unit: string
}> = [
  { basis: 'PIECE', label: '按件', unit: '元/件' },
  { basis: 'TON', label: '按吨', unit: '元/吨' },
  { basis: 'FIXED', label: '固定金额', unit: '元/单' },
]

export function servicePriceCatalogs(catalogs: ProcessCatalog[]): ProcessCatalog[] {
  return catalogs.filter((catalog) => catalog.pricingStrategy === 'SERVICE_QUANTITY')
}

export function toggleCustomerPrice(options: {
  values: CustomerProcessPriceSaveDTO[]
  catalogUuid: string
  basis: CustomerProcessPriceBasis
  checked: boolean
}): CustomerProcessPriceSaveDTO[] {
  const { values, catalogUuid, basis, checked } = options
  if (!checked) return removePrice(values, catalogUuid, basis)
  if (findPrice(values, catalogUuid, basis)) return values
  const hasDefault = values.some((item) => item.catalogUuid === catalogUuid && item.isDefault === 1)
  return [...values, { catalogUuid, billingBasis: basis, price: 0, isDefault: hasDefault ? 0 : 1 }]
}

export function updateCustomerPrice(options: {
  values: CustomerProcessPriceSaveDTO[]
  catalogUuid: string
  basis: CustomerProcessPriceBasis
  changes: Partial<Pick<CustomerProcessPriceSaveDTO, 'price' | 'isDefault'>>
}): CustomerProcessPriceSaveDTO[] {
  const { values, catalogUuid, basis, changes } = options
  return values.map((item) => {
    if (item.catalogUuid !== catalogUuid) return item
    const selected = item.billingBasis === basis
    const isDefault = changes.isDefault === 1 ? (selected ? 1 : 0) : item.isDefault
    return selected ? { ...item, ...changes, isDefault } : { ...item, isDefault }
  })
}

export function findPrice(
  values: CustomerProcessPriceSaveDTO[],
  catalogUuid: string,
  basis: CustomerProcessPriceBasis,
): CustomerProcessPriceSaveDTO | undefined {
  return values.find((item) => item.catalogUuid === catalogUuid && item.billingBasis === basis)
}

function removePrice(
  values: CustomerProcessPriceSaveDTO[],
  catalogUuid: string,
  basis: CustomerProcessPriceBasis,
): CustomerProcessPriceSaveDTO[] {
  const removed = findPrice(values, catalogUuid, basis)
  const remaining = values.filter((item) => item !== removed)
  if (removed?.isDefault !== 1) return remaining
  const replacement = remaining.find((item) => item.catalogUuid === catalogUuid)
  return remaining.map((item) => item === replacement ? { ...item, isDefault: 1 } : item)
}
