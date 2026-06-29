import type {
  CustomerReportVO,
  LossReportVO,
  MachineReportVO,
  MonthlyReportVO,
} from '../../../types/report'

export function summarizeReports(data: {
  customers: CustomerReportVO[]
  losses: LossReportVO[]
  machines: MachineReportVO[]
  monthly: MonthlyReportVO[]
}) {
  const totalOrders = sum(data.monthly, (item) => item.orderCount)
  const totalAmount = sum(data.monthly, (item) => item.totalAmount)
  const totalTon = sum(data.monthly, (item) => item.totalTon)
  const totalFinishWeight = sum(data.monthly, (item) => item.totalFinishWeight)
  const totalLossWeight = sum(data.losses, (item) => item.totalLossWeight)
  const totalOriginalWeight = sum(data.losses, (item) => item.totalOriginalWeight)

  return {
    avgLossRatio: totalOriginalWeight > 0 ? (totalLossWeight / totalOriginalWeight) * 100 : 0,
    machineCount: data.machines.length,
    totalAmount,
    totalFinishWeight,
    totalKnife: sum(data.monthly, (item) => item.totalKnife),
    totalLossWeight,
    totalOrders,
    totalTon,
  }
}

function sum<T>(items: T[], pick: (item: T) => number | undefined) {
  return items.reduce((total, item) => total + Number(pick(item) ?? 0), 0)
}
