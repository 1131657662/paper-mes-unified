import type { MachineCapability, MachineCapabilitySaveDTO } from '../../types/machine'

export function capabilitiesToForm(
  capabilities: MachineCapability[] = [],
): MachineCapabilitySaveDTO[] {
  return capabilities.map((item) => ({
    catalogUuid: item.catalogUuid,
    isDefault: item.defaultCapability ? 1 : 0,
    priority: item.priority,
    minWidth: item.minWidth,
    maxWidth: item.maxWidth,
    maxRollWeight: item.maxRollWeight,
    maxDiameter: item.maxDiameter,
    remark: item.remark,
  }))
}

export function clearCapabilityDefaults(
  capabilities: MachineCapabilitySaveDTO[] = [],
): MachineCapabilitySaveDTO[] {
  return capabilities.map((item) => ({ ...item, isDefault: 0 }))
}

export function capabilityRangeText(capability: MachineCapability): string {
  const limits = [
    widthRange(capability),
    capability.maxRollWeight == null ? undefined : `卷重 <= ${capability.maxRollWeight} kg`,
    capability.maxDiameter == null ? undefined : `卷径 <= ${capability.maxDiameter} mm`,
  ].filter(Boolean)
  return limits.join(' · ') || '未限制加工范围'
}

function widthRange(capability: MachineCapability) {
  if (capability.minWidth == null && capability.maxWidth == null) return undefined
  return `门幅 ${capability.minWidth ?? 0}-${capability.maxWidth ?? '不限'} mm`
}
