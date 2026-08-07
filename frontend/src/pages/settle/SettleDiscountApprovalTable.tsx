import { CheckOutlined, CloseOutlined, EyeOutlined, StopOutlined } from '@ant-design/icons'
import { Button, Popconfirm, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { formatDateTime } from '../../utils/dateTime'
import { formatNumber } from '../../utils/numberFormatters'
import type { SettleDiscountApproval } from '../../types/settle'
import { approvalStatusColor, approvalStatusText, DISCOUNT_APPROVAL_STATUS } from './discountApprovalModel'

interface Props {
  canApproveAdmin: boolean
  canApproveFinance: boolean
  currentUserUuid?: string
  data: SettleDiscountApproval[]
  loading: boolean
  mutationLoading: boolean
  onApprove: (record: SettleDiscountApproval) => void
  onCancel: (record: SettleDiscountApproval) => void
  onOpen: (record: SettleDiscountApproval) => void
  onReject: (record: SettleDiscountApproval) => void
}

export default function SettleDiscountApprovalTable(props: Props) {
  return <Table<SettleDiscountApproval> rowKey="uuid" size="middle" pagination={false}
    loading={props.loading} dataSource={props.data} columns={columns(props)}
    scroll={{ x: 1220 }} locale={{ emptyText: '当前范围没有优惠审批记录' }} />
}

function columns(props: Props): ColumnsType<SettleDiscountApproval> {
  return [
    {
      title: '结算单', dataIndex: 'settleNo', width: 190, fixed: 'left',
      render: (_, row) => <div className="discount-approval-document">
        <Button type="link" onClick={() => props.onOpen(row)}>{row.settleNo ?? row.settleUuid}</Button>
        <Typography.Text type="secondary" ellipsis>{row.customerName || '-'}</Typography.Text>
      </div>,
    },
    {
      title: '本次收款方案', key: 'plan', width: 250,
      render: (_, row) => <div className="discount-approval-plan">
        <span>到账 ¥{formatNumber(row.cashAmount, 2)}</span>
        <span>废纸 ¥{formatNumber(row.scrapOffsetAmount, 2)}</span>
        <strong>优惠 ¥{formatNumber(row.discountAmount, 2)}</strong>
      </div>,
    },
    {
      title: '审批级别', dataIndex: 'requiredLevel', width: 118,
      render: (value: SettleDiscountApproval['requiredLevel']) =>
        <Tag color={value === 'ADMIN' ? 'volcano' : 'blue'}>{value === 'ADMIN' ? '管理员审批' : '财务复核'}</Tag>,
    },
    {
      title: '占未收比例', dataIndex: 'discountPercent', width: 110, align: 'right',
      render: (value: number) => `${formatNumber(value, 2)}%`,
    },
    { title: '优惠原因', dataIndex: 'reason', width: 190, ellipsis: true },
    {
      title: '申请信息', key: 'request', width: 170,
      render: (_, row) => <div className="discount-approval-requester">
        <span>{row.requestByName}</span><Typography.Text type="secondary">{formatDateTime(row.requestTime)}</Typography.Text>
      </div>,
    },
    {
      title: '状态', dataIndex: 'approvalStatus', width: 100,
      render: (value: number) => <Tag color={approvalStatusColor(value)}>{approvalStatusText(value)}</Tag>,
    },
    {
      title: '审批结果', key: 'decision', width: 180, ellipsis: true,
      render: (_, row) => row.decisionReason || row.approveByName || '-',
    },
    {
      title: '操作', key: 'actions', width: 220, fixed: 'right',
      render: (_, row) => <ApprovalActions {...props} record={row} />,
    },
  ]
}

function ApprovalActions(props: Props & { record: SettleDiscountApproval }) {
  const { record } = props
  const pending = record.approvalStatus === DISCOUNT_APPROVAL_STATUS.pending
  const own = record.requestBy === props.currentUserUuid
  const eligible = record.requiredLevel === 'ADMIN' ? props.canApproveAdmin
    : props.canApproveFinance || props.canApproveAdmin
  return <Space size={4} wrap={false}>
    <Button type="text" icon={<EyeOutlined />} title="查看结算单" onClick={() => props.onOpen(record)} />
    {pending && eligible && !own && <>
      <Popconfirm title="确认批准这项优惠方案？" onConfirm={() => props.onApprove(record)}>
        <Button type="text" icon={<CheckOutlined />} title="批准" loading={props.mutationLoading} />
      </Popconfirm>
      <Button type="text" danger icon={<CloseOutlined />} title="驳回"
        onClick={() => props.onReject(record)} />
    </>}
    {own && [DISCOUNT_APPROVAL_STATUS.pending, DISCOUNT_APPROVAL_STATUS.approved]
      .includes(record.approvalStatus as 1 | 2) &&
      <Popconfirm title="确认取消当前优惠申请？" onConfirm={() => props.onCancel(record)}>
        <Button type="text" icon={<StopOutlined />} title="取消申请" loading={props.mutationLoading} />
      </Popconfirm>}
  </Space>
}
