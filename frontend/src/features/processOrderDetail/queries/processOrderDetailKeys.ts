import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getHistoricalProcessOrderIssuePrintView, getProcessOrder, getProcessOrderIssueConsistency, getProcessOrderPrintView, getSnapshotDiff, listProcessOrderIssueVersions } from '../../../api/processOrder'
import type { PrintViewVersion } from '../../../types/processOrder'

export const processOrderDetailKeys = createQueryKeys('processOrderDetail', {
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getProcessOrder(uuid),
  }),
  issueVersions: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => listProcessOrderIssueVersions(uuid),
  }),
  issueConsistency: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getProcessOrderIssueConsistency(uuid),
  }),
  printView: (uuid: string, version: PrintViewVersion) => ({
    queryKey: [uuid, version],
    queryFn: () => getProcessOrderPrintView(uuid, version),
  }),
  historicalIssuePrintView: (uuid: string, issueVersion: number) => ({
    queryKey: [uuid, issueVersion],
    queryFn: () => getHistoricalProcessOrderIssuePrintView(uuid, issueVersion),
  }),
  snapshotDiff: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getSnapshotDiff(uuid),
  }),
})
