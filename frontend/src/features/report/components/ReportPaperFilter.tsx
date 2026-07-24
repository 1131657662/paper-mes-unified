import { Select } from 'antd'
import { useDeferredValue, useState } from 'react'
import type { Paper } from '../../../types/paper'
import { useReportPaperCandidates } from '../hooks/useReportPaperCandidates'
import { mergeCandidateOptions } from './reportCandidateOptions'

interface Props {
  id?: string
  initial: Paper[]
  onChange?: (value?: string) => void
  value?: string
}

export default function ReportPaperFilter({ id, initial, onChange, value }: Props) {
  const [search, setSearch] = useState('')
  const keyword = useDeferredValue(search.trim())
  const { data, isError, isFetching } = useReportPaperCandidates(keyword)
  const options = mergeCandidateOptions(paperOptions(initial), paperNameOptions(data ?? []))
  const changeValue = (next?: string) => {
    setSearch('')
    onChange?.(next)
  }
  return <Select allowClear showSearch aria-label="产品 / 品名" filterOption={false} id={id}
    value={value} onChange={changeValue}
    loading={isFetching} searchValue={search} onSearch={setSearch} optionFilterProp="label"
    notFoundContent={candidateState(keyword, isFetching, isError)}
    placeholder="全部产品" options={options} />
}

function paperOptions(items: Paper[]) {
  return items.map((item) => ({ value: item.paperName, label: item.paperName }))
}

function paperNameOptions(items: string[]) {
  return items.map((paperName) => ({ value: paperName, label: paperName }))
}

function candidateState(keyword: string, isFetching: boolean, isError: boolean) {
  if (!keyword) return '请输入品名'
  if (isFetching) return '正在搜索…'
  if (isError) return '候选加载失败，请重试'
  return '暂无匹配品名'
}
