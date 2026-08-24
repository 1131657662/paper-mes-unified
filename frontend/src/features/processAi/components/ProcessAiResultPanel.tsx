import { useState } from 'react'
import { Alert, Button, message, Popconfirm } from 'antd'
import { CheckOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { useConfirmProcessAiResult } from '../hooks/useConfirmProcessAiResult'
import { useReviseProcessAiResult } from '../hooks/useReviseProcessAiResult'
import { acceptedPaths, defaultAcceptedOptionIds } from '../processAiFieldOptions'
import { buildProcessAiDefaultNotices, type ProcessAiDefaultNotice } from '../processAiDefaultModel'
import {
  buildProcessAiRemarkReview,
  buildProcessAiReviewGroups,
  conflictingOptionIds,
} from '../processAiReviewModel'
import type {
  ProcessAiClarificationQuestion,
  ProcessAiConfirmResponse,
  ProcessAiCurrentDraft,
  ProcessAiParseResult,
  ProcessAiUnderstandingEvidence,
} from '../types'
import ProcessAiRemarkReview from './ProcessAiRemarkReview'
import ProcessAiReviewTable from './ProcessAiReviewTable'
import ProcessAiCorrectionPanel from './ProcessAiCorrectionPanel'
import ProcessAiClarificationQuestions from './ProcessAiClarificationQuestions'

interface Props {
  currentDraft: ProcessAiCurrentDraft
  conversationRequirement: string
  expectedVersion: number
  orderUuid: string
  result: ProcessAiParseResult
  onApply: (confirmation: ProcessAiConfirmResponse) => Promise<void> | void
  onClarify: (question: ProcessAiClarificationQuestion, answerCode: string) => void
  onRevised: (result: ProcessAiParseResult) => void
}

export default function ProcessAiResultPanel(props: Props) {
  const groups = props.result.resultKind === 'UNDERSTANDING'
    ? [] : buildProcessAiReviewGroups(props.result, props.currentDraft)
  const [selectedIds, setSelectedIds] = useState(() => defaultAcceptedOptionIds(
    groups, conflictingOptionIds(groups),
  ))
  const [applyKey] = useState(() => crypto.randomUUID())
  const { mutateAsync: confirmResult, isPending: isConfirming } = useConfirmProcessAiResult()
  const { mutateAsync: reviseResult, isPending: isRevising } = useReviseProcessAiResult()
  const stale = props.result.expectedVersion !== props.expectedVersion
  const ready = props.result.status === 'READY'
    && props.result.dialogueState === 'PREVIEW_READY'
    && props.result.compiled.eligible && !stale
  const selectedPaths = acceptedPaths(groups, selectedIds)
  const remarkReview = buildProcessAiRemarkReview(
    props.result, props.currentDraft.remarkLong, props.conversationRequirement,
  )
  const defaultNotices = buildProcessAiDefaultNotices(props.result.requiredDefaultIds)

  const confirm = async () => {
    if (!props.result.previewHash) return
    const confirmation = await confirmResult({
      orderUuid: props.orderUuid,
      request: {
        conversationId: props.result.conversationId,
        parseId: props.result.parseId,
        expectedVersion: props.expectedVersion,
        applyIdempotencyKey: applyKey,
        acceptedFieldPaths: selectedPaths,
        parseRevision: props.result.parseRevision,
        previewHash: props.result.previewHash,
        acknowledgedDefaultIds: props.result.requiredDefaultIds,
      },
    })
    await props.onApply(confirmation)
    message.success('AI 候选已保存到当前加工单草稿')
  }

  const revise = async (corrections: import('../types').ProcessAiCorrection[]) => {
    const revised = await reviseResult({ orderUuid: props.orderUuid, request: {
      conversationId: props.result.conversationId, parseId: props.result.parseId,
      expectedVersion: props.expectedVersion, parseRevision: props.result.parseRevision,
      corrections,
    } })
    props.onRevised(revised)
  }

  if (props.result.resultKind === 'UNDERSTANDING' || props.result.understanding) {
    return <UnderstandingPanel result={props.result} onClarify={props.onClarify} />
  }

  return <section className="process-ai-result">
    <ResultStatus result={props.result} stale={stale} />
    <ProcessAiDefaultNotices notices={defaultNotices} />
    <ProcessAiRemarkReview review={remarkReview} />
    <ProcessAiCorrectionPanel key={`${props.result.parseId}:${props.result.parseRevision}`}
      result={props.result} loading={isRevising} onSubmit={revise} />
    <ProcessAiReviewTable groups={groups} result={props.result}
      selectedIds={selectedIds} onChange={setSelectedIds} />
    <ProcessAiClarificationQuestions
      questions={props.result.clarificationQuestions ?? []} onClarify={props.onClarify} />
    <ResultIssues result={props.result} />
    {ready && <Popconfirm title="确认更新客户加工要求，并把所选字段写入当前草稿？"
      okText="确认应用" cancelText="取消" disabled={selectedPaths.length === 0}
      onConfirm={() => void confirm()}>
      <Button block type="primary" icon={<CheckOutlined />} loading={isConfirming}
        disabled={selectedPaths.length === 0}>确认所选字段并加入配置</Button>
    </Popconfirm>}
  </section>
}

function ProcessAiDefaultNotices({ notices }: { notices: ProcessAiDefaultNotice[] }) {
  if (notices.length === 0) return null
  return <div className="process-ai-default-notices" aria-label="待确认的系统默认值">
    {notices.map((notice) => <Alert key={notice.id} type="warning" showIcon
      message={notice.message} description={<>
        <code>{notice.id}</code>
        <div>{notice.description}</div>
      </>} />)}
  </div>
}

function ResultStatus({ result, stale }: { result: ProcessAiParseResult; stale: boolean }) {
  if (stale) return <Alert showIcon type="info" message="该轮建议已应用或已过期" />
  if (result.compiled.eligible && result.status === 'READY'
    && result.dialogueState === 'PREVIEW_READY') {
    return <Alert showIcon type="success" message="候选方案已通过后端工艺校验" />
  }
  if (result.result.needsClarification) {
    return <Alert showIcon type="warning" message="需要补充信息后才能生成方案" />
  }
  return <Alert showIcon type="error" message="当前建议不能直接应用" />
}

function ResultIssues({ result }: { result: ProcessAiParseResult }) {
  const legacyQuestions = (result.clarificationQuestions?.length ?? 0) > 0
    ? [] : result.result?.clarificationQuestions ?? []
  const issues = [...result.compiled.errors, ...(result.result?.conflicts ?? []),
    ...(result.result?.unmappedText ?? [])]
  return <>
    {legacyQuestions.length > 0 && <Alert type="warning" showIcon
      icon={<ExclamationCircleOutlined />} message="请在下方继续回复"
      description={legacyQuestions.join('；')} />}
    {issues.length > 0 && <Alert type="error" showIcon message="未解决的问题"
      description={issues.join('；')} />}
    {result.compiled.warnings.length > 0 && <Alert type="warning" showIcon message="应用前注意"
      description={result.compiled.warnings.join('；')} />}
  </>
}

function UnderstandingPanel({ result, onClarify }: {
  result: ProcessAiParseResult
  onClarify: (question: ProcessAiClarificationQuestion, answerCode: string) => void
}) {
  const understanding = result.understanding
  const questions = (result.clarificationQuestions?.length ?? 0) > 0
    ? result.clarificationQuestions ?? [] : understanding?.clarificationQuestions ?? []
  return <section className="process-ai-understanding">
    <Alert showIcon type="info" message={understanding?.conclusion ?? 'AI需要进一步确认'} />
    {understanding?.evidence.length ? <UnderstandingEvidence evidence={understanding.evidence} /> : null}
    {understanding?.assumptions.length ? <Alert type="warning" message="当前假设"
      description={understanding.assumptions.join('；')} /> : null}
    {understanding?.risks.length ? <Alert type="error" message="风险提示"
      description={understanding.risks.join('；')} /> : null}
    <ProcessAiClarificationQuestions questions={questions} onClarify={onClarify} />
  </section>
}

function UnderstandingEvidence({ evidence }: { evidence: ProcessAiUnderstandingEvidence[] }) {
  return <div className="process-ai-understanding__evidence">
    <strong>识别依据</strong>
    <ul>
      {evidence.map((item) => <li key={`${item.field}:${item.sourceRef}:${item.text}`}>
        <span>{item.text}</span>
        <small>（{evidenceSourceLabel(item.sourceType)}：{item.sourceRef}）</small>
      </li>)}
    </ul>
  </div>
}

function evidenceSourceLabel(sourceType: ProcessAiUnderstandingEvidence['sourceType']) {
  const labels: Record<ProcessAiUnderstandingEvidence['sourceType'], string> = {
    CUSTOMER_TEXT: '客户原话',
    DB_FACT: '订单事实',
    APPROVED_MEMORY: '已批准记忆',
    DEFAULT: '系统默认',
    MODEL_INFERENCE: 'AI推断',
  }
  return labels[sourceType]
}
