export function roundWeightTotal(value: number | null | undefined): number {
  const numeric = Number(value ?? 0)
  return Number.isFinite(numeric) ? Math.round(numeric) : 0
}

export function allocateIntegerWeight(total: number, bases: number[]): number[] {
  if (!bases.length) return []
  if (total == null) return bases.map(() => 0)
  if (!Number.isFinite(total) || total < 0) {
    throw new Error('预估重量分配总重必须是非负有限数值')
  }
  const target = Math.max(0, roundWeightTotal(total))
  const positiveBases = bases.map((value) => {
    const numeric = Number(value)
    return Number.isFinite(numeric) ? Math.max(0, numeric) : 0
  })
  const basisTotal = positiveBases.reduce((sum, value) => sum + value, 0)
  if (target <= 0) return bases.map(() => 0)
  if (basisTotal <= 0) return allocateEvenly(target, bases.length)

  const floors = positiveBases.map((basis) => Math.floor(target * basis / basisTotal))
  const fractions = positiveBases.map((basis, index) => ({
    index,
    fraction: target * basis / basisTotal - (floors[index] ?? 0),
  }))
  let remaining = target - floors.reduce((sum, value) => sum + value, 0)
  fractions.sort((left, right) => right.fraction - left.fraction || left.index - right.index)
  for (let index = 0; index < remaining; index += 1) {
    const targetIndex = fractions[index % fractions.length]?.index
    if (targetIndex != null) floors[targetIndex] = (floors[targetIndex] ?? 0) + 1
  }
  return floors
}

function allocateEvenly(total: number, count: number): number[] {
  const base = Math.floor(total / count)
  const remainder = total - base * count
  return Array.from({ length: count }, (_, index) => base + (index < remainder ? 1 : 0))
}
