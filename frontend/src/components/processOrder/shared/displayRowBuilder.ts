import type { RollProductionVO } from '../../../types/processOrder'
import type { DisplayRow, MergeGroup } from './types'

/**
 * 将后端返回的 rollProductions 转换为展示行（DisplayRow[]）。
 *
 * 核心逻辑：
 * 1. 发现 mode 5 合并组（通过 finish.sources 跨卷引用）
 * 2. 将合并组中的卷归为一个 DisplayRow（去重成品）
 * 3. 非合并卷各占一个 DisplayRow
 */
export function buildDisplayRows(productions: RollProductionVO[]): DisplayRow[] {
  if (!productions?.length) return []

  // Phase 1: 发现合并组
  const mergeGroups = findMergeGroups(productions)

  // Phase 2: 构建集合便于查找
  const mergeSourceUuids = new Set<string>()
  const mergeMainUuidBySource = new Map<string, string>()
  for (const group of mergeGroups) {
    for (const uuid of group.sourceUuids) {
      mergeSourceUuids.add(uuid)
      mergeMainUuidBySource.set(uuid, group.mainUuid)
    }
  }

  // Phase 3: 构建 DisplayRow
  const prodByUuid = new Map(productions.map((p) => [p.originalUuid!, p]))
  const usedInMerge = new Set<string>()
  const rows: DisplayRow[] = []

  // 先处理合并组
  for (const group of mergeGroups) {
    const allProds = group.allUuids.map((uuid) => prodByUuid.get(uuid)!).filter(Boolean)
    if (allProds.length < 2) continue

    const mainProd = allProds.find((p) => p.originalUuid === group.mainUuid) || allProds[0]
    const sourceProds = allProds.filter((p) => p.originalUuid !== group.mainUuid)

    // 去重成品
    const finishSet = new Set<string>()
    const allFinishes = allProds.flatMap((p) =>
      (p.finishes ?? []).filter((f) => {
        if (finishSet.has(f.uuid)) return false
        finishSet.add(f.uuid)
        return true
      }),
    )

    const steps = [...(mainProd.steps ?? [])].sort((a, b) => (a.stepSort ?? 0) - (b.stepSort ?? 0))
    const rewindParams = mainProd.rewindParams ?? []
    const rewindMode = rewindParams[0]?.paramMode

    for (const uuid of group.allUuids) usedInMerge.add(uuid)

    const sourceLabel = allProds.map((p, i) => {
      const name = p.rollNo || p.paperName || `原纸${i + 1}`
      return `${name}`
    }).join(' / ')

    rows.push({
      key: `merge-${group.mainUuid}`,
      seq: 0, // 后续统一赋值
      label: sourceLabel,
      isMergeGroup: true,
      originalUuids: group.allUuids,
      rollProductions: allProds,
      mainProduction: mainProd,
      sourceProductions: sourceProds,
      steps,
      rewindParams,
      finishes: allFinishes,
      totalKnifeCount: steps.reduce((s, step) => s + (step.knifeCount ?? 0), 0),
      totalEstimateWeight: allFinishes.reduce((s, f) => s + (f.estimateWeight ?? 0), 0),
      rewindMode,
      isDirectShip: mainProd.processMode === 3,
      hasConfig: rewindParams.length > 0 || (mainProd.finishes?.length ?? 0) > 0,
    })
  }

  // 处理非合并卷
  for (const prod of productions) {
    const uuid = prod.originalUuid!
    if (usedInMerge.has(uuid)) continue

    const steps = [...(prod.steps ?? [])].sort((a, b) => (a.stepSort ?? 0) - (b.stepSort ?? 0))
    const rewindParams = prod.rewindParams ?? []
    const rewindMode = rewindParams[0]?.paramMode

    rows.push({
      key: `single-${uuid}`,
      seq: 0,
      label: prod.rollNo || prod.paperName || '-',
      isMergeGroup: false,
      originalUuids: [uuid],
      rollProductions: [prod],
      mainProduction: prod,
      sourceProductions: [],
      steps,
      rewindParams,
      finishes: prod.finishes ?? [],
      totalKnifeCount: steps.reduce((s, step) => s + (step.knifeCount ?? 0), 0),
      totalEstimateWeight: (prod.finishes ?? []).reduce((s, f) => s + (f.estimateWeight ?? 0), 0),
      rewindMode,
      isDirectShip: prod.processMode === 3,
      hasConfig: rewindParams.length > 0 || (prod.finishes?.length ?? 0) > 0,
    })
  }

  // 按原纸顺序排序并分配序号
  // 将已被合并的 UUID 替换为合并组
  const seqOrder: { uuid: string; isMerge: boolean }[] = []
  const addedMerges = new Set<string>()
  for (const prod of productions) {
    const uuid = prod.originalUuid!
    if (mergeMainUuidBySource.has(uuid)) {
      // 这是来源卷，找到它的主卷
      const mainUuid = mergeMainUuidBySource.get(uuid)!
      if (!addedMerges.has(mainUuid)) {
        addedMerges.add(mainUuid)
        seqOrder.push({ uuid: mainUuid, isMerge: true })
      }
    } else if (mergeSourceUuids.has(uuid)) {
      // 来源卷，已在上面处理
    } else if (!usedInMerge.has(uuid)) {
      seqOrder.push({ uuid, isMerge: false })
    }
  }
  // 加上未被上面覆盖的合并组主卷
  for (const group of mergeGroups) {
    if (!addedMerges.has(group.mainUuid)) {
      addedMerges.add(group.mainUuid)
      seqOrder.push({ uuid: group.mainUuid, isMerge: true })
    }
  }

  // 根据 seqOrder 排序 rows
  const sortedRows: DisplayRow[] = []
  const rowByMainUuid = new Map(
    rows.map((r) => [r.isMergeGroup ? r.mainProduction.originalUuid! : r.originalUuids[0], r]),
  )
  for (const item of seqOrder) {
    const row = rowByMainUuid.get(item.uuid)
    if (row) sortedRows.push(row)
  }
  // 添加未在 seqOrder 中的行
  for (const row of rows) {
    if (!sortedRows.includes(row)) sortedRows.push(row)
  }

  sortedRows.forEach((row, i) => { row.seq = i + 1 })

  return sortedRows
}

