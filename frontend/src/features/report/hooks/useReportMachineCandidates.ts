import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportMachineCandidates(keyword: string) {
  return useQuery({
    ...queries.report.machineCandidates(keyword),
    enabled: keyword.length > 0,
  })
}
