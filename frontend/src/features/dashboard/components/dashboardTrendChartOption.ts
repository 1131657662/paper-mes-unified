import dayjs from 'dayjs'
import type { EChartsOption } from 'echarts'
import type { DashboardTrendModel } from './dashboardTrendModel'
import { formatMoney, formatNumber } from '../../report/utils/reportFormatters'

interface TrendPointData {
  month: string
  orderCount: number
  value: number
}

interface TooltipParam {
  data: TrendPointData
}

export function buildTrendChartOption(model: DashboardTrendModel): EChartsOption {
  return {
    animationDuration: 460,
    animationDurationUpdate: 320,
    aria: { enabled: true, label: { description: '近12个月加工应收趋势' } },
    grid: { containLabel: true, left: 8, right: 14, top: 18, bottom: 8 },
    tooltip: {
      trigger: 'axis',
      triggerOn: 'mousemove',
      confine: true,
      className: 'dashboard-trend__tooltip',
      backgroundColor: 'rgb(255 255 255 / 98%)',
      borderColor: '#d9e3ed',
      borderWidth: 1,
      padding: [9, 11],
      textStyle: { color: '#334155', fontSize: 12 },
      extraCssText: 'border-radius:8px;box-shadow:0 8px 22px rgb(15 23 42 / 12%);',
      axisPointer: { type: 'none' },
      formatter: formatTrendTooltip,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: model.monthly.map((item, index) => axisMonth(item.month ?? '', index)),
      axisLine: { lineStyle: { color: '#dbe4ee' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11, margin: 11 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      splitNumber: 3,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11, formatter: (value: number) => shortMoney(value) },
      splitLine: { lineStyle: { color: '#edf2f7', type: 'solid' } },
    },
    series: [{
      name: '加工应收',
      type: 'line',
      data: model.monthly.map((item) => ({
        month: item.month ?? '',
        orderCount: Number(item.orderCount ?? 0),
        value: Number(item.amount ?? 0),
      })),
      smooth: 0.35,
      smoothMonotone: 'x',
      showSymbol: true,
      symbol: 'circle',
      symbolSize: (value: number) => value > 0 ? 7 : 5,
      lineStyle: { color: '#1677ff', width: 2.5 },
      itemStyle: { color: '#fff', borderColor: '#1677ff', borderWidth: 2 },
      emphasis: { scale: 1.25, itemStyle: { borderWidth: 2.5 } },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(22, 119, 255, 0.18)' },
            { offset: 1, color: 'rgba(22, 119, 255, 0.01)' },
          ],
        },
      },
    }],
  }
}

export function formatTrendTooltip(params: unknown): string {
  const first = Array.isArray(params) ? params[0] : params
  if (!isTooltipParam(first)) return ''
  const month = dayjs(`${first.data.month}-01`).format('YYYY年M月')
  return `<div style="min-width:150px"><div style="margin-bottom:6px;color:#172033;font-weight:650">${month}</div>`
    + `<div><span style="display:inline-block;width:7px;height:7px;margin-right:7px;border-radius:50%;background:#1677ff"></span>`
    + `加工应收 <strong style="float:right;margin-left:16px;color:#172033">${formatMoney(first.data.value)}</strong></div>`
    + `<div style="margin-top:4px;color:#64748b">完成加工单 <strong style="float:right;margin-left:16px;color:#334155">`
    + `${formatNumber(first.data.orderCount)} 单</strong></div></div>`
}

function isTooltipParam(value: unknown): value is TooltipParam {
  if (!value || typeof value !== 'object' || !('data' in value)) return false
  const data = value.data
  return Boolean(data && typeof data === 'object' && 'month' in data && 'orderCount' in data && 'value' in data)
}

function axisMonth(month: string, index: number): string {
  const value = dayjs(`${month}-01`)
  return index === 0 || value.month() === 0 ? value.format('YY/MM') : value.format('MM')
}

function shortMoney(value: number): string {
  if (value >= 10000) return `${formatNumber(value / 10000, 1)}万`
  return formatNumber(value, 0)
}
