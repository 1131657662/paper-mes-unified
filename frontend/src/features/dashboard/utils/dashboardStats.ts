import type {
  CustomerReportVO,
  LossReportVO,
  MachineReportVO,
  MonthlyReportVO,
} from '../../../types/report'

export function buildDashboardSummary(data: DashboardReportData) {
  const totalOrders = sum(data.monthly, (item) => item.orderCount)
  const totalTon = sum(data.monthly, (item) => item.totalTon)
  const totalAmount = sum(data.monthly, (item) => item.totalAmount)
  const totalFinishWeight = sum(data.monthly, (item) => item.totalFinishWeight)
  const totalLossWeight = sum(data.losses, (item) => item.totalLossWeight)
  const originalWeight = sum(data.losses, (item) => item.totalOriginalWeight)

  return {
    activeCustomerCount: data.customers.length,
    avgLossRatio: originalWeight > 0 ? (totalLossWeight / originalWeight) * 100 : 0,
    machineCount: data.machines.length,
    totalAmount,
    totalFinishWeight,
    totalLossWeight,
    totalOrders,
    totalTon,
  }
}

export function toCustomerRank(customers: CustomerReportVO[]) {
  return customers.slice(0, 6).map((item) => ({
    key: item.customerUuid,
    label: item.customerName,
    meta: `${item.orderCount ?? 0} 单 / ${numberText(item.totalTon)}t`,
    value: item.totalAmount ?? 0,
  }))
}

export function toMachineRank(machines: MachineReportVO[]) {
  return machines.slice(0, 6).map((item) => ({
    key: item.machineUuid,
    label: item.machineName,
    meta: `${item.rollCount ?? 0} 卷 / ${item.totalKnife ?? 0} 刀`,
    value: item.totalOutputWeight ?? 0,
  }))
}

function numberText(value?: number) {
  return Number(value ?? 0).toLocaleString('zh-CN', { maximumFractionDigits: 3 })
}

function sum<T>(items: T[], pick: (item: T) => number | undefined) {
  return items.reduce((total, item) => total + Number(pick(item) ?? 0), 0)
}

interface DashboardReportData {
  customers: CustomerReportVO[]
  losses: LossReportVO[]
  machines: MachineReportVO[]
  monthly: MonthlyReportVO[]
}
