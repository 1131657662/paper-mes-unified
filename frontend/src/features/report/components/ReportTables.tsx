import { Segmented, Tabs } from 'antd'
import type { ReactNode } from 'react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import DocumentDetailTable from '../../../components/biz/DocumentDetailTable'
import { mesTablePagination } from '../../../components/biz/mesPaginationUtils'
import type { PageResult } from '../../../types/common'
import type { ReportDetailVO, ReportDimension, ReportDimensionVO } from '../../../types/report'
import ReportDimensionSummaryRow from './ReportDimensionSummaryRow'
import { detailColumns, dimensionColumns } from './reportTableColumns'

interface Props {
  details?: PageResult<ReportDetailVO>
  dimension: ReportDimension
  dimensions: ReportDimensionVO[]
  loading: boolean
  onDetailPageChange: (current: number, size: number) => void
  onDimensionChange: (dimension: ReportDimension) => void
  onRefresh: () => void
}

type ReportTableTab = 'summary' | 'details'

export default function ReportTables(props: Props) {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<ReportTableTab>('summary')

  const changeTab = (key: string) => {
    if (key === 'summary' || key === 'details') setActiveTab(key)
  }

  return (
    <section className="report-panel report-panel--tables">
      <div className="report-panel__head">
        <h3>多维明细</h3>
        <p>汇总用于横向比较，明细用于追溯加工单、成品与结算链路。</p>
      </div>
      <Tabs
        activeKey={activeTab}
        className="report-table-tabs"
        onChange={changeTab}
        tabBarExtraContent={activeTab === 'summary'
          ? <DimensionSelector value={props.dimension} onChange={props.onDimensionChange} />
          : null}
        items={[
          {
            key: 'summary', label: '维度汇总',
            children: <DimensionTable {...props} />,
          },
          {
            key: 'details', label: detailTabLabel(props.details),
            children: <DetailTable {...props} onOpenOrder={(uuid) => navigate(`/process-orders/${uuid}`)} />,
          },
        ]}
      />
    </section>
  )
}

function DimensionSelector({ value, onChange }: { value: ReportDimension; onChange: Props['onDimensionChange'] }) {
  const changeDimension = (next: string | number) => {
    if (typeof next === 'string' && isReportDimension(next)) onChange(next)
  }
  return <Segmented aria-label="报表汇总维度" value={value} options={dimensionOptions} onChange={changeDimension} />
}

function DimensionTable(props: Props) {
  return (
    <ReportTableBlock hint="应收按回录完成日期归属；到账只统计有效收款流水。">
      <DocumentDetailTable<ReportDimensionVO>
        columns={dimensionColumns(props.dimension)}
        dataSource={props.dimensions}
        loading={props.loading}
        onReload={props.onRefresh}
        pagination={false}
        rowKey={(record) => `${props.dimension}-${record.dimensionKey}`}
        scroll={{ x: 2084, y: 420 }}
        storageKey={`report-dimension-${props.dimension}`}
        summary={() => <ReportDimensionSummaryRow rows={props.dimensions} />}
      />
    </ReportTableBlock>
  )
}

function DetailTable(props: Props & { onOpenOrder: (uuid: string) => void }) {
  return (
    <ReportTableBlock hint={detailHint(props.details)}>
      <DocumentDetailTable<ReportDetailVO>
        columns={detailColumns(props.onOpenOrder)}
        dataSource={props.details?.records ?? []}
        loading={props.loading}
        onReload={props.onRefresh}
        pagination={detailPagination(props.details, props.onDetailPageChange)}
        rowKey="orderUuid"
        scroll={{ x: 2614, y: 560 }}
        storageKey="report-order-details"
      />
    </ReportTableBlock>
  )
}

function detailPagination(details: PageResult<ReportDetailVO> | undefined, onChange: Props['onDetailPageChange']) {
  return mesTablePagination(details?.size ?? 20, {
    current: details?.current ?? 1,
    pageSize: details?.size ?? 20,
    pageSizeOptions: [10, 20, 50, 100],
    total: details?.total ?? 0,
    onChange,
  })
}

function detailTabLabel(details?: PageResult<ReportDetailVO>) {
  return details ? `加工单明细 (${details.total})` : '加工单明细'
}

function detailHint(details?: PageResult<ReportDetailVO>) {
  if (!details) return '点击加工单号可进入详情核对来源、成品和结算链路。'
  if (details.total === 0) return '当前筛选条件下暂无加工单。'
  const start = (details.current - 1) * details.size + 1
  const end = Math.min(details.current * details.size, details.total)
  return `当前显示第 ${start}-${end} 条；导出包含完整筛选结果。`
}

function ReportTableBlock({ children, hint }: { children: ReactNode; hint: string }) {
  return (
    <div className="report-table-block">
      <div className="report-table-block__head"><span>{hint}</span></div>
      <div className="document-module-table">{children}</div>
    </div>
  )
}

function isReportDimension(value: string): value is ReportDimension {
  return dimensionOptions.some((option) => option.value === value)
}

const dimensionOptions = [
  { value: 'customer', label: '客户' }, { value: 'paper', label: '产品' },
  { value: 'process', label: '工艺' }, { value: 'machine', label: '机台' },
  { value: 'month', label: '月度' }, { value: 'invoice', label: '开票' },
  { value: 'settleType', label: '结算' }, { value: 'status', label: '状态' },
] satisfies Array<{ value: ReportDimension; label: string }>
