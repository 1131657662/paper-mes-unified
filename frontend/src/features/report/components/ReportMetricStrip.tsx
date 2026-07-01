import {
  AccountBookOutlined,
  BarChartOutlined,
  DollarOutlined,
  FieldTimeOutlined,
  FileDoneOutlined,
  ScissorOutlined,
} from '@ant-design/icons'
import { StatisticCard } from '@ant-design/pro-components'
import type { ReactNode } from 'react'
import type { ReportOverviewVO } from '../../../types/report'
import { formatKg, formatMoney, formatNumber, formatPercent, formatTonFromKg } from '../utils/reportFormatters'

interface Props {
  overview?: ReportOverviewVO
}

export default function ReportMetricStrip({ overview }: Props) {
  const cards: MetricProps[] = [
    {
      icon: <FileDoneOutlined />,
      main: `${formatNumber(overview?.orderCount)} 单`,
      sub: `${formatNumber(overview?.originalRollCount)} 原卷 / ${formatNumber(overview?.finishRollCount)} 成品`,
      title: '加工单',
    },
    {
      icon: <DollarOutlined />,
      main: formatMoney(overview?.totalAmount),
      sub: `加工费 ${formatMoney(overview?.processAmount)} / 附加 ${formatMoney(overview?.extraAmount)}`,
      title: '加工应收',
      tone: 'primary',
    },
    {
      icon: <BarChartOutlined />,
      main: formatTonFromKg(overview?.originalWeight),
      sub: `成品 ${formatKg(overview?.finishWeight)}`,
      title: '生产吨位',
    },
    {
      icon: <ScissorOutlined />,
      main: formatKg(overview?.lossWeight),
      sub: formatPercent(overview?.lossRatio),
      title: '损耗表现',
      tone: 'warning',
    },
    {
      icon: <AccountBookOutlined />,
      main: formatMoney(overview?.unreceivedAmount),
      sub: `有效已收 ${formatMoney(overview?.receivedAmount)} / 待结算应收 ${formatMoney(overview?.pendingSettleAmount)}`,
      title: '已结算未收',
      tone: 'danger',
    },
    {
      icon: <FieldTimeOutlined />,
      main: `${formatNumber(overview?.knifeCount)} 刀`,
      sub: `锯 ${formatMoney(overview?.sawAmount)} / 复卷 ${formatMoney(overview?.rewindAmount)}`,
      title: '刀数与工艺费',
    },
  ]

  return (
    <StatisticCard.Group className="report-metrics" gutter={[10, 10]} ghost>
      {cards.map((item) => (
        <StatisticCard
          className={`report-metric report-metric--${item.tone ?? 'default'}`}
          colSpan={{ xs: 24, sm: 12, md: 8, xl: 4 }}
          key={item.title}
          statistic={{
            description: item.sub,
            icon: <span className="report-metric__icon">{item.icon}</span>,
            title: item.title,
            value: item.main,
          }}
        />
      ))}
    </StatisticCard.Group>
  )
}

interface MetricProps {
  icon: ReactNode
  main: string
  sub: string
  title: string
  tone?: 'danger' | 'default' | 'primary' | 'warning'
}
