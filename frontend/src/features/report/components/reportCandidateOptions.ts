export interface ReportCandidateOption {
  label: string
  value: string
}

export function mergeCandidateOptions(...groups: ReportCandidateOption[][]): ReportCandidateOption[] {
  const unique = new Map<string, ReportCandidateOption>()
  groups.flat().forEach((option) => unique.set(option.value, option))
  return [...unique.values()]
}