/* ---------- 合并组发现 ---------- */

function findMergeGroups(productions: RollProductionVO[]): MergeGroup[] {
  // 构建 UUID → 成品中跨卷来源的映射
  // graph[uuid] = Set of uuids that uuid's finishes reference as sources
  const crossLinks = new Map<string, Set<string>>()

  for (const prod of productions) {
    const uuid = prod.originalUuid!
    for (const finish of prod.finishes ?? []) {
      for (const source of finish.sources ?? []) {
        if (source.originalUuid && source.originalUuid !== uuid) {
          // 成品属于 prod，但引用了 source.originalUuid 作为来源
          // → uuid 和 source.originalUuid 应该在同一个合并组
          if (!crossLinks.has(uuid)) crossLinks.set(uuid, new Set())
          crossLinks.get(uuid)!.add(source.originalUuid)
          if (!crossLinks.has(source.originalUuid)) crossLinks.set(source.originalUuid, new Set())
          crossLinks.get(source.originalUuid)!.add(uuid)
        }
      }
    }
  }

  if (crossLinks.size === 0) return []

  // Union-Find 连通分量
  const allUuids = new Set<string>()
  for (const uuid of crossLinks.keys()) allUuids.add(uuid)
  for (const targets of crossLinks.values()) for (const t of targets) allUuids.add(t)

  const parent = new Map<string, string>()
  for (const uuid of allUuids) parent.set(uuid, uuid)

  const find = (x: string): string => {
    const p = parent.get(x)!
    if (p !== x) { parent.set(x, find(p)) }
    return parent.get(x)!
  }
  const union = (a: string, b: string) => {
    const ra = find(a), rb = find(b)
    if (ra !== rb) parent.set(ra, rb)
  }

  for (const [uuid, targets] of crossLinks.entries()) {
    for (const t of targets) {
      union(uuid, t)
    }
  }

  // 收集连通分量
  const components = new Map<string, Set<string>>()
  for (const uuid of allUuids) {
    const root = find(uuid)
    if (!components.has(root)) components.set(root, new Set())
    components.get(root)!.add(uuid)
  }

  // 构建 MergeGroup（大小 > 1）
  const groups: MergeGroup[] = []
  for (const members of components.values()) {
    if (members.size < 2) continue
    const memberList = Array.from(members)
    // 找主卷：有 rewindParams 且 paramMode === 5 的那个
    const prodMap = new Map(productions.map((p) => [p.originalUuid!, p]))
    const mainMember = memberList.find((uuid) => {
      const prod = prodMap.get(uuid)
      return prod?.rewindParams?.some((p) => p.paramMode === 5)
    })
    const mainUuid = mainMember || memberList[0]
    groups.push({
      mainUuid,
      sourceUuids: memberList.filter((u) => u !== mainUuid),
      allUuids: memberList,
    })
  }

  return groups
}
