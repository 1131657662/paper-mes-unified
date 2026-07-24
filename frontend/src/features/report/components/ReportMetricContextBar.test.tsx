import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ReportMetricContextVO, ReportQueryExecutionMetaVO } from '../../../types/report'
import ReportMetricContextBar from './ReportMetricContextBar'

describe('报表指标口径状态条', () => {
  it('紧凑模式保留版本、水位和一致性状态', () => {
    const markup = renderBar(true)

    expect(markup).toContain('统计报表全域指标口径 V2')
    expect(markup).toContain('实时口径')
    expect(markup).toContain('数据截至 2026-07-21 16:20:00')
    expect(markup).not.toContain('38 个原子指标')
  })

  it('完整模式展示发布代码和指标数量', () => {
    const markup = renderBar(false)

    expect(markup).toContain('REPORT-BASELINE-V2')
    expect(markup).toContain('38 个原子指标')
  })
})

function renderBar(compact: boolean) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return renderToStaticMarkup(
    <QueryClientProvider client={queryClient}>
      <ReportMetricContextBar compact={compact} context={context} execution={execution} loading={false} />
    </QueryClientProvider>,
  )
}

const context: ReportMetricContextVO = {
  asOf: '2026-07-21T16:20:00+08:00',
  metrics: Array.from({ length: 38 }, (_, index) => ({
    definitionChecksum: `checksum-${index}`,
    description: `指标 ${index}`,
    displayScale: 2,
    metricCode: `METRIC_${index}`,
    metricName: `指标 ${index}`,
    metricUuid: `metric-${index}`,
    metricVersionUuid: `version-${index}`,
    unitCode: 'CNY',
    valueType: 'MONEY',
    versionNo: 2,
  })),
  publishedAt: '2026-07-21T08:00:00+08:00',
  releaseChecksum: 'release-checksum',
  releaseCode: 'REPORT-BASELINE-V2',
  releaseName: '统计报表全域指标口径 V2',
  releaseUuid: 'release-v2',
}

const execution: ReportQueryExecutionMetaVO = {
  consistencyMode: 'LIVE_DB_READ',
  coverage: 'LIVE_ONLY',
  dataAsOf: '2026-07-21T16:20:00+08:00',
  metricReleaseUuid: 'release-v2',
  metricVersionMap: {},
  queryHash: 'query-hash',
  queryId: 'query-id',
  sectionStatuses: { details: 'READY', dimensions: 'READY', overview: 'READY' },
  sourceWatermark: '2026-07-21T16:20:00+08:00',
  warnings: [],
}
