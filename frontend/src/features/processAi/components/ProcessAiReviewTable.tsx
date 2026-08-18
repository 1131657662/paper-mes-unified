import { Checkbox, Collapse, Tag, Typography } from 'antd'
import type { ProcessAiParseResult } from '../types'
import type { ProcessAiReviewGroup } from '../processAiReviewModel'

interface Props {
  groups: ProcessAiReviewGroup[]
  result: ProcessAiParseResult
  selectedIds: string[]
  onChange: (ids: string[]) => void
}

export default function ProcessAiReviewTable(props: Props) {
  return <div className="process-ai-review-groups">
    {props.groups.map((group) => <ReviewGroup key={group.ownerRollRef} group={group}
      result={props.result} selectedIds={props.selectedIds} onChange={props.onChange} />)}
  </div>
}

function ReviewGroup({ group, result, selectedIds, onChange }: {
  group: ProcessAiReviewGroup
  result: ProcessAiParseResult
  selectedIds: string[]
  onChange: (ids: string[]) => void
}) {
  const selected = new Set(selectedIds)
  const enabled = group.options.some((option) => selected.has(option.id))
  const assignment = result.result.assignments.find((item) => item.ownerRollRef === group.ownerRollRef)
  const candidate = result.compiled.plans.find((item) => item.ownerRollRef === group.ownerRollRef)
  const toggle = (id: string, checked: boolean) => onChange(checked
    ? [...selectedIds, id] : selectedIds.filter((value) => value !== id))
  const toggleGroup = (checked: boolean) => onChange(checked
    ? [...new Set([...selectedIds, ...group.options.map((option) => option.id)])]
    : selectedIds.filter((id) => !group.options.some((option) => option.id === id)))

  return <section className="process-ai-assignment">
    <header className="process-ai-assignment__title">
      <Checkbox checked={enabled} onChange={(event) => toggleGroup(event.target.checked)}>应用本组</Checkbox>
      <Typography.Text strong>{group.ownerRollRef}</Typography.Text>
      {candidate && <Tag color={candidate.plan.mainStepType === 1 ? 'gold' : 'blue'}>
        {candidate.plan.mainStepType === 1 ? '锯纸' : '复卷'}
      </Tag>}
    </header>
    <div className="process-ai-review-table" role="table" aria-label={`${group.ownerRollRef} AI 工艺差异`}>
      <ReviewHeader />
      {group.options.map((option) => <div className="process-ai-review-row" role="row" key={option.id}>
        <div className="process-ai-review-cell process-ai-review-cell--field" role="cell">
          <Checkbox checked={selected.has(option.id)} disabled={!enabled || option.required}
            onChange={(event) => toggle(option.id, event.target.checked)}>
            {option.label}
          </Checkbox>
          {option.conflict && <Tag color="warning">人工已修改</Tag>}
        </div>
        <ReviewValue value={option.baselineValue} />
        <ReviewValue value={option.currentValue} changed={option.conflict} />
        <ReviewValue value={option.aiValue} proposed />
      </div>)}
    </div>
    {assignment?.evidence.length ? <Collapse ghost size="small" items={[{
      key: 'evidence', label: '解析依据', children: assignment.evidence.map((item) =>
        <Typography.Paragraph key={`${item.field}-${item.text}`}>{item.text}</Typography.Paragraph>),
    }]} /> : null}
  </section>
}

function ReviewHeader() {
  return <div className="process-ai-review-row process-ai-review-row--header" role="row">
    <div role="columnheader">字段</div>
    <div role="columnheader">解析时基线</div>
    <div role="columnheader">当前人工值</div>
    <div role="columnheader">AI 建议</div>
  </div>
}

function ReviewValue({ value, changed, proposed }: {
  value: string
  changed?: boolean
  proposed?: boolean
}) {
  const className = [
    'process-ai-review-cell',
    changed ? 'process-ai-review-cell--changed' : '',
    proposed ? 'process-ai-review-cell--proposed' : '',
  ].filter(Boolean).join(' ')
  return <div className={className} role="cell">{value || '未填写'}</div>
}
