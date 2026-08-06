import type { SorterResult } from 'antd/es/table/interface'
import type { FinishSourceVO } from '../../../types/processOrder'
import type { FinishCustomerSpec } from '../../processOrderCustomerSpec/customerSpecTypes'
import { compareValues, isEmpty } from '../../../pages/delivery/deliveryDetailSorting'
import type { CustomerSpecificationGroup } from '../../processOrderCustomerSpec/customerSpecModel'
import type { FinishedProductRow } from './finishedProductRows'
import type { PhysicalSpecificationGroup } from './physicalSpecificationModel'

export type FinishedProductsSortDirection = 'asc' | 'desc'
export interface FinishedProductsSortSpec<TField extends string> {
  field: TField
  direction: FinishedProductsSortDirection
}

export type CustomerSummarySortField = 'paperName' | 'gramWeight' | 'width' | 'count' | 'weight' | 'physicalSpecifications'
export type CustomerSummarySortSpec = FinishedProductsSortSpec<CustomerSummarySortField>
export type CustomerComparisonSortField = 'finishRollNo' | 'customerSpecification' | 'customerDisplayWeight' | 'sourceMotherRoll' | 'status'
export type CustomerComparisonSortSpec = FinishedProductsSortSpec<CustomerComparisonSortField>
export type PhysicalSummarySortField = 'paperName' | 'gramWeight' | 'width' | 'productType' | 'count' | 'recordedCount' | 'estimateWeight' | 'actualWeight' | 'difference'
export type PhysicalSummarySortSpec = FinishedProductsSortSpec<PhysicalSummarySortField>
export type PhysicalItemsSortField = 'finishRollNo' | 'specification' | 'sourceMotherRoll' | 'status' | 'estimateWeight' | 'actualWeight' | 'difference'
export type PhysicalItemsSortSpec = FinishedProductsSortSpec<PhysicalItemsSortField>

export interface CustomerSpecificationComparisonRow {
  row: FinishedProductRow
  spec: FinishCustomerSpec
}

export const CUSTOMER_SUMMARY_FIELDS = new Set<CustomerSummarySortField>(['paperName', 'gramWeight', 'width', 'count', 'weight', 'physicalSpecifications'])
export const CUSTOMER_COMPARISON_FIELDS = new Set<CustomerComparisonSortField>(['finishRollNo', 'customerSpecification', 'customerDisplayWeight', 'sourceMotherRoll', 'status'])
export const PHYSICAL_SUMMARY_FIELDS = new Set<PhysicalSummarySortField>(['paperName', 'gramWeight', 'width', 'productType', 'count', 'recordedCount', 'estimateWeight', 'actualWeight', 'difference'])
export const PHYSICAL_ITEMS_FIELDS = new Set<PhysicalItemsSortField>(['finishRollNo', 'specification', 'sourceMotherRoll', 'status', 'estimateWeight', 'actualWeight', 'difference'])

export const isCustomerSummarySortSpec = (value: unknown): value is CustomerSummarySortSpec => isSortSpec(value, CUSTOMER_SUMMARY_FIELDS)
export const isCustomerComparisonSortSpec = (value: unknown): value is CustomerComparisonSortSpec => isSortSpec(value, CUSTOMER_COMPARISON_FIELDS)
export const isPhysicalSummarySortSpec = (value: unknown): value is PhysicalSummarySortSpec => isSortSpec(value, PHYSICAL_SUMMARY_FIELDS)
export const isPhysicalItemsSortSpec = (value: unknown): value is PhysicalItemsSortSpec => isSortSpec(value, PHYSICAL_ITEMS_FIELDS)

export function sortCustomerSummaryRows(rows: CustomerSpecificationGroup[], chain: CustomerSummarySortSpec[]) { return sortRows(rows, chain, customerSummaryValue) }
export function sortCustomerComparisonRows(rows: CustomerSpecificationComparisonRow[], chain: CustomerComparisonSortSpec[]) { return sortRows(rows, chain, customerComparisonValue) }
export function sortPhysicalSummaryRows(rows: PhysicalSpecificationGroup[], chain: PhysicalSummarySortSpec[]) { return sortRows(rows, chain, physicalSummaryValue) }
export function sortPhysicalItemRows(rows: FinishedProductRow[], chain: PhysicalItemsSortSpec[]) { return sortRows(rows, chain, physicalItemsValue) }

export function updateCustomerSummarySortChain(current: CustomerSummarySortSpec[], sorter: SorterResult<CustomerSpecificationGroup> | SorterResult<CustomerSpecificationGroup>[]) { return updateSortChain(current, sorter, CUSTOMER_SUMMARY_FIELDS) }
export function updateCustomerComparisonSortChain(current: CustomerComparisonSortSpec[], sorter: SorterResult<CustomerSpecificationComparisonRow> | SorterResult<CustomerSpecificationComparisonRow>[]) { return updateSortChain(current, sorter, CUSTOMER_COMPARISON_FIELDS) }
export function updatePhysicalSummarySortChain(current: PhysicalSummarySortSpec[], sorter: SorterResult<PhysicalSpecificationGroup> | SorterResult<PhysicalSpecificationGroup>[]) { return updateSortChain(current, sorter, PHYSICAL_SUMMARY_FIELDS) }
export function updatePhysicalItemsSortChain(current: PhysicalItemsSortSpec[], sorter: SorterResult<FinishedProductRow> | SorterResult<FinishedProductRow>[]) { return updateSortChain(current, sorter, PHYSICAL_ITEMS_FIELDS) }

