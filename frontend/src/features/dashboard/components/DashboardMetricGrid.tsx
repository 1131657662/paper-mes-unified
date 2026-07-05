import type { ReactNode } from 'react'
import { StatisticCard } from '@ant-design/pro-components'
import {
  AlertOutlined,
  BankOutlined,
  InboxOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import type { DashboardMetrics } from '../../../types/dashboard'
import { formatMoney, formatPercent, formatTonFromKg } from '../../report/utils/reportFormatters'

interface Props {
  metrics?: DashboardMetrics
}

export default function DashboardMetricGrid({ metrics }: Props) {
  const cards: MetricCardProps[] = [
    {
      icon: <ThunderboltOutlined />,
      label: '本月完成加工',
      main: `${metrics?.monthOrderCount ?? 0} 单`,
      sub: `原卷 ${formatTonFromKg(metrics?.monthOriginalWeight)}`,
      tip: `成品 ${formatTonFromKg(metrics?.monthFinishWeight)} / 加工应收 ${formatMoney(metrics?.monthAmount)}`,
      tone: 'blue',
    },
    {
      icon: <InboxOutlined />,
      label: '成品库存',
      main: `${metrics?.inStockFinishCount ?? 0} 卷`,
      sub: `${formatTonFromKg(metrics?.inStockFinishWeight)} 可出库`,
      tip: '已入库、未出库的正式成品',
      tone: 'cyan',
    },
    {
      icon: <BankOutlined />,
      label: '已结算未收',
      main: formatMoney(metrics?.receivableAmount),
      sub: `${metrics?.receivableCount ?? 0} 单待跟进`,
      tip: `现金 ${formatMoney(metrics?.cashReceivedAmount)} / 废纸 ${formatMoney(metrics?.scrapOffsetAmount)}`,
      tone: 'green',
    },
    {
      icon: <AlertOutlined />,
      label: '本月损耗',
      main: formatPercent(metrics?.monthLossRatio),
      sub: `${formatTonFromKg(metrics?.monthLossWeight)} 损耗重量`,
      tip: `待结算应收 ${formatMoney(metrics?.pendingSettleAmount)}，不与已结算未收混算`,
      tone: 'orange',
    },
  ]

  return (
    <StatisticCard.Group className="dashboard-metrics" gutter={[12, 12]} ghost>
      {cards.map((item) => (
        <StatisticCard
          className={`dashboard-metric dashboard-metric--${item.tone}`}
          colSpan={{ xs: 24, sm: 12, lg: 6 }}
          footer={<span className="dashboard-metric__footer">{item.tip}</span>}
          key={item.label}
          statistic={{
            description: item.sub,
            icon: <span className="dashboard-metric__icon">{item.icon}</span>,
            title: item.label,
            value: item.main,
          }}
        />
      ))}
    </StatisticCard.Group>
  )
}

interface MetricCardProps {
  icon: ReactNode
  label: string
  main: string
  sub: string
  tip: string
  tone: 'blue' | 'cyan' | 'green' | 'orange'
}
