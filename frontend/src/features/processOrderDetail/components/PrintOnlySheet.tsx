import type { ProcessOrderDetailVO, ProcessOrderPrintViewVO, PrintViewVersion } from '../../../types/processOrder'
import { printVersionProps } from '../printVersionModel'
import PrintPreviewSheet from './PrintPreviewSheet'

interface Props {
  copies: number
  detail: ProcessOrderDetailVO
  version: PrintViewVersion
  view?: ProcessOrderPrintViewVO
}

export default function PrintOnlySheet({ copies, detail, version, view }: Props) {
  return (
    <div className="print-issue-print-root">
      {Array.from({ length: copies }, (_, index) => (
        <div className="print-issue-print-copy" key={`copy-${index + 1}`}>
          <PrintPreviewSheet detail={detail} {...printVersionProps(version, view)} />
        </div>
      ))}
    </div>
  )
}
