import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportCustomerCandidates(keyword: string) {
  return useQuery({
    ...queries.report.customerCandidates(keyword),
    enabled: keyword.length > 0,
  })
}
