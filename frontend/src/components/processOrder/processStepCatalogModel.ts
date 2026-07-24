import type { ProcessStepDTO } from '../../api/processOrder'
import type { ProcessCatalog } from '../../types/processCatalog'
import type { CustomerProcessPrice } from '../../types/customer'

export type ProcessStepFormValues = Omit<ProcessStepDTO, 'originalUuid' | 'stepType' | 'billingMode'> & {
  originalUuid?: string
  originalUuids?: string[]
  stepType?: number
  billingMode?: number
  fixedAmountScope?: 'TOTAL' | 'EACH'
}

interface TypeChangeOptions {
  catalog?: ProcessCatalog
  currentBillingMode?: number
  currentUnit?: string
  currentIsMain?: number
}

interface PayloadOptions {
  values: ProcessStepFormValues
  catalog?: ProcessCatalog
  extraOnly: boolean
}

export function findProcessCatalog(
  catalogs: ProcessCatalog[] | undefined,
  stepType: number | undefined,
): ProcessCatalog | undefined {
  return catalogs?.find((catalog) => catalog.stepType === stepType)
}

export function serviceStepCatalogs(
  catalogs: ProcessCatalog[] | undefined,
  extraOnly: boolean,
): ProcessCatalog[] | undefined {
  if (!extraOnly) return catalogs
  return catalogs?.filter((catalog) => catalog.pricingStrategy === 'SERVICE_QUANTITY')
}

export function customerPriceFormPatch(options: {
  catalog?: ProcessCatalog
  prices?: CustomerProcessPrice[]
  billingMode?: number
  billingBasis?: string
  preferDefault?: boolean
}): Partial<ProcessStepDTO> {
  const { catalog, prices = [], preferDefault = false } = options
  if (!catalog || catalog.pricingStrategy !== 'SERVICE_QUANTITY') return {}
  const catalogPrices = prices.filter((price) => price.catalogUuid === catalog.uuid)
  const price = preferDefault
    ? catalogPrices.find((item) => item.defaultOption) ?? catalogPrices[0]
    : priceForSelection(catalogPrices, options.billingMode, options.billingBasis)
  if (!price) return selectionFallback(catalog, options.billingMode, options.billingBasis)
  if (price.billingBasis === 'FIXED') {
    return { billingMode: 3, billingBasis: undefined, billingAmount: price.price, unitPrice: undefined }
  }
  return {
    billingMode: 1,
    billingBasis: price.billingBasis,
    unitPrice: price.price,
    billingAmount: undefined,
  }
}

function priceForSelection(prices: CustomerProcessPrice[], billingMode?: number, billingBasis?: string) {
  if (billingMode === 3) return prices.find((price) => price.billingBasis === 'FIXED')
  if (billingMode === 1) {
    return prices.find((price) => price.billingBasis === billingBasis)
      ?? prices.find((price) => price.billingBasis !== 'FIXED')
  }
  return undefined
}

function selectionFallback(catalog: ProcessCatalog, billingMode?: number, billingBasis?: string) {
  if (billingMode === 3) return { billingBasis: undefined, unitPrice: undefined }
  if (billingMode === 4) {
    return { billingBasis: undefined, unitPrice: undefined, billingAmount: undefined }
  }
  if (billingMode === 0) return { billingBasis: billingBasis ?? defaultBillingBasis(catalog) }
  if (billingMode !== 1) return {}
  const supported = catalog.units.some((unit) => unit.code === billingBasis)
  const defaultUnit = catalog.units.find((unit) => unit.defaultUnit) ?? catalog.units[0]
  return { billingBasis: supported ? billingBasis : defaultUnit?.code }
}

export function processTypeDefaults(options: TypeChangeOptions): Partial<ProcessStepFormValues> {
  const { catalog, currentBillingMode, currentUnit, currentIsMain } = options
  if (!catalog) return {}
  const defaultUnit = catalog.units.find((unit) => unit.defaultUnit) ?? catalog.units[0]
  const supportsMode = currentBillingMode === 0
    || Boolean(currentBillingMode && catalog.billingModes.some((mode) => mode === currentBillingMode))
  const supportsUnit = catalog.units.some((unit) => unit.code === currentUnit)
  return {
    isMain: catalog.allowsMainProcess ? (currentIsMain ?? 0) : 0,
    billingMode: supportsMode ? currentBillingMode : catalog.pricingStrategy === 'SERVICE_QUANTITY' ? 0 : catalog.billingModes[0],
    billingBasis: supportsUnit ? currentUnit : defaultUnit?.code,
  }
}

export function processStepPayload(options: PayloadOptions): ProcessStepDTO {
  const { values, catalog, extraOnly } = options
  const {
    originalUuid,
    originalUuids: _originalUuids,
    stepType,
    fixedAmountScope: _fixedAmountScope,
    ...formValues
  } = values
  if (!originalUuid || stepType == null) {
    throw new Error('工序必要字段尚未填写')
  }
  const completeValues: ProcessStepDTO = { ...formValues, originalUuid, stepType, billingMode: normalizeBillingMode(values.billingMode) }
  const payload = extraOnly ? { ...completeValues, isMain: 0 } : completeValues
  if (catalog?.pricingStrategy === 'SERVICE_QUANTITY') {
    return {
      ...payload,
      serviceQuantity: undefined,
      unitPrice: values.billingMode === 0 ? undefined : payload.unitPrice,
    }
  }
  return {
    ...payload,
    billingBasis: undefined,
    serviceQuantity: undefined,
    billingMode: undefined,
    billingAmount: undefined,
  }
}

function normalizeBillingMode(mode?: number): ProcessStepDTO['billingMode'] {
  if (mode === 0) return 1
  if (mode === 1 || mode === 2 || mode === 3 || mode === 4) return mode
  return undefined
}

export function processStepInitialValues(options: {
  initialValues?: ProcessStepDTO & { uuid?: string }
  defaultOriginalUuid?: string
  defaultOriginalUuids?: string[]
}): ProcessStepFormValues {
  if (options.initialValues) return pendingPricingValues(options.initialValues)
  return {
    ...(options.defaultOriginalUuid ? { originalUuid: options.defaultOriginalUuid } : {}),
    ...(options.defaultOriginalUuids ? { originalUuids: options.defaultOriginalUuids } : {}),
    isMain: 0,
    billingMode: 0,
    fixedAmountScope: 'TOTAL',
  }
}

function pendingPricingValues(values: ProcessStepDTO & { uuid?: string }): ProcessStepFormValues {
  const pending = values.billingMode === 1 && values.unitPrice == null
  return { ...values, billingMode: pending ? 0 : values.billingMode, fixedAmountScope: 'TOTAL' }
}

function defaultBillingBasis(catalog: ProcessCatalog) {
  return (catalog.units.find((unit) => unit.defaultUnit) ?? catalog.units[0])?.code
}
