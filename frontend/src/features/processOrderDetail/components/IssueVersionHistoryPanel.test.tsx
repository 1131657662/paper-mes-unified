import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ProcessOrderIssueVersion } from '../../../types/processOrder'
import { IssueVersionHistoryContent } from './IssueVersionHistoryPanel'

describe('下发版本历史', () => {
  it('历史未版本化快照明确说明未补造审计元数据', () => {
    const legacy = {
      orderUuid: 'order-1',
      status: 'LEGACY_UNVERSIONED',
      hasSnapshotBefore: false,
      hasSnapshotAfter: true,
    } satisfies ProcessOrderIssueVersion

    const markup = renderToStaticMarkup(
      <IssueVersionHistoryContent isError={false} loading={false}
        versions={[legacy]} onRetry={() => undefined} />,
    )

    expect(markup).toContain('历史未版本化')
    expect(markup).toContain('未补造版本号、操作者、变更时间或下发时间')
    expect(markup).not.toContain('Vundefined')
  })

  it('真实版本显示业务版本号且不显示历史警告', () => {
    const applied = {
      uuid: 'version-2',
      orderUuid: 'order-1',
      versionNo: 2,
      status: 'APPLIED',
      hasSnapshotBefore: true,
      hasSnapshotAfter: true,
    } satisfies ProcessOrderIssueVersion

    const markup = renderToStaticMarkup(
      <IssueVersionHistoryContent isError={false} loading={false}
        versions={[applied]} onRetry={() => undefined} />,
    )

    expect(markup).toContain('V2')
    expect(markup).not.toContain('包含 V3.53 前历史下发快照')
  })
})
