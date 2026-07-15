import type { AvailableFinishVO } from '../../types/delivery'

export interface DeliveryFinishFilters {
  keyword: string
  selectedOnly: boolean
  sourceIssue: DeliverySourceIssueFilter
}

export type DeliverySourceIssueFilter = 'all' | 'missingIdentity' | 'missingSource'

export const defaultDeliveryFinishFilters: DeliveryFinishFilters = {
  keyword: '',
  selectedOnly: false,
  sourceIssue: 'all',
}

export function filterDeliveryFinishes(
  finishes: AvailableFinishVO[],
  filters: DeliveryFinishFilters,
  selectedRowKeys: React.Key[] = [],
): AvailableFinishVO[] {
  const keyword = normalizeSearchText(filters.keyword)
  const sourceIssue = filters.sourceIssue ?? 'all'
  const selectedKeys = new Set(selectedRowKeys.map(String))
  return finishes.filter((finish) => {
    if (sourceIssue === 'missingSource' && !hasMissingMotherRollSource(finish)) return false
    if (sourceIssue === 'missingIdentity' && !hasMissingMotherRollIdentity(finish)) return false
    if (filters.selectedOnly && !selectedKeys.has(finish.finishUuid)) return false
    return !keyword || searchableText(finish).includes(keyword)
  })
}

export function hasMissingMotherRollSource(finish: AvailableFinishVO): boolean {
  return finish.sourceType !== 2 && (finish.sourceMotherRolls?.length ?? 0) === 0
}

export function hasMissingMotherRollIdentity(finish: AvailableFinishVO): boolean {
  const sources = finish.sourceMotherRolls ?? []
  return sources.length > 0 && sources.some((source) => !source.rollNo && !source.extraNo)
}

function searchableText(finish: AvailableFinishVO): string {
  const sources = finish.sourceMotherRolls ?? []
  const text = [
    finish.finishRollNo,
    finish.orderNo,
    finish.paperName,
    finish.gramWeight,
    finish.finishWidth,
    finish.originalRollNos,
    ...sources.flatMap((source) => [
      source.rowSort ? `母卷 ${source.rowSort}` : undefined,
      source.rollNo,
      source.rollNo ? `卷号 ${source.rollNo}` : undefined,
      source.extraNo,
      source.extraNo ? `编号 ${source.extraNo}` : undefined,
      source.paperName,
      source.gramWeight,
      source.originalWidth,
    ]),
  ].filter((value) => value != null).join(' ')
  return normalizeSearchText(text)
}

function normalizeSearchText(value: string): string {
  return value.toLocaleLowerCase().replace(/[\s\-_/]+/g, '')
}
