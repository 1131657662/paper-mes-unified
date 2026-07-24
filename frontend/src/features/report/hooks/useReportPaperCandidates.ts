import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportPaperCandidates(keyword: string) {
  return useQuery({
    ...queries.report.paperCandidates(keyword),
    enabled: keyword.length > 0,
  })
}
