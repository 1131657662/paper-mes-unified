import type { SettleCandidateVO } from '../../types/settle'

export function mergeCandidateSelection(
  current: Record<string, SettleCandidateVO>,
  candidates: SettleCandidateVO[],
  selectedKeys: Set<string>,
) {
  const next: Record<string, SettleCandidateVO> = {}
  for (const [uuid, candidate] of Object.entries(current)) {
    if (selectedKeys.has(uuid)) next[uuid] = candidate
  }
  for (const candidate of candidates) {
    if (selectedKeys.has(candidate.orderUuid)) next[candidate.orderUuid] = candidate
  }
  return next
}

export function isSelectableCandidate(candidate: SettleCandidateVO, lockedCustomerUuid?: string) {
  return Number(candidate.totalAmount ?? 0) > 0
    && (!lockedCustomerUuid || candidate.customerUuid === lockedCustomerUuid)
}
