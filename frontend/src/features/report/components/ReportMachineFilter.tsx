import { Select } from 'antd'
import { useDeferredValue, useState } from 'react'
import type { Machine } from '../../../types/machine'
import { useReportMachineCandidates } from '../hooks/useReportMachineCandidates'
import { mergeCandidateOptions } from './reportCandidateOptions'

interface Props {
  id?: string
  initial: Machine[]
  onChange?: (value?: string) => void
  value?: string
}

export default function ReportMachineFilter({ id, initial, onChange, value }: Props) {
  const [search, setSearch] = useState('')
  const keyword = useDeferredValue(search.trim())
  const { data, isError, isFetching } = useReportMachineCandidates(keyword)
  const options = mergeCandidateOptions(machineOptions(initial), machineOptions(data?.records ?? []))
  const changeValue = (next?: string) => {
    setSearch('')
    onChange?.(next)
  }
  return <Select allowClear showSearch aria-label="机台" filterOption={false} id={id}
    value={value} onChange={changeValue}
    loading={isFetching} searchValue={search} onSearch={setSearch} optionFilterProp="label"
    notFoundContent={candidateState(keyword, isFetching, isError)}
    placeholder="全部机台" options={options} />
}

function machineOptions(items: Machine[]) {
  return items.map((item) => ({ value: item.uuid, label: item.machineName }))
}

function candidateState(keyword: string, isFetching: boolean, isError: boolean) {
  if (!keyword) return '请输入机台名称'
  if (isFetching) return '正在搜索…'
  if (isError) return '候选加载失败，请重试'
  return '暂无匹配机台'
}
