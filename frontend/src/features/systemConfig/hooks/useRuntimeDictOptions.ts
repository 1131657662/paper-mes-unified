import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { RuntimeDictOption } from '../../../types/systemConfig'

export interface DictSelectOption {
  label: string
  value: number | string
  code: string
  remark?: string
}

export function useRuntimeDictOptions(types: string[]) {
  return useQuery({
    ...queries.systemConfig.runtimeDictOptions(types),
    enabled: types.length > 0,
    select: groupOptionsByType,
    staleTime: 5 * 60 * 1000,
  })
}

export function useDictOptions(dictType: string, fallback: DictSelectOption[] = []) {
  const query = useRuntimeDictOptions([dictType])
  const options = query.data?.[dictType] ?? fallback
  return { ...query, options }
}

export function useNumberDictOptions(dictType: string, fallback: DictSelectOption[] = []) {
  const query = useDictOptions(dictType, fallback)
  const options = query.options.filter((item) => typeof item.value === 'number')
  return { ...query, options }
}

function groupOptionsByType(items: RuntimeDictOption[]) {
  return items.reduce<Record<string, DictSelectOption[]>>((acc, item) => {
    const option = toSelectOption(item)
    acc[item.dictType] = [...(acc[item.dictType] ?? []), option]
    return acc
  }, {})
}

function toSelectOption(item: RuntimeDictOption): DictSelectOption {
  return {
    code: item.itemCode,
    label: item.itemName,
    remark: item.remark,
    value: item.itemValue ?? item.itemCode,
  }
}
