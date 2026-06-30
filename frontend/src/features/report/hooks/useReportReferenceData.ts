import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportMachines() {
  return useQuery(queries.report.machines)
}

export function useReportPapers() {
  return useQuery(queries.report.papers)
}
