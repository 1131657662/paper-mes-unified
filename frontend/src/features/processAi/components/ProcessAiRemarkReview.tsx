import { Alert, Typography } from 'antd'
import type { ProcessAiRemarkReview as RemarkReview } from '../processAiReviewModel'

export default function ProcessAiRemarkReview({ review }: { review: RemarkReview }) {
  return <section className="process-ai-remark-review">
    <div className="process-ai-remark-review__heading">
      <Typography.Text strong>客户加工要求</Typography.Text>
      {review.conflict && <Alert type="warning" showIcon
        message="当前备注在解析后被修改，确认后将以右侧内容更新" />}
    </div>
    <div className="process-ai-remark-review__grid">
      <RemarkColumn title="解析时基线" value={review.baselineValue} />
      <RemarkColumn title="当前人工值" value={review.currentValue} changed={review.conflict} />
      <RemarkColumn title="确认后内容" value={review.proposedValue} proposed />
    </div>
  </section>
}

function RemarkColumn({ title, value, changed, proposed }: {
  title: string
  value: string
  changed?: boolean
  proposed?: boolean
}) {
  const className = [
    'process-ai-remark-review__value',
    changed ? 'process-ai-remark-review__value--changed' : '',
    proposed ? 'process-ai-remark-review__value--proposed' : '',
  ].filter(Boolean).join(' ')
  return <div>
    <Typography.Text type="secondary">{title}</Typography.Text>
    <div className={className}>{value || '未填写'}</div>
  </div>
}
