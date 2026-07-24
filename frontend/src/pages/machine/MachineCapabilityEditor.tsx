import { Checkbox, Input, InputNumber, Switch, Tag, Typography } from 'antd'
import type { ProcessCatalog } from '../../types/processCatalog'
import type { MachineCapabilitySaveDTO } from '../../types/machine'

interface Props {
  catalogs: ProcessCatalog[]
  enabled: boolean
  value?: MachineCapabilitySaveDTO[]
  onChange?: (value: MachineCapabilitySaveDTO[]) => void
}

export default function MachineCapabilityEditor({ catalogs, enabled, value = [], onChange }: Props) {
  const selected = new Map(value.map((item) => [item.catalogUuid, item]))
  const toggle = (catalogUuid: string, checked: boolean) => {
    const next = checked
      ? [...value, { catalogUuid, isDefault: 0, priority: 100 }]
      : value.filter((item) => item.catalogUuid !== catalogUuid)
    onChange?.(next)
  }
  const patch = (catalogUuid: string, changes: Partial<MachineCapabilitySaveDTO>) => {
    onChange?.(value.map((item) => item.catalogUuid === catalogUuid ? { ...item, ...changes } : item))
  }
  return (
    <div className="machine-capability-editor">
      {catalogs.map((catalog) => {
        const capability = selected.get(catalog.uuid)
        return (
          <div className={capability ? 'machine-capability-row is-selected' : 'machine-capability-row'} key={catalog.uuid}>
            <CapabilityIdentity catalog={catalog} checked={!!capability}
              onChange={(checked) => toggle(catalog.uuid, checked)} />
            {capability && (
              <CapabilityFields capability={capability} enabled={enabled} processName={catalog.name}
                onChange={(changes) => patch(catalog.uuid, changes)} />
            )}
          </div>
        )
      })}
    </div>
  )
}

function CapabilityIdentity({ catalog, checked, onChange }: {
  catalog: ProcessCatalog
  checked: boolean
  onChange: (checked: boolean) => void
}) {
  return (
    <div className="machine-capability-identity">
      <Checkbox aria-label={`启用${catalog.name}能力`} checked={checked}
        onChange={(event) => onChange(event.target.checked)} />
      <div>
        <Typography.Text strong>{catalog.name}</Typography.Text>
        <Tag>{categoryText(catalog.category)}</Tag>
      </div>
    </div>
  )
}

function CapabilityFields({ capability, enabled, processName, onChange }: {
  capability: MachineCapabilitySaveDTO
  enabled: boolean
  processName: string
  onChange: (changes: Partial<MachineCapabilitySaveDTO>) => void
}) {
  return (
    <div className="machine-capability-fields">
      <label><span>默认</span><Switch aria-label={`${processName}默认资源`} size="small" disabled={!enabled}
        checked={enabled && capability.isDefault === 1}
        onChange={(checked) => onChange({ isDefault: checked ? 1 : 0 })} /></label>
      <NumberField label="顺序" processName={processName} limits={{ min: 1, max: 9999 }} value={capability.priority}
        onChange={(priority) => onChange({ priority })} />
      <NumberField label="最小门幅" processName={processName} suffix="mm" limits={{ min: 1 }} value={capability.minWidth}
        onChange={(minWidth) => onChange({ minWidth })} />
      <NumberField label="最大门幅" processName={processName} suffix="mm" limits={{ min: 1 }} value={capability.maxWidth}
        onChange={(maxWidth) => onChange({ maxWidth })} />
      <NumberField label="最大卷重" processName={processName} suffix="kg" limits={{ min: 0.001 }} value={capability.maxRollWeight}
        onChange={(maxRollWeight) => onChange({ maxRollWeight })} />
      <NumberField label="最大卷径" processName={processName} suffix="mm" limits={{ min: 1 }} value={capability.maxDiameter}
        onChange={(maxDiameter) => onChange({ maxDiameter })} />
      <label className="machine-capability-remark"><span>能力备注</span><Input aria-label={`${processName}能力备注`} maxLength={255}
        value={capability.remark} onChange={(event) => onChange({ remark: event.target.value || undefined })} /></label>
    </div>
  )
}

function NumberField({ label, processName, suffix, limits, value, onChange }: {
  label: string; processName: string; suffix?: string; limits: { min: number; max?: number }; value?: number
  onChange: (value?: number) => void
}) {
  return <label><span>{label}</span><InputNumber aria-label={`${processName}${label}`} addonAfter={suffix}
    min={limits.min} max={limits.max}
    value={value} onChange={(next) => onChange(next ?? undefined)} /></label>
}

function categoryText(category: ProcessCatalog['category']) {
  return { PRODUCTION: '生产', SERVICE: '服务', QUALITY: '质量', PACKAGING: '包装', LOGISTICS: '物流' }[category]
}
