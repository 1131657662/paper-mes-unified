import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { ReportDetailQuery, ReportQuery } from '../../../types/report'
import { reportService, type ReportTopicCode } from '../services/reportService'
import type { ReportOperationalTopicCode } from '../../../types/reportOperational'

export const reportKeys = createQueryKeys('report', {
  customerCandidates: (keyword: string) => ({
    queryKey: [keyword],
    queryFn: () => reportService.customerCandidates(keyword),
  }),
  details: (query: ReportDetailQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.details(query),
  }),
  dimensions: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.dimensions(query),
  }),
  machines: {
    queryKey: null,
    queryFn: () => reportService.machines(),
  },
  machineCandidates: (keyword: string) => ({
    queryKey: [keyword],
    queryFn: () => reportService.machineCandidates(keyword),
  }),
  metricContext: {
    queryKey: null,
    queryFn: () => reportService.metricContext(),
  },
  metricRelease: (releaseUuid: string) => ({
    queryKey: [releaseUuid],
    queryFn: () => reportService.metricRelease(releaseUuid),
  }),
  metricReleases: {
    queryKey: null,
    queryFn: () => reportService.metricReleases(),
  },
  overview: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.overview(query),
  }),
  operationalAnalysis: (topic: ReportOperationalTopicCode, query: ReportQuery) => ({
    queryKey: [topic, query],
    queryFn: () => reportService.operationalAnalysis(topic, query),
  }),
  papers: {
    queryKey: null,
    queryFn: () => reportService.papers(),
  },
  paperCandidates: (keyword: string) => ({
    queryKey: [keyword],
    queryFn: () => reportService.paperCandidates(keyword),
  }),
  queryMetadata: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.queryMetadata(query),
  }),
  topicAnalysis: (topic: ReportTopicCode, query: ReportQuery) => ({
    queryKey: [topic, query],
    queryFn: () => reportService.topicAnalysis(topic, query),
  }),
})
