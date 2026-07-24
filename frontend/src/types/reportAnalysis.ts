import type { PageResult } from './common'
import type {
  ReportDetailVO,
  ReportDimensionVO,
  ReportOverviewVO,
  ReportQueryExecutionMetaVO,
} from './report'

export interface ReportPageAnalysisVO {
  execution: ReportQueryExecutionMetaVO
  overview: ReportOverviewVO
  currentBreakdown: ReportDimensionVO[]
  details: PageResult<ReportDetailVO>
  monthlyTrend: ReportDimensionVO[]
  customerRanking: ReportDimensionVO[]
  paperRanking: ReportDimensionVO[]
}

export interface ReportDimensionAnalysisVO {
  execution: ReportQueryExecutionMetaVO
  rows: ReportDimensionVO[]
}

export interface ReportProductionAnalysisVO {
  topicCode: 'production'
  overview: ReportOverviewVO
  monthlyTrend: ReportDimensionVO[]
  processBreakdown: ReportDimensionVO[]
  machineBreakdown: ReportDimensionVO[]
  asOf: string
  execution: ReportQueryExecutionMetaVO
}

export interface ReportQualityLossAnalysisVO {
  topicCode: 'quality-loss'
  overview: ReportOverviewVO
  monthlyTrend: ReportDimensionVO[]
  paperBreakdown: ReportDimensionVO[]
  lossLeaders: ReportDetailVO[]
  asOf: string
  execution: ReportQueryExecutionMetaVO
}

export type ReportTopicAnalysisVO =
  | ReportProductionAnalysisVO
  | ReportQualityLossAnalysisVO
