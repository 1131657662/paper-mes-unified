import { Button, Table, Tag, Typography } from 'antd'
import { ToolOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { DataHealthIssue } from '../../types/dataHealth'

interface Props {
  issues: DataHealthIssue[]
  loading: boolean
  onRepair: (issue: DataHealthIssue) => void
}

export default function DataHealthIssueTable({ issues, loading, onRepair }: Props) {
  const columns: ColumnsType<DataHealthIssue> = [
    {
      title: '级别', dataIndex: 'severity', width: 86,
      render: (value) => value === 'CRITICAL' ? <Tag color="error">严重</Tag> : <Tag color="warning">警告</Tag>,
    },
    { title: '业务类型', dataIndex: 'businessType', width: 100 },
    {
      title: '业务单号', dataIndex: 'businessNo', width: 180,
      render: (value) => <Typography.Text strong copyable={Boolean(value)}>{value || '-'}</Typography.Text>,
    },
    { title: '异常', dataIndex: 'title', width: 220 },
    { title: '检查结果', dataIndex: 'detail', ellipsis: { showTitle: false } },
    {
      title: '操作', key: 'action', fixed: 'right', width: 100,
      render: (_, issue) => issue.repairAction ? (
        <Button danger type="link" icon={<ToolOutlined />} onClick={() => onRepair(issue)}>
          {issue.repairAction === 'OPEN_INVENTORY_WAREHOUSE_REPAIR' ? '去库存治理' : '修复'}
        </Button>
      ) : <Typography.Text type="secondary">人工核对</Typography.Text>,
    },
  ]
  return (
    <Table
      rowKey={(issue) => `${issue.issueType}-${issue.businessUuid}`}
      columns={columns}
      dataSource={issues}
      loading={loading}
      pagination={{ pageSize: 20, showSizeChanger: true }}
      scroll={{ x: 1050, y: 'calc(100vh - 390px)' }}
      size="small"
    />
  )
}
