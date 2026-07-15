import {
  getDataHealthSummary,
  reconcileSettlement,
  restoreCompletedOrder,
} from '../../../api/dataHealth'

export const dataHealthService = {
  inspect: getDataHealthSummary,
  reconcileSettlement,
  restoreCompletedOrder,
}
