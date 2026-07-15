import type { ProcessOrderPrintViewVO, PrintViewVersion } from '../../types/processOrder'
import { formatDateTime } from '../../utils/dateTime'

interface PrintVersionMetadata {
  snapshotTime?: string
  snapshotUser?: string
}

export function paperVersionText(
  version: PrintViewVersion,
  source?: ProcessOrderPrintViewVO['source'],
): string {
  if (source === 'LIVE_PREVIEW') return '待下发预览'
  return version === 'FINISHED' ? '完工实际版' : '下发冻结版'
}

export function printVersionProps(version: PrintViewVersion, view?: ProcessOrderPrintViewVO) {
  return {
    ...printVersionMetadata(view),
    version,
    versionLabel: paperVersionText(version, view?.source),
  }
}

export function printVersionMetadata(view?: ProcessOrderPrintViewVO): PrintVersionMetadata {
  if (!view) return {}
  return {
    snapshotTime: view.snapshotTime ? formatDateTime(view.snapshotTime) : undefined,
    snapshotUser: view.source === 'LIVE_PREVIEW' ? undefined : view.snapshotUser?.trim() || '未记录',
  }
}

export function printVersionWarning(view?: ProcessOrderPrintViewVO): string | undefined {
  if (!view?.warning) return undefined
  return `历史快照兼容模式：${view.warning}`
}
