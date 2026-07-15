import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { buildBackRecordMetrics } from './backRecordMetrics'
import { buildBackRecordDTO, type BackRecordFormValues } from './backRecordUtils'
import { buildInitialOnSiteOutputGroups, existingFinishSourceUuid } from './backRecordOnSiteOutputModel'
import { buildBackRecordWorkbench, buildWorkItemMetrics } from './backRecordWorkbenchUtils'
import { buildBackRecordSourceOptions } from './backRecordRollOptions'

describe('现场定尺回录数据', () => {
  it('超差放行提交管理员凭据且不再信任授权布尔值', () => {
    const dto = buildBackRecordDTO(onSiteDetail(), {
      rolls: { 'roll-1': { actualWeight: 100 } },
    }, {
      releaseAdminUsername: 'admin',
      releaseAdminPassword: 'secret',
      releaseReason: '复核后确认放行',
    })

    expect(dto).toMatchObject({
      releaseAdminUsername: 'admin',
      releaseAdminPassword: 'secret',
      releaseReason: '复核后确认放行',
    })
    expect(dto).not.toHaveProperty('overToleranceAuthorized')
    expect(dto).not.toHaveProperty('operator')
  })

  it('警告偏差只提交原因而不提交管理员凭据', () => {
    const dto = buildBackRecordDTO(onSiteDetail(), {
      rolls: { 'roll-1': { actualWeight: 100 } },
    }, undefined, { varianceReason: '原纸含水率变化' })

    expect(dto.varianceReason).toBe('原纸含水率变化')
    expect(dto.releaseAdminUsername).toBeUndefined()
    expect(dto.releaseAdminPassword).toBeUndefined()
  })

  it('将逐卷现场门幅写入回录DTO', () => {
    const detail = onSiteDetail()
    const values: BackRecordFormValues = {
      rolls: { 'roll-1': { actualWeight: 100 } },
      onSiteOutputs: { 'roll-roll-1': [{ uuid: 'finish-1', outputType: 'FINISH', originalUuid: 'roll-1', finishWidth: 900, actualWeight: 95 }] },
    }

    const dto = buildBackRecordDTO(detail, values)

    expect(dto.finishes?.[0]).toMatchObject({ uuid: 'finish-1', finishWidth: 900, actualWeight: 95 })
  })

  it('将动态新增成品和切边写入回录DTO', () => {
    const detail = onSiteDetail()
    const values: BackRecordFormValues = {
      rolls: { 'roll-1': { actualWeight: 100 } },
      onSiteOutputs: { 'roll-roll-1': [
        { outputType: 'FINISH', originalUuid: 'roll-1', finishWidth: 900, actualWeight: 80 },
        { outputType: 'TRIM', originalUuid: 'roll-1', finishWidth: 100, actualWeight: 20, actualRemark: '保留' },
      ] },
    }

    const dto = buildBackRecordDTO(detail, values)

    expect(dto.finishes).toEqual([expect.objectContaining({ uuid: undefined, originalUuid: 'roll-1', finishWidth: 900, actualWeight: 80 })])
    expect(dto.trims).toEqual([{ originalUuid: 'roll-1', finishWidth: 100, actualWeight: 20, actualRemark: '保留' }])
  })

  it('统计正式成品缺少现场门幅', () => {
    const detail = onSiteDetail()
    const values: BackRecordFormValues = {
      rolls: { 'roll-1': { actualWeight: 100 } },
      onSiteOutputs: { 'roll-roll-1': [{ outputType: 'FINISH', originalUuid: 'roll-1', actualWeight: 95 }] },
    }

    const metrics = buildBackRecordMetrics(detail, values)

    expect(metrics.missingOnSiteFinishWidth).toBe(1)
  })

  it('ignores sparse output rows while switching work items', () => {
    const detail = onSiteDetail()
    const values: BackRecordFormValues = {
      onSiteOutputs: {
        'roll-roll-1': [
          undefined,
          { outputType: 'FINISH', originalUuid: 'roll-1', finishWidth: 900, actualWeight: 80 },
        ],
      },
    }

    const metrics = buildBackRecordMetrics(detail, values)

    expect(metrics.finishActualTotal).toBe(80)
  })

  it('ignores sparse output rows in work item closure metrics', () => {
    const detail = onSiteDetail()
    const item = buildBackRecordWorkbench(detail).items[0]
    expect(item).toBeDefined()
    if (!item) return
    const values: BackRecordFormValues = {
      onSiteOutputs: { [item.key]: [undefined] },
    }

    const metrics = buildWorkItemMetrics(item, values)

    expect(metrics.finishActual).toBe(0)
  })

  it('initializes outputs for every onsite work item before navigation', () => {
    const detail = twoOnSiteRollDetail()
    const workbench = buildBackRecordWorkbench(detail)

    const groups = buildInitialOnSiteOutputGroups(detail, workbench.items)

    expect(Object.values(groups)).toEqual([
      [expect.objectContaining({ uuid: 'finish-1', originalUuid: 'roll-1' })],
      [expect.objectContaining({ uuid: 'finish-2', originalUuid: 'roll-2' })],
    ])
  })

  it('counts every onsite output before navigation', () => {
    const detail = twoOnSiteRollDetail()
    const workbench = buildBackRecordWorkbench(detail)
    const onSiteOutputs = buildInitialOnSiteOutputGroups(detail, workbench.items)

    const metrics = buildBackRecordMetrics(detail, { onSiteOutputs })

    expect(metrics.missingOnSiteFinishWidth).toBe(2)
  })

  it('keeps original roll numbering after excluding direct shipments', () => {
    const detail = twoOnSiteRollDetail()
    detail.originalRolls.splice(1, 0, { uuid: 'direct-roll', processMode: 3, originalWidth: 1200 })

    const options = buildBackRecordSourceOptions(detail.originalRolls)

    expect(options.map((option) => option.label)).toEqual([
      expect.stringContaining('母卷 1 /'),
      expect.stringContaining('母卷 3 /'),
    ])
  })

  it('多母卷时按成品关系带出唯一真实来源', () => {
    const productions = [{
      originalUuid: 'roll-1',
      finishes: [{ uuid: 'finish-1', sources: [{ originalUuid: 'roll-2' }] }],
    }]

    expect(existingFinishSourceUuid(productions, 'finish-1')).toBe('roll-2')
  })

  it('单件成品有多个真实来源时不自动猜测', () => {
    const productions = [{
      originalUuid: 'roll-1',
      finishes: [{ uuid: 'finish-1', sources: [
        { originalUuid: 'roll-1' },
        { originalUuid: 'roll-2' },
      ] }],
    }]

    expect(existingFinishSourceUuid(productions, 'finish-1')).toBeUndefined()
  })
})

