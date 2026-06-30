import type { ReactNode } from 'react'
import { StatisticCard } from '@ant-design/pro-components'
import {
  AlertOutlined,
  BankOutlined,
  InboxOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import type { DashboardMetrics } from '../../../types/dashboard'
import { formatKg, formatMoney, formatPercent } from '../../report/utils/reportFormatters'

interface Props {
  metrics?: DashboardMetrics
}

export default function DashboardMetricGrid({ metrics }: Props) {
  const cards: MetricCardProps[] = [
    {
      icon: <ThunderboltOutlined />,
      label: '本月完成加工',
      main: `${metrics?.monthOrderCount ?? 0} 单`,
      sub: `原卷 ${formatKg(metrics?.monthOriginalWeight)}`,
      tip: `成品 ${formatKg(metrics?.monthFinishWeight)} / 加工费 ${formatMoney(metrics?.monthAmount)}`,
      tone: 'blue',
    },
    {
      icon: <InboxOutlined />,
      label: '成品库存',
      main: `${metrics?.inStockFinishCount ?? 0} 卷`,
      sub: `${formatKg(metrics?.inStockFinishWeight)} 可出库`,
      tip: '已入库、未出库的正式成品',
      tone: 'cyan',
    },
    {
      icon: <BankOutlined />,
      label: '应收未收',
      main: formatMoney(metrics?.receivableAmount),
      sub: `${metrics?.receivableCount ?? 0} 张结算单未结清`,
      tip: '结算后仍需财务跟进',
      tone: 'green',
    },
    {
      icon: <AlertOutlined />,
      label: '本月损耗',
      main: formatPercent(metrics?.monthLossRatio),
      sub: `${formatKg(metrics?.monthLossWeight)} 损耗重量`,
      tip: '来自回录闭合后的损耗统计',
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
