import {
  createReportSubscription,
  deleteReportSubscription,
  getReportSubscriptionRecipients,
  getReportSubscriptions,
  getReportSubscriptionRuns,
  runReportSubscriptionNow,
  retryReportSubscriptionRun,
  updateReportSubscription,
} from '../../../api/reportSubscription'

export const reportSubscriptionService = {
  candidates: getReportSubscriptionRecipients,
  create: createReportSubscription,
  delete: deleteReportSubscription,
  list: getReportSubscriptions,
  runs: getReportSubscriptionRuns,
  runNow: runReportSubscriptionNow,
  retryRun: retryReportSubscriptionRun,
  update: updateReportSubscription,
}
