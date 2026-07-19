import { BizError } from '../../api/request'

/** Only eligibility changes invalidate the user's selected candidate set. */
export function isCandidateEligibilityError(error: unknown) {
  if (!(error instanceof BizError)) return false
  if (error.message.includes('待核价')) return false
  if (error.errorCode === 'E002') return true
  if (error.errorCode !== 'E001' && error.errorCode !== 'E004') return false
  return error.message.includes('加工单')
}
