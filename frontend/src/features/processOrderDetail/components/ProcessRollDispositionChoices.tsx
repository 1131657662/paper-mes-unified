import {
  ApartmentOutlined,
  CheckOutlined,
  ClearOutlined,
  CloseCircleOutlined,
  ScissorOutlined,
  SearchOutlined,
  SendOutlined,
} from '@ant-design/icons'
import { Button, Checkbox, Input, Radio, Space, Tag, Typography } from 'antd'
import { useState, type ReactNode } from 'react'
import type { ProcessRollDispositionAction } from '../../../types/processOrder'
import type { RollOption } from './processRollDispositionOptions'
import { filterRollOption } from './processRollDispositionOptions'
import './ProcessRollDispositionChoices.css'

const ACTION_OPTIONS: ReadonlyArray<ActionOption> = [
  {
    value: 'DIRECT_SHIP',
    label: '转直发',
    description: '本次不加工，现场称重后直接入库，不再走当前加工路线。',
    icon: <SendOutlined />,
    tone: 'blue',
  },
  {
    value: 'CANCEL',
    label: '取消本次加工',
    description: '不加工、不入库，母卷保留为已取消终态；支持批量选择。',
    icon: <CloseCircleOutlined />,
    tone: 'orange',
  },
  {
    value: 'SPLIT_TO_ORDER',
    label: '拆分代加工单',
    description: '转入新的待下发加工单，可继续编辑工艺并重新审核。',
    icon: <ScissorOutlined />,
    tone: 'purple',
  },
]

interface ActionOption {
  value: ProcessRollDispositionAction
  label: string
  description: string
  icon: ReactNode
  tone: 'blue' | 'orange' | 'purple'
}

interface RollSelectionListProps {
  options: RollOption[]
  multiple?: boolean
  value?: string[]
  onChange?: (value: string[]) => void
}

export function RollSelectionList({ options, multiple = true, value = [], onChange }: RollSelectionListProps) {
  const [query, setQuery] = useState('')
  const visibleOptions = options.filter((option) => filterRollOption(query, option))
  const selectedVisibleCount = visibleOptions.filter((option) => value.includes(option.value)).length
  const showToolbar = options.length > 3 || (multiple && options.length > 1)

  const changeSelection = (checkedValues: Array<string | number | boolean>) => {
    const values = checkedValues.filter((item): item is string => typeof item === 'string')
    onChange?.(multiple ? values : values.slice(-1))
  }

  const selectVisible = () => {
    const next = new Set(value)
    visibleOptions.forEach((option) => next.add(option.value))
    onChange?.([...next])
  }

  return (
    <div className="process-roll-disposition__selection">
      {showToolbar ? <div className="process-roll-disposition__selection-toolbar">
        {options.length > 3 ? <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="搜索卷号、编号、批次或规格"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        /> : null}
        {multiple ? (
          <Space size={4} className="process-roll-disposition__selection-actions">
            <Button
              type="link"
              size="small"
              icon={<CheckOutlined />}
              disabled={!visibleOptions.length || selectedVisibleCount === visibleOptions.length}
              onClick={selectVisible}
            >
              全选当前
            </Button>
            <Button
              type="link"
              size="small"
              icon={<ClearOutlined />}
              disabled={!value.length}
              onClick={() => onChange?.([])}
            >
              清空
            </Button>
          </Space>
        ) : null}
      </div> : null}
      <Checkbox.Group aria-label="可处置母卷" value={value} onChange={changeSelection} className="process-roll-disposition__roll-list">
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          {visibleOptions.length ? visibleOptions.map((option) => (
            <Checkbox key={option.value} value={option.value} className="process-roll-disposition__roll-option">
              <RollOptionContent option={option} />
            </Checkbox>
          )) : (
            <Typography.Text type="secondary" className="process-roll-disposition__empty-search">
              未找到匹配的母卷
            </Typography.Text>
          )}
        </Space>
      </Checkbox.Group>
    </div>
  )
}

interface ActionSelectionProps {
  selectedCount?: number
  value?: ProcessRollDispositionAction
  onChange?: (value: ProcessRollDispositionAction) => void
}

export function ActionSelection({ selectedCount = 0, value, onChange }: ActionSelectionProps) {
  const singleRollOnly = selectedCount > 1
  return (
    <Radio.Group aria-label="母卷处置方式" value={value} onChange={(event) => onChange?.(event.target.value)} className="process-roll-disposition__action-list">
      <div className="process-roll-disposition__action-grid">
        {ACTION_OPTIONS.map((option) => {
          const disabled = singleRollOnly && option.value !== 'CANCEL'
          return (
            <Radio
              key={option.value}
              value={option.value}
              disabled={disabled}
              className={`process-roll-disposition__action-option process-roll-disposition__action-option--${option.tone}`}
            >
              <span className="process-roll-disposition__action-content">
                <span className="process-roll-disposition__action-title">
                  <span className="process-roll-disposition__action-icon" aria-hidden="true">{option.icon}</span>
                  <strong>{option.label}</strong>
                </span>
                <Typography.Text type="secondary">{disabled ? '请先保留 1 卷，再使用该操作。' : option.description}</Typography.Text>
              </span>
            </Radio>
          )
        })}
      </div>
    </Radio.Group>
  )
}

function RollOptionContent({ option }: { option: RollOption }) {
  return (
    <span className="process-roll-disposition__roll-content">
      <span className="process-roll-disposition__roll-head">
        <span className="process-roll-disposition__roll-title">
          <strong>母卷 {option.sequence}</strong>
          <Typography.Text type="secondary">{option.identity}</Typography.Text>
        </span>
        <Tag color={option.statusLabel === '加工中' ? 'orange' : 'blue'}>{option.statusLabel}</Tag>
      </span>
      <span className="process-roll-disposition__roll-paper">
        <ApartmentOutlined aria-hidden="true" />
        <strong>{option.paperName}</strong>
      </span>
      <span className="process-roll-disposition__roll-fields">
        {option.fields.map((field) => (
          <span key={field.label} className="process-roll-disposition__roll-field">
            <small>{field.label}</small>
            <span>{field.value}</span>
          </span>
        ))}
      </span>
    </span>
  )
}
