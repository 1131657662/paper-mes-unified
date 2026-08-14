import { Form } from 'antd'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import BackRecordMergeSummary from './BackRecordMergeSummary'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

describe('BackRecordMergeSummary', () => {
  it('renders an independent weight input for every merged source roll', () => {
    const markup = renderSummary(standardMergeItem())

    expect(markup).toContain('母卷-A 复称重量')
    expect(markup).toContain('母卷-B 复称重量')
    expect(markup).toContain('母卷-C 复称重量')
    expect(markup).toContain('复称重量（必填）')
  })

  it('labels source weights optional for fixed amount rewind', () => {
    const item = standardMergeItem()
    item.rollProductions![0]!.steps![0]!.billingMode = 3

    const markup = renderSummary(item)

    expect(markup).toContain('复称重量（选填）')
    expect(markup).toContain('提交后的重量闭合会标记为未核验')
  })
})

function SummaryFixture({ item }: { item: BackRecordWorkItem }) {
  const [form] = Form.useForm()
  return (
    <Form form={form}>
      <BackRecordMergeSummary item={item} onFieldExhausted={() => undefined} />
    </Form>
  )
}

function renderSummary(item: BackRecordWorkItem): string {
  return renderToStaticMarkup(<SummaryFixture item={item} />)
}

function standardMergeItem(): BackRecordWorkItem {
  const rollProductions = ['A', 'B', 'C'].map((suffix, index) => ({
    originalUuid: `roll-${suffix.toLowerCase()}`,
    rollNo: `母卷-${suffix}`,
    rollWeight: 1,
    weightStatus: 'ESTIMATED' as const,
    steps: index === 0 ? [{
      uuid: 'step-1',
      originalUuid: 'roll-a',
      stepType: 2,
      billingMode: 1 as const,
    }] : [],
  }))
  return {
    key: 'merge-roll-a',
    kind: 'roll',
    title: '合并复卷 3 卷',
    roll: { uuid: 'roll-a', processMode: 1 },
    production: rollProductions[0],
    rollProductions,
    isMergeGroup: true,
    sourceMode: 'linked',
    finishes: [],
  }
}
