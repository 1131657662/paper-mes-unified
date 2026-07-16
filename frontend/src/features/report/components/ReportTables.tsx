import { Segmented } from 'antd'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import DocumentDetailTable from '../../../components/biz/DocumentDetailTable'
import { mesTablePagination } from '../../../components/biz/mesPaginationUtils'
import type { ReportDetailVO, ReportDetailsVO, ReportDimension, ReportDimensionVO } from '../../../types/report'
import ReportDimensionSummaryRow from './ReportDimensionSummaryRow'
import { detailColumns, dimensionColumns } from './reportTableColumns'

interface Props {
  details?: ReportDetailsVO
  dimension: ReportDimension
  dimensions: ReportDimensionVO[]
  loading: boolean
  onDimensionChange: (dimension: ReportDimension) => void
  onRefresh: () => void
}

export default function ReportTables({
  details,
  dimension,
  dimensions,
  loading,
  onDimensionChange,
  onRefresh,
}: Props) {
  const navigate = useNavigate()

  return (
    <section className="report-panel report-panel--tables">
      <div className="report-panel__head report-panel__head--inline">
        <div>
          <h3>报表明细</h3>
          <p>维度汇总与加工单明细均支持列宽拖拽、列设置和刷新。</p>
        </div>
        <Segmented
          aria-label="报表汇总维度"
          value={dimension}
          options={dimensionOptions}
          onChange={(value) => onDimensionChange(value as ReportDimension)}
        />
      </div>
      <div className="report-table-stack">
        <ReportTableBlock title="维度汇总" hint="应收按加工单制单日期归属；实际到账和废纸抵扣均只统计有效收款流水。">
          <DocumentDetailTable<ReportDimensionVO>
            columns={dimensionColumns(dimension)}
            dataSource={dimensions}
            loading={loading}
            onReload={onRefresh}
            pagination={false}
            rowKey={(record) => `${dimension}-${record.dimensionKey}`}
            scroll={{ x: 2070, y: 260 }}
            storageKey={`report-dimension-${dimension}`}
            summary={() => <ReportDimensionSummaryRow rows={dimensions} />}
          />
        </ReportTableBlock>
        <ReportTableBlock title="加工单明细" hint={detailHint(details)}>
          <DocumentDetailTable<ReportDetailVO>
            columns={detailColumns((uuid) => navigate(`/process-orders/${uuid}`))}
            dataSource={details?.rows ?? []}
            loading={loading}
            onReload={onRefresh}
            pagination={mesTablePagination(20)}
            rowKey="orderUuid"
            scroll={{ x: 2506, y: 560 }}
            storageKey="report-order-details"
          />
        </ReportTableBlock>
      </div>
    </section>
  )
}

function detailHint(details?: ReportDetailsVO) {
  if (!details) return '点击加工单号可进入详情核对来源、成品和结算链路。'
  if (!details.truncated) return `当前筛选共 ${details.total} 条，点击加工单号可进入详情。`
  return `共 ${details.total} 条，页面显示前 ${details.displayLimit} 条；导出包含完整筛选结果。`
}

function ReportTableBlock({ children, hint, title }: { children: ReactNode; hint: string; title: string }) {
  return (
    <div className="report-table-block">
      <div className="report-table-block__head">
        <strong>{title}</strong>
        <span>{hint}</span>
      </div>
      <div className="document-module-table">{children}</div>
    </div>
  )
}

const dimensionOptions = [
  { value: 'customer', label: '客户' },
  { value: 'paper', label: '产品' },
  { value: 'process', label: '工艺' },
  { value: 'machine', label: '机台' },
  { value: 'month', label: '月度' },
  { value: 'invoice', label: '开票' },
  { value: 'settleType', label: '结算' },
  { value: 'status', label: '状态' },
]
