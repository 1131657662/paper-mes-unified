import type { DeliveryDetailVO } from '../../types/delivery'
import {
  deliveryDetailSpecText,
  deliveryOriginalSnapshotText,
  deliveryProcessSnapshotText,
  formatKg,
  formatTon,
} from '../../features/delivery/utils/deliveryFormatters'
import '../../pages/documentModule.css'

interface Props {
  detail: DeliveryDetailVO
}

export default function DeliveryPrintSheet({ detail }: Props) {
  const { order, details } = detail

  return (
    <div className="document-print-area document-print-area--delivery">
      <div className="document-print-sheet">
        <header className="document-print-sheet__header">
          <div>
            <h1>出库单</h1>
            <div className="document-print-sheet__meta">
              <span>出库单号：{order.deliveryNo}</span>
              <span>客户：{order.customerName}</span>
              <span>出库日期：{order.deliveryDate}</span>
            </div>
          </div>
          <div className="document-print-sheet__summary">
            <span>{order.totalCount} 卷</span>
            <span>{formatTon(order.totalWeight)}</span>
            <span>车牌：{order.carNo || '-'}</span>
            <span>柜号：{order.containerNo || '-'}</span>
          </div>
        </header>

        <section>
          <h2>出库明细</h2>
          <table className="document-print-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>加工单</th>
                <th>卷号</th>
                <th>品名</th>
                <th>克重</th>
                <th>规格</th>
                <th>件重</th>
                <th>出库重量</th>
                <th>原纸信息</th>
                <th>加工方式</th>
                <th>工艺摘要</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              {details.map((item, index) => (
                <tr key={item.uuid}>
                  <td>{index + 1}</td>
                  <td>{item.orderNo || '-'}</td>
                  <td>{item.finishRollNo || '-'}</td>
                  <td>{item.paperName || '-'}</td>
                  <td>{item.gramWeight ? `${item.gramWeight}g` : '-'}</td>
                  <td>{deliveryDetailSpecText(item)}</td>
                  <td>{formatKg(item.actualWeight)}</td>
                  <td>{formatKg(item.outWeight)}</td>
                  <td>{deliveryOriginalSnapshotText(item)}</td>
                  <td>{item.processModeText || '-'}</td>
                  <td>{deliveryProcessSnapshotText(item)}</td>
                  <td>{item.remark || item.actualRemark || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <footer className="document-print-sheet__footer">
          <span>提货人：{order.pickerName || '-'}</span>
          <span>签收人：{order.signUser || ''}</span>
          <span>仓库签字：</span>
          <span>司机签字：</span>
        </footer>
      </div>
    </div>
  )
}
