import { useState } from 'react'
import { Alert, Button, message, Popconfirm } from 'antd'
import { CheckOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { useConfirmProcessAiResult } from '../hooks/useConfirmProcessAiResult'
import { acceptedPaths, defaultAcceptedOptionIds } from '../processAiFieldOptions'
import {
  buildProcessAiRemarkReview,
  buildProcessAiReviewGroups,
  conflictingOptionIds,
} from '../processAiReviewModel'
import type { ProcessAiConfirmResponse, ProcessAiCurrentDraft, ProcessAiParseResult } from '../types'
import ProcessAiRemarkReview from './ProcessAiRemarkReview'
import ProcessAiReviewTable from './ProcessAiReviewTable'

interface Props {
  currentDraft: ProcessAiCurrentDraft
  conversationRequirement: string
  defaultOriginalUuid?: string
  expectedVersion: number
  orderUuid: string
  result: ProcessAiParseResult
  onApply: (confirmation: ProcessAiConfirmResponse) => void
}

export default function ProcessAiResultPanel(props: Props) {
  const groups = buildProcessAiReviewGroups(props.result, props.currentDraft)
  const defaultOwner = props.result.compiled.plans
    .find((item) => item.originalUuid === props.defaultOriginalUuid)?.ownerRollRef
  const [selectedIds, setSelectedIds] = useState(() => defaultAcceptedOptionIds(
    groups, defaultOwner, conflictingOptionIds(groups),
  ))
  const [applyKey] = useState(() => crypto.randomUUID())
  const { mutateAsync: confirmResult, isPending: isConfirming } = useConfirmProcessAiResult()
  const stale = props.result.expectedVersion !== props.expectedVersion
  const ready = props.result.status === 'READY' && props.result.compiled.eligible && !stale
  const selectedPaths = acceptedPaths(groups, selectedIds)
  const remarkReview = buildProcessAiRemarkReview(
    props.result, props.currentDraft.remarkLong, props.conversationRequirement,
  )

  const confirm = async () => {
    const confirmation = await confirmResult({
      orderUuid: props.orderUuid,
      request: {
        conversationId: props.result.conversationId,
        parseId: props.result.parseId,
        expectedVersion: props.expectedVersion,
        applyIdempotencyKey: applyKey,
        acceptedFieldPaths: selectedPaths,
      },
    })
    props.onApply(confirmation)
    message.success('AI 候选已保存到当前加工单草稿')
  }

  return <section className="process-ai-result">
    <ResultStatus result={props.result} stale={stale} />
    <ProcessAiRemarkReview review={remarkReview} />
    <ProcessAiReviewTable groups={groups} result={props.result}
      selectedIds={selectedIds} onChange={setSelectedIds} />
    <ResultIssues result={props.result} />
    {ready && <Popconfirm title="确认更新客户加工要求，并把所选字段写入当前草稿？"
      okText="确认应用" cancelText="取消" disabled={selectedPaths.length === 0}
      onConfirm={() => void confirm()}>
      <Button block type="primary" icon={<CheckOutlined />} loading={isConfirming}
        disabled={selectedPaths.length === 0}>确认所选字段并加入配置</Button>
    </Popconfirm>}
  </section>
}

function ResultStatus({ result, stale }: { result: ProcessAiParseResult; stale: boolean }) {
  if (stale) return <Alert showIcon type="info" message="该轮建议已应用或已过期" />
  if (result.compiled.eligible && result.status === 'READY') {
    return <Alert showIcon type="success" message="候选方案已通过后端工艺校验" />
  }
  if (result.result.needsClarification) {
    return <Alert showIcon type="warning" message="需要补充信息后才能生成方案" />
  }
  return <Alert showIcon type="error" message="当前建议不能直接应用" />
}

function ResultIssues({ result }: { result: ProcessAiParseResult }) {
  const questions = result.result.clarificationQuestions
  const issues = [...result.compiled.errors, ...result.result.conflicts, ...result.result.unmappedText]
  return <>
    {questions.length > 0 && <Alert type="warning" showIcon icon={<ExclamationCircleOutlined />}
      message="请在下方继续回复" description={questions.join('；')} />}
    {issues.length > 0 && <Alert type="error" showIcon message="未解决的问题"
      description={issues.join('；')} />}
    {result.compiled.warnings.length > 0 && <Alert type="warning" showIcon message="应用前注意"
      description={result.compiled.warnings.join('；')} />}
  </>
}
