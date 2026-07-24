import {
  createReportAlertRule,
  acknowledgeReportAlertEvent,
  deleteReportAlertRule,
  getReportAlertRules,
  getReportAlertEvents,
  getReportThresholdContext,
  updateReportAlertRule,
  ignoreReportAlertEvent,
} from '../../../api/reportAlert'

export const reportAlertService = {
  createRule: createReportAlertRule,
  acknowledgeEvent: acknowledgeReportAlertEvent,
  deleteRule: deleteReportAlertRule,
  rules: getReportAlertRules,
  events: getReportAlertEvents,
  thresholdContext: getReportThresholdContext,
  updateRule: updateReportAlertRule,
  ignoreEvent: ignoreReportAlertEvent,
}
