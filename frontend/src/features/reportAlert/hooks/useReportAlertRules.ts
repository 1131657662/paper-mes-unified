import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportAlertRules(enabled: boolean) {
  return useQuery({ ...queries.reportAlert.rules, enabled })
}
