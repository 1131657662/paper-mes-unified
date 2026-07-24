import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportSavedViews(enabled = true) {
  return useQuery({ ...queries.reportSavedView.list, enabled })
}
