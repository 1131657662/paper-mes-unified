import { Select } from 'antd'
import { useDeferredValue, useState } from 'react'
import type { Customer } from '../../../types/customer'
import { useReportCustomerCandidates } from '../hooks/useReportCustomerCandidates'
import { mergeCandidateOptions } from './reportCandidateOptions'

interface Props {
  id?: string
  initial: Customer[]
  loading: boolean
  onChange?: (value?: string) => void
  value?: string
}

export default function ReportCustomerFilter({ id, initial, loading, onChange, value }: Props) {
  const [search, setSearch] = useState('')
  const keyword = useDeferredValue(search.trim())
  const { data, isError, isFetching } = useReportCustomerCandidates(keyword)
  const options = mergeCandidateOptions(customerOptions(initial), customerOptions(data?.records ?? []))
  const changeValue = (next?: string) => {
    setSearch('')
    onChange?.(next)
  }
  return <Select allowClear showSearch aria-label="客户" filterOption={false} id={id}
    value={value} onChange={changeValue}
    loading={loading || isFetching} searchValue={search} onSearch={setSearch} optionFilterProp="label"
    notFoundContent={candidateState(keyword, isFetching, isError)}
    placeholder="全部客户" options={options} />
}

function customerOptions(items: Customer[]) {
  return items.map((item) => ({ value: item.uuid, label: item.customerName }))
}

function candidateState(keyword: string, isFetching: boolean, isError: boolean) {
  if (!keyword) return '请输入客户名称'
  if (isFetching) return '正在搜索…'
  if (isError) return '候选加载失败，请重试'
  return '暂无匹配客户'
}
