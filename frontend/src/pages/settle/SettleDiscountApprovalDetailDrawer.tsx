import { Alert, Button, Descriptions, Drawer, Space, Tag } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import type { SettleDiscountApproval } from '../../types/settle'
import { formatDateTime } from '../../utils/dateTime'
import { formatNumber } from '../../utils/numberFormatters'
import { approvalLevelText, approvalStatusColor, approvalStatusText } from './discountApprovalModel'

interface Props {
  approval?: SettleDiscountApproval
  error: boolean
  loading: boolean
  onClose: () => void
  onOpenSettlement: (settleUuid: string) => void
  open: boolean
}

export default function SettleDiscountApprovalDetailDrawer({ approval, error, loading,
  onClose, onOpenSettlement, open }: Props) {
  return <Drawer title="优惠审批详情" width={560} open={open} loading={loading}
    onClose={onClose} extra={approval && <Button icon={<EyeOutlined />}
      onClick={() => onOpenSettlement(approval.settleUuid)}>查看结算单</Button>}>
    {error && <Alert showIcon type="error" message="审批详情加载失败"
      description="请关闭后重试，当前操作不会改变审批状态。" />}
    {approval && <ApprovalDescriptions approval={approval} />}
  </Drawer>
}

function ApprovalDescriptions({ approval }: { approval: SettleDiscountApproval }) {
  return <Descriptions bordered column={1} size="small">
    <Descriptions.Item label="结算单">
      {approval.settleNo ?? approval.settleUuid} · {approval.customerName || '-'}
    </Descriptions.Item>
    <Descriptions.Item label="状态">
      <Space>
        <Tag color={approvalStatusColor(approval.approvalStatus)}>
          {approvalStatusText(approval.approvalStatus)}
        </Tag>
        <Tag color={approval.requiredLevel === 'ADMIN' ? 'volcano' : 'blue'}>
          {approvalLevelText(approval.requiredLevel)}
        </Tag>
      </Space>
    </Descriptions.Item>
    <Descriptions.Item label="收款方案">
      到账 ¥{formatNumber(approval.cashAmount, 2)} · 废纸抵扣 ¥{formatNumber(approval.scrapOffsetAmount, 2)}
      {' · '}优惠 ¥{formatNumber(approval.discountAmount, 2)}
    </Descriptions.Item>
    <Descriptions.Item label="未收金额快照">
      ¥{formatNumber(approval.unreceivedSnapshot, 2)}，优惠占比 {formatNumber(approval.discountPercent, 2)}%
    </Descriptions.Item>
    <Descriptions.Item label="优惠原因">{approval.reason}</Descriptions.Item>
    <Descriptions.Item label="申请信息">
      {approval.requestByName} · {formatDateTime(approval.requestTime)}
    </Descriptions.Item>
    <Descriptions.Item label="审批结果">
      {approval.approveByName || approval.cancelByName || '-'}
      {approval.approveTime ? ` · ${formatDateTime(approval.approveTime)}` : ''}
      {approval.decisionReason ? ` · ${approval.decisionReason}` : ''}
    </Descriptions.Item>
  </Descriptions>
}
