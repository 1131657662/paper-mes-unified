import { Alert, Segmented, Space, Tag } from 'antd'
import type { ProcessOrderPrintViewVO, PrintViewVersion } from '../../../types/processOrder'
import { paperVersionText, printVersionMetadata, printVersionWarning } from '../printVersionModel'

interface Props {
  value: PrintViewVersion
  view?: ProcessOrderPrintViewVO
  onChange: (version: PrintViewVersion) => void
}

export default function PrintVersionControl({ value, view, onChange }: Props) {
  const versions = view?.availableVersions ?? ['ISSUED']
  const metadata = printVersionMetadata(view)
  const warning = printVersionWarning(view)
  return (
    <div className="print-version print-issue__screen-only">
      <div className="print-version__switch-row">
        <Segmented<PrintViewVersion>
          aria-label="加工单打印版本"
          value={value}
          options={versions.map((version) => ({ label: versionText(version), value: version }))}
          onChange={onChange}
        />
        <Space size={6} wrap>
          <Tag color={value === 'FINISHED' ? 'green' : 'blue'}>{paperVersionText(value, view?.source)}</Tag>
          {metadata.snapshotTime && <span>生成时间 {metadata.snapshotTime}</span>}
          {metadata.snapshotUser && <span>操作人 {metadata.snapshotUser}</span>}
          {view?.schemaVersion && <span>快照 v{view.schemaVersion}</span>}
        </Space>
      </div>
      {warning && <Alert type="warning" showIcon message={warning} />}
    </div>
  )
}

function versionText(version: PrintViewVersion): string {
  return version === 'FINISHED' ? '完工版本' : '下发版本'
}
