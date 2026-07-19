import { describe, expect, it } from 'vitest'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import type { ProcessOrderDetailVO, ProcessStep } from '../../../types/processOrder'
import RollFilterDropdown from './RollFilterDropdown'
import { renderProcessingQuantity, renderRollCell } from './StepTableCells'
import { buildStepTableColumns, stepMatchesRollFilter } from './StepTableColumns'

describe('工序表母卷筛选', () => {
  it('筛选面板用卷序区分缺少卷号和编号的同品名母卷', () => {
    const rolls = [
      { uuid: 'roll-1', rowSort: 1, paperName: '测试' },
      { uuid: 'roll-2', rowSort: 2, paperName: '测试' },
    ]

    const html = renderToStaticMarkup(createElement(RollFilterDropdown, {
      rolls, selectedKeys: [], setSelectedKeys: () => undefined, confirm: () => undefined,
    }))

    expect(html).toContain('第 01 卷')
    expect(html).toContain('第 02 卷')
    expect(html).toContain('原始卷号 -')
    expect(html).toContain('编号 -')
  })

  it('仅保留属于所选母卷的工序', () => {
    const step = { uuid: 'step-1', originalUuid: 'roll-1' } satisfies ProcessStep

    expect(stepMatchesRollFilter('roll-1', step)).toBe(true)
    expect(stepMatchesRollFilter('roll-2', step)).toBe(false)
  })

  it('单元格同时展示原始卷号和编号', () => {
    const detail = {
      originalRolls: [{ uuid: 'roll-1', rowSort: 1, rollNo: 'R-001', extraNo: 'KH-88',
        paperName: '牛卡纸', actualWeight: 2355 }],
    } as ProcessOrderDetailVO
    const step = { uuid: 'step-1', originalUuid: 'roll-1' } satisfies ProcessStep

    const html = renderToStaticMarkup(renderRollCell(detail, step))

    expect(html).toContain('原始卷号')
    expect(html).toContain('R-001')
    expect(html).toContain('编号')
    expect(html).toContain('KH-88')
    expect(html).toContain('第 01 卷')
    expect(html).toContain('来料 2.355 t')
  })

  it('锯纸按刀数、复卷按吨位显示加工量', () => {
    expect(renderProcessingQuantity({ uuid: 'saw', stepType: 1, knifeCount: 3 })).toBe('3 刀')
    expect(renderProcessingQuantity({ uuid: 'rewind', stepType: 2, processWeight: 3.7 })).toBe('3.700 t')
  })

  it('移除无业务意义的序列并合并重复工序列', () => {
    const columns = buildStepTableColumns({ canManageOrder: false, canAdjustPricing: false,
      onEdit: () => undefined, onDelete: () => undefined, onAdjustPricing: () => undefined,
      onConfigureRoute: () => undefined })
    const titles = columns.map((column) => column.title)

    expect(titles).not.toContain('序')
    expect(titles).not.toContain('工序名称')
    expect(titles).not.toContain('阶段 / 输入')
    expect(titles).not.toContain('标准金额')
    expect(titles).not.toContain('最终金额')
    expect(titles).toContain('工序')
    expect(titles).toContain('加工量')
    expect(titles).toContain('费用')
  })
})
