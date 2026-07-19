import { SearchOutlined } from '@ant-design/icons'
import { Button, Checkbox, Empty, Input } from 'antd'
import { useState } from 'react'
import type { FilterDropdownProps } from 'antd/es/table/interface'
import type { OriginalRoll } from '../../../types/processOrder'
import './RollFilterDropdown.css'

interface Props extends Pick<FilterDropdownProps, 'selectedKeys' | 'setSelectedKeys' | 'confirm' | 'clearFilters'> {
  rolls: OriginalRoll[]
}

interface OptionProps {
  checked: boolean
  roll: OriginalRoll
  onChange: (checked: boolean) => void
}

export default function RollFilterDropdown({ rolls, selectedKeys, setSelectedKeys, confirm, clearFilters }: Props) {
  const [query, setQuery] = useState('')
  const selected = new Set(selectedKeys.map(String))
  const visibleRolls = rolls.filter((roll) => rollMatchesSearch(roll, query))
  const selectVisible = () => setSelectedKeys(mergeVisibleKeys(selectedKeys, visibleRolls))

  return (
    <div className="roll-filter-dropdown">
      <div className="roll-filter-dropdown__header">
        <Input allowClear value={query} prefix={<SearchOutlined />} placeholder="搜索卷序、卷号、编号或品名"
          onChange={(event) => setQuery(event.target.value)} />
        <div className="roll-filter-dropdown__summary">
          <span>共 {rolls.length} 卷，已选 {selected.size} 卷</span>
          <Button type="link" size="small" disabled={visibleRolls.length === 0} onClick={selectVisible}>全选当前结果</Button>
        </div>
      </div>
      <div className="roll-filter-dropdown__list">
        {visibleRolls.length === 0 ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="没有匹配的母卷" />
          : visibleRolls.map((roll) => (
            <RollFilterOption key={roll.uuid} roll={roll} checked={selected.has(roll.uuid)}
              onChange={(checked) => setSelectedKeys(toggleRollKey(selectedKeys, roll.uuid, checked))} />
          ))}
      </div>
      <div className="roll-filter-dropdown__footer">
        <Button size="small" disabled={selectedKeys.length === 0}
          onClick={() => clearFilters?.({ confirm: true, closeDropdown: false })}>清空筛选</Button>
        <Button size="small" type="primary" onClick={() => confirm({ closeDropdown: true })}>应用筛选</Button>
      </div>
    </div>
  )
}

function rollMatchesSearch(roll: OriginalRoll, query: string): boolean {
  const normalizedQuery = query.trim().toLocaleLowerCase()
  return !normalizedQuery || rollSearchText(roll).includes(normalizedQuery)
}

function RollFilterOption({ checked, roll, onChange }: OptionProps) {
  return (
    <Checkbox className="roll-filter-option" checked={checked} onChange={(event) => onChange(event.target.checked)}>
      <span className="roll-filter-option__content">
        <span className="roll-filter-option__top">
          <strong>第 {rollSequence(roll)} 卷</strong>
          <span>{rollIdentifiers(roll)}</span>
        </span>
        <span className="roll-filter-option__meta">{rollMetadata(roll)}</span>
      </span>
    </Checkbox>
  )
}

function toggleRollKey(keys: React.Key[], uuid: string, checked: boolean): React.Key[] {
  if (checked) return keys.includes(uuid) ? keys : [...keys, uuid]
  return keys.filter((key) => String(key) !== uuid)
}

function mergeVisibleKeys(keys: React.Key[], rolls: OriginalRoll[]): React.Key[] {
  return [...new Set([...keys.map(String), ...rolls.map((roll) => roll.uuid)])]
}

function rollSearchText(roll: OriginalRoll): string {
  return [rollSequence(roll), roll.rollNo, roll.extraNo, roll.paperName, roll.batchNo,
    roll.gramWeight, roll.actualGramWeight, roll.originalWidth, roll.actualWidth]
    .filter((value) => value != null).join(' ').toLocaleLowerCase()
}

function rollSequence(roll: OriginalRoll): string {
  return roll.rowSort == null ? '-' : String(roll.rowSort).padStart(2, '0')
}

function rollIdentifiers(roll: OriginalRoll): string {
  return `原始卷号 ${roll.rollNo || '-'} · 编号 ${roll.extraNo || '-'}`
}

function rollMetadata(roll: OriginalRoll): string {
  const gramWeight = roll.actualGramWeight ?? roll.gramWeight
  const width = roll.actualWidth ?? roll.originalWidth
  const weight = roll.actualWeight ?? roll.totalWeight ?? roll.rollWeight
  return [roll.paperName || '品名待补充', gramWeight == null ? undefined : `${gramWeight}g`,
    width == null ? undefined : `${width}mm`, weight == null ? undefined : `来料 ${(weight / 1000).toFixed(3)} t`,
    roll.batchNo ? `批次 ${roll.batchNo}` : undefined]
    .filter(Boolean).join(' · ')
}
