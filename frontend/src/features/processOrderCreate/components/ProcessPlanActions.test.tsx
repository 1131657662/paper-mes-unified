import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ProcessPlanActions from './ProcessPlanActions'

describe('主加工方案操作栏', () => {
  it('未保存且有目标时合并为一次保存并应用', () => {
    const markup = renderToStaticMarkup(
      <ProcessPlanActions
        batchTargetCount={3}
        previewReady
        saved={false}
        onExecute={() => undefined}
        saving={false}
      />,
    )

    expect(markup).toContain('已选择 3 卷兼容母卷')
    expect(markup).toContain('保存并应用到 3 卷')
  })

  it('没有兼容目标时只保存当前卷', () => {
    const markup = renderActions(0)

    expect(markup).toContain('保存本卷加工方案')
  })

  it('存在其他兼容卷时允许复制方案', () => {
    const markup = renderActions(1)

    expect(markup).toContain('应用到 1 卷')
  })
})

function renderActions(batchTargetCount: number) {
  return renderToStaticMarkup(
    <ProcessPlanActions
      batchTargetCount={batchTargetCount}
      previewReady
      saved={false}
      onExecute={() => undefined}
      saving={false}
    />,
  )
}

it('已保存且未修改的本卷方案不可重复保存', () => {
  const markup = renderToStaticMarkup(
    <ProcessPlanActions
      batchTargetCount={0}
      previewReady
      onExecute={() => undefined}
      saved
      saving={false}
    />,
  )

  expect(markup).toContain('当前方案已保存')
  expect(markup).toContain('disabled=""')
})

it('未通过后端预览时禁止保存和批量应用', () => {
  const markup = renderToStaticMarkup(
    <ProcessPlanActions batchTargetCount={2} previewReady={false} saved={false}
      onExecute={() => undefined} saving={false} />,
  )

  expect(markup).toContain('尚未通过后端预览')
  expect(markup).toContain('disabled=""')
})