function onSiteDetail(): ProcessOrderDetailVO {
  return {
    order: { uuid: 'order-1' },
    originalRolls: [{ uuid: 'roll-1', processMode: 2, originalWidth: 1200 }],
    rolls: [],
    finishRolls: [{
      uuid: 'finish-1',
      finishRollNo: 'A000001',
      finishWidth: 0,
      isSpare: 0,
      rollNoStatus: 1,
      sourceType: 1,
    }],
    steps: [],
    rollProductions: [{
      originalUuid: 'roll-1',
      processMode: 2,
      finishes: [{ uuid: 'finish-1', finishWidth: 0 }],
    }],
  }
}

function twoOnSiteRollDetail(): ProcessOrderDetailVO {
  const detail = onSiteDetail()
  return {
    ...detail,
    originalRolls: [
      ...detail.originalRolls,
      { uuid: 'roll-2', processMode: 2, originalWidth: 1200 },
    ],
    finishRolls: [
      ...detail.finishRolls,
      { uuid: 'finish-2', finishRollNo: 'A000002', finishWidth: 0, isSpare: 0, rollNoStatus: 1, sourceType: 1 },
    ],
    rollProductions: [
      ...(detail.rollProductions ?? []),
      { originalUuid: 'roll-2', processMode: 2, finishes: [{ uuid: 'finish-2', finishWidth: 0 }] },
    ],
  }
}
