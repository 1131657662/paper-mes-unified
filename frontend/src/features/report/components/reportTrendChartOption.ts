import type { EChartsOption } from 'echarts'
import type { ReportDimensionVO } from '../../../types/report'
import { formatMoney, formatTonFromKg } from '../utils/reportFormatters'

interface TrendPoint {
  name: string
  value: number
  weight: number
}

interface TooltipParam {
  data: TrendPoint
}

export function buildReportTrendChartOption(monthly: ReportDimensionVO[]): EChartsOption {
  const points = monthly.map(toTrendPoint)
  return {
    animationDuration: 460,
    aria: { enabled: true, label: { description: '月度加工应收曲线' } },
    grid: { containLabel: false, left: 10, right: 10, top: 12, bottom: 12 },
    tooltip: {
      trigger: 'axis',
      confine: true,
      renderMode: 'richText',
      formatter: formatReportTrendTooltip,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((point) => point.name),
      axisLabel: { show: false },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      min: 0,
      splitNumber: 4,
      axisLabel: { show: false },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#e8eef5', type: 'solid' } },
    },
    series: [{
      type: 'line',
      data: points,
      smooth: 0.35,
      smoothMonotone: 'x',
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: { color: '#1677ff', width: 2.5 },
      itemStyle: { color: '#fff', borderColor: '#1677ff', borderWidth: 2 },
      areaStyle: { color: 'rgba(22, 119, 255, 0.12)' },
    }],
  }
}

export function formatReportTrendTooltip(params: unknown): string {
  const first = Array.isArray(params) ? params[0] : params
  if (!isTooltipParam(first)) return ''
  const { name, value, weight } = first.data
  return `${name}\n加工应收 ${formatMoney(value)}\n原纸 ${formatTonFromKg(weight)}`
}

function toTrendPoint(item: ReportDimensionVO): TrendPoint {
  return {
    name: item.dimensionName,
    value: Number(item.totalAmount ?? 0),
    weight: Number(item.originalWeight ?? 0),
  }
}

function isTooltipParam(value: unknown): value is TooltipParam {
  if (!value || typeof value !== 'object' || !('data' in value)) return false
  const data = value.data
  return Boolean(data && typeof data === 'object' && 'name' in data && 'value' in data && 'weight' in data)
}
