import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ProcessPlanActions from './ProcessPlanActions'

describe('主加工方案操作栏', () => {
  it('区分本卷保存与兼容卷批量应用', () => {
    const markup = renderToStaticMarkup(
      <ProcessPlanActions
        batchTargetCount={3}
        checkedCount={4}
        onlyCurrentTarget={false}
        saved={false}
        onApply={() => undefined}
        onSave={() => undefined}
        saving={false}
      />,
    )

    expect(markup).toContain('批量范围：已选 4 卷，可应用 3 卷')
    expect(markup).toContain('保存本卷加工方案')
    expect(markup).toContain('批量应用加工方案（3 卷）')
    expect(markup).not.toContain('保存当前')
    expect(markup).not.toContain('应用到选中')
  })

  it('唯一批量目标是当前卷时提示直接保存本卷', () => {
    const markup = renderActions(true)

    expect(markup).toContain('当前只有本卷')
    expect(markup).toContain('disabled=""')
  })

  it('唯一批量目标是其他卷时仍允许复制方案', () => {
    const markup = renderActions(false)

    expect(markup).not.toContain('当前只有本卷')
    expect(markup).not.toContain('disabled=""')
  })
})

function renderActions(onlyCurrentTarget: boolean) {
  return renderToStaticMarkup(
    <ProcessPlanActions
      batchTargetCount={1}
      checkedCount={1}
      onlyCurrentTarget={onlyCurrentTarget}
      saved={false}
      onApply={() => undefined}
      onSave={() => undefined}
      saving={false}
    />,
  )
}

it('已保存且未修改的本卷方案不可重复保存', () => {
  const markup = renderToStaticMarkup(
    <ProcessPlanActions
      batchTargetCount={0}
      checkedCount={1}
      onlyCurrentTarget={false}
      onApply={() => undefined}
      onSave={() => undefined}
      saved
      saving={false}
    />,
  )

  expect(markup).toContain('当前加工方案已保存')
  expect(markup).toContain('disabled=""')
})
