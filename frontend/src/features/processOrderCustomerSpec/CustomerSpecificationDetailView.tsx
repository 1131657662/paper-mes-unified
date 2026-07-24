import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import CustomerSpecificationComparisonTable from './CustomerSpecificationComparisonTable'
import CustomerSpecificationSummaryTable from './CustomerSpecificationSummaryTable'
import type { FinishCustomerSpec } from './customerSpecTypes'

interface Props { rows: FinishedProductRow[]; specs?: FinishCustomerSpec[] }

export default function CustomerSpecificationDetailView({ rows, specs }: Props) {
  return (
    <div className="customer-specification-detail-view">
      <section aria-labelledby="customer-specification-summary-title">
        <h3 id="customer-specification-summary-title">规格汇总</h3>
        <CustomerSpecificationSummaryTable rows={rows} specs={specs} />
      </section>
      <section aria-labelledby="customer-specification-items-title">
        <h3 id="customer-specification-items-title">逐件明细</h3>
        <CustomerSpecificationComparisonTable rows={rows} specs={specs} />
      </section>
    </div>
  )
}