function isSortSpec<TField extends string>(value: unknown, fields: ReadonlySet<TField>): value is FinishedProductsSortSpec<TField> {
  if (!value || typeof value !== 'object') return false
  const item = value as Record<string, unknown>
  return typeof item.field === 'string' && fields.has(item.field as TField) && (item.direction === 'asc' || item.direction === 'desc')
}

function updateSortChain<TField extends string, TRow>(current: FinishedProductsSortSpec<TField>[], sorter: SorterResult<TRow> | SorterResult<TRow>[], fields: ReadonlySet<TField>) {
  const active = (Array.isArray(sorter) ? sorter : [sorter]).map((item) => toSortSpec(item, fields)).filter((item): item is FinishedProductsSortSpec<TField> => item !== null)
  if (!active.length) return []
  const activeByField = new Map(active.map((item) => [item.field, item.direction]))
  const next = current.filter((item) => activeByField.has(item.field)).map((item) => ({ ...item, direction: activeByField.get(item.field) ?? item.direction }))
  const currentFields = new Set(current.map((item) => item.field))
  active.forEach((item) => { if (!currentFields.has(item.field)) next.push(item) })
  return next
}

function toSortSpec<TField extends string, TRow>(value: SorterResult<TRow>, fields: ReadonlySet<TField>) {
  const field = value.field ?? value.columnKey
  if (typeof field !== 'string' || !fields.has(field as TField)) return null
  if (value.order !== 'ascend' && value.order !== 'descend') return null
  return { field: field as TField, direction: value.order === 'ascend' ? 'asc' as const : 'desc' as const }
}

function sortRows<TRow, TField extends string>(rows: TRow[], chain: FinishedProductsSortSpec<TField>[], getValue: (row: TRow, field: TField) => string | number | undefined) {
  if (!chain.length) return rows
  return rows.map((row, index) => ({ row, index })).sort((left, right) => {
    for (const spec of chain) {
      const leftValue = getValue(left.row, spec.field)
      const rightValue = getValue(right.row, spec.field)
      const result = compareValues(leftValue, rightValue)
      if (isEmpty(leftValue) || isEmpty(rightValue)) { if (result !== 0) return result; continue }
      if (result !== 0) return spec.direction === 'asc' ? result : -result
    }
    return left.index - right.index
  }).map(({ row }) => row)
}

function customerSummaryValue(row: CustomerSpecificationGroup, field: CustomerSummarySortField) {
  if (field === 'physicalSpecifications') return row.physicalSpecifications.join(' / ')
  return row[field]
}

function customerComparisonValue(item: CustomerSpecificationComparisonRow, field: CustomerComparisonSortField) {
  if (field === 'customerSpecification') return customerSpecText(item.spec)
  if (field === 'sourceMotherRoll') return sourceText(item.row.sources)
  if (field === 'status') return item.spec.specificationChanged || item.spec.weightChanged ? 1 : 0
  if (field === 'finishRollNo') return item.spec.finishRollNo
  return item.spec.customerDisplayWeight
}

function physicalSummaryValue(row: PhysicalSpecificationGroup, field: PhysicalSummarySortField) {
  return row[field]
}

function physicalItemsValue(row: FinishedProductRow, field: PhysicalItemsSortField) {
  const finish = row.finish
  if (field === 'finishRollNo') return finish.finishRollNo
  if (field === 'specification') return [finish.paperName, finish.gramWeight, finish.finishWidth, finish.finishDiameter, finish.finishCoreDiameter].map(value => value ?? '').join('/')
  if (field === 'sourceMotherRoll') return sourceText(row.sources)
  if (field === 'status') return [finish.isRemain, finish.isSpare, finish.rollNoStatus, finish.finishStatus].map(value => value ?? '').join('/')
  if (field === 'estimateWeight') return finish.estimateWeight
  if (field === 'actualWeight') return finish.actualWeight
  if (field === 'difference') {
    if (finish.actualWeight == null || finish.estimateWeight == null) return undefined
    return finish.actualWeight - finish.estimateWeight
  }
  return undefined
}

function customerSpecText(spec: FinishCustomerSpec) { return [spec.customerPaperName, spec.customerGramWeight, spec.customerFinishWidth].map(value => value ?? '').join('/') }
function sourceText(sources: FinishSourceVO[]) { return sources.map((source) => source.rollNo ?? source.paperName ?? source.extraNo ?? '').join(' / ') }
