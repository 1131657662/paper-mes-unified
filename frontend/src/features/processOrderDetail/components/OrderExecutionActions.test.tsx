import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ExecutionActions, {
  type ExecutionActionHandlers,
  type ExecutionCapabilities,
} from './OrderExecutionActions'

const actions: ExecutionActionHandlers = {
  onBackRecord: () => undefined,
  onCalcFee: () => undefined,
  onChangeStatus: () => undefined,
  onEditDraft: () => undefined,
  onGoDelivery: () => undefined,
  onGoSettle: () => undefined,
  onManageRolls: () => undefined,
  onPrint: () => undefined,
  onPrepareReissue: () => undefined,
  onSnapshotDiff: () => undefined,
  onVoidOrder: () => undefined,
}

const noCapabilities: ExecutionCapabilities = {
  canBackRecord: false,
  canCreateOrder: false,
  canManageDelivery: false,
  canManageOrder: false,
  canManageSettlement: false,
}

describe('加工单详情执行操作', () => {
  it('未确认打印时禁用转待回录', () => {
    const markup = renderActions(2, { ...noCapabilities, canManageOrder: true }, false)

    expect(markup).toContain('disabled=""')
    expect(markup).toContain('title="请先确认已完成打印"')
  })

  it('确认打印后允许转待回录', () => {
    const markup = renderActions(2, { ...noCapabilities, canManageOrder: true })

    expect(markup).toContain('转待回录')
    expect(markup).not.toContain('disabled=""')
  })

  it('已结算加工单仍可创建出库且不再生成结算', () => {
    const markup = renderActions(5, { ...noCapabilities, canManageDelivery: true })

    expect(markup).toContain('创建出库')
    expect(markup).not.toContain('生成结算')
    expect(markup).not.toContain('暂无可执行动作')
  })

  it('无出库权限时已结算加工单不显示空操作按钮', () => {
    const markup = renderActions(5, noCapabilities)

    expect(markup).not.toContain('暂无可执行动作')
    expect(markup).not.toContain('创建出库')
  })
})

function renderActions(status: number, capabilities: ExecutionCapabilities, hasPrinted = true): string {
  return renderToStaticMarkup(
    <ExecutionActions
      actions={actions}
      capabilities={capabilities}
      hasPrinted={hasPrinted}
      loading={{}}
      status={status}
    />,
  )
}
