import { FilterOutlined } from '@ant-design/icons'
import { Badge, Button, Dropdown } from 'antd'
import type { MenuProps } from 'antd'
import type { DeliverySourceIssueFilter as SourceIssue } from './deliveryFinishFilter'

const MENU_ITEMS: MenuProps['items'] = [
  { key: 'all', label: '全部来源状态' },
  { key: 'missingSource', label: '未关联母卷' },
  { key: 'missingIdentity', label: '母卷号待补' },
]

const FILTER_LABELS: Record<SourceIssue, string> = {
  all: '来源状态',
  missingIdentity: '母卷号待补',
  missingSource: '未关联母卷',
}

interface Props {
  value: SourceIssue
  onChange: (value: SourceIssue) => void
}

export default function DeliverySourceIssueFilter(props: Props) {
  const menu: MenuProps = {
    items: MENU_ITEMS,
    selectable: true,
    selectedKeys: [props.value],
    onClick: ({ key }) => {
      if (isSourceIssue(key)) props.onChange(key)
    },
  }
  const label = FILTER_LABELS[props.value]
  const accessibleValue = props.value === 'all' ? '全部' : label
  return (
    <Dropdown menu={menu} trigger={['click']}>
      <Badge dot={props.value !== 'all'} offset={[-3, 3]}>
        <Button
          className="delivery-source-issue-filter"
          aria-label={`来源状态筛选：${accessibleValue}`}
          icon={<FilterOutlined />}
        >
          {label}
        </Button>
      </Badge>
    </Dropdown>
  )
}

function isSourceIssue(value: string): value is SourceIssue {
  return value === 'all' || value === 'missingIdentity' || value === 'missingSource'
}
