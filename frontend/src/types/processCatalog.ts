export interface ProcessCatalogUnit {
  code: string
  name: string
  defaultUnit: boolean
}

export type ProcessPricingStrategy = 'SAW_KNIFE' | 'REWIND_WEIGHT' | 'SERVICE_QUANTITY'
export type ProcessBillingMode = 1 | 2 | 3 | 4

export interface ProcessCatalog {
  uuid: string
  stepType: number
  code: string
  name: string
  category: 'PRODUCTION' | 'SERVICE' | 'QUALITY' | 'PACKAGING' | 'LOGISTICS'
  pricingStrategy: ProcessPricingStrategy
  producesInventoryOutput: boolean
  allowsLossRecording: boolean
  allowsMainProcess: boolean
  units: ProcessCatalogUnit[]
  billingModes: ProcessBillingMode[]
}
