import type { ProcessOrderAppendSessionVO } from '../../types/processOrder'

export function hasAppendOrderVersionChange(session: ProcessOrderAppendSessionVO): boolean {
  return validVersion(session.baseOrderVersion)
    && validVersion(session.currentOrderVersion)
    && session.baseOrderVersion !== session.currentOrderVersion
}

export function appendOrderVersionForCommit(session: ProcessOrderAppendSessionVO): number {
  const version = session.currentOrderVersion ?? session.baseOrderVersion
  if (!validVersion(version)) throw new Error('后端未返回有效的加工单版本，请刷新后重试')
  return version
}

function validVersion(version: number | undefined): version is number {
  return Number.isInteger(version) && Number(version) > 0
}
