import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getProcessOrder, getProcessOrderPrintView, getSnapshotDiff, listProcessOrderIssueVersions } from '../../../api/processOrder'
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
  printView: (uuid: string, version: PrintViewVersion) => ({
    queryKey: [uuid, version],
    queryFn: () => getProcessOrderPrintView(uuid, version),
  }),
  snapshotDiff: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getSnapshotDiff(uuid),
  }),
})
