import { ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import { Button, Checkbox, Input, Space, Tooltip, Typography } from 'antd'
import type { DeliveryFinishScope } from './deliveryFinishScope'
import type { DeliveryFinishFilters } from './deliveryFinishFilter'
import DeliverySourceIssueFilter from './DeliverySourceIssueFilter'

interface Props {
  filters: DeliveryFinishFilters
  resultCount: number
  scope: DeliveryFinishScope
  totalCount: number
  onChange: (filters: DeliveryFinishFilters) => void
}

export default function DeliveryFinishTableToolbar(props: Props) {
  const scopeName = props.scope === 'remain' ? '余料' : '成品'
  const filtered = props.resultCount !== props.totalCount
  const sourceIssue = props.filters.sourceIssue ?? 'all'
  return (
    <div className="delivery-finish-toolbar">
      <Input
        allowClear
        className="delivery-finish-toolbar__search"
        placeholder="搜索成品卷、母卷、加工单或规格"
        prefix={<SearchOutlined />}
        value={props.filters.keyword}
        onChange={(event) => props.onChange({ ...props.filters, keyword: event.target.value })}
      />
      <Space size={12} wrap>
        <DeliverySourceIssueFilter
          value={sourceIssue}
          onChange={(sourceIssue) => props.onChange({ ...props.filters, sourceIssue })}
        />
        <Checkbox
          checked={props.filters.selectedOnly}
          onChange={(event) => props.onChange({ ...props.filters, selectedOnly: event.target.checked })}
        >
          仅看已选
        </Checkbox>
        <Typography.Text type="secondary">
          {filtered ? `显示 ${props.resultCount} / ${props.totalCount} 卷${scopeName}` : `共 ${props.totalCount} 卷${scopeName}`}
        </Typography.Text>
        <Tooltip title="清除筛选">
          <Button
            aria-label="清除筛选"
            icon={<ReloadOutlined />}
            disabled={!props.filters.keyword && sourceIssue === 'all' && !props.filters.selectedOnly}
            onClick={() => props.onChange({ keyword: '', selectedOnly: false, sourceIssue: 'all' })}
          />
        </Tooltip>
      </Space>
    </div>
  )
}
