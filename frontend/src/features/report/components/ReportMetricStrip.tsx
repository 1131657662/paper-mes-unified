import {
  AccountBookOutlined,
  BarChartOutlined,
  DollarOutlined,
  FileDoneOutlined,
  ScissorOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import { StatisticCard } from '@ant-design/pro-components'
import type { ReactNode } from 'react'
import type { ReportOverviewVO } from '../../../types/report'
import { formatMoney, formatNumber, formatPercent, formatTonFromKg } from '../utils/reportFormatters'

interface Props {
  overview?: ReportOverviewVO
}

export default function ReportMetricStrip({ overview }: Props) {
  return (
    <StatisticCard.Group className="report-metrics" gutter={[10, 10]} ghost>
      {buildCards(overview).map((item) => (
        <StatisticCard
          className={`report-metric report-metric--${item.tone ?? 'default'}`}
          colSpan={{ xs: 24, sm: 12, md: 8, xl: 4 }}
          key={item.title}
          statistic={{
            description: <span className="report-metric__description" title={item.sub}>{item.sub}</span>,
            icon: <span className="report-metric__icon">{item.icon}</span>,
            title: item.title,
            value: item.main,
          }}
        />
      ))}
    </StatisticCard.Group>
  )
}

function buildCards(overview?: ReportOverviewVO): MetricProps[] {
  return [
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
      sub: `成品 ${formatTonFromKg(overview?.finishWeight)}`,
      title: '生产吨位',
    },
    {
      icon: <ScissorOutlined />,
      main: formatTonFromKg(overview?.lossWeight),
      sub: formatPercent(overview?.lossRatio),
      title: '损耗表现',
      tone: 'warning',
    },
    {
      icon: <AccountBookOutlined />,
      main: formatMoney(overview?.unreceivedAmount),
      sub: `已收金额 ${formatMoney(overview?.receivedAmount)} / 待结算 ${formatMoney(overview?.pendingSettleAmount)}`,
      title: '已结算未收',
      tone: 'danger',
    },
    {
      icon: <WalletOutlined />,
      main: formatMoney(overview?.receivedAmount),
      sub: `现金 ${formatMoney(overview?.cashReceivedAmount)} / 废纸抵扣 ${formatMoney(overview?.scrapOffsetAmount)}`,
      title: '回款构成',
    },
  ]
}

interface MetricProps {
  icon: ReactNode
  main: string
  sub: string
  title: string
  tone?: 'danger' | 'default' | 'primary' | 'warning'
}
