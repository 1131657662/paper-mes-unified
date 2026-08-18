import { Alert, Button, Popconfirm, Typography } from 'antd'
import type { ProcessAiPackagingDraft } from '../../processAi/types'

interface Props {
  draft: ProcessAiPackagingDraft
  onDismiss?: (draft: ProcessAiPackagingDraft) => Promise<void>
}

export default function AiPackagingCandidateNotice({ draft, onDismiss }: Props) {
  if (!onDismiss) return null
  return <Alert
    type="warning"
    showIcon
    message="AI 包装候选待确认"
    description={<CandidateDescription draft={draft} />}
    action={<Popconfirm
      title="放弃这条 AI 包装候选？"
      description="放弃后不会写入附加工艺，之后可重新解析。"
      okText="确认放弃"
      cancelText="返回"
      onConfirm={() => onDismiss(draft)}
    >
      <Button danger size="small">放弃候选</Button>
    </Popconfirm>}
  />
}

function CandidateDescription({ draft }: { draft: ProcessAiPackagingDraft }) {
  const values = draft.values
  const price = values.billingMode === 3
    ? `固定金额 ${values.billingAmount ?? '待填'} 元`
    : `${values.unitPrice ?? '待填'} 元/${values.billingBasis === 'TON' ? '吨' : '件'}`
  return <Typography.Text>
    {values.stepName || '重新包装'}：请确认机台、数量和价格后保存；当前建议 {price}。
  </Typography.Text>
}
