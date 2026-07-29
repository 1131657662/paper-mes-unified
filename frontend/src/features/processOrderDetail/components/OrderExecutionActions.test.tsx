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

function renderActions(status: number, capabilities: ExecutionCapabilities): string {
  return renderToStaticMarkup(
    <ExecutionActions
      actions={actions}
      capabilities={capabilities}
      hasPrinted
      loading={{}}
      status={status}
    />,
  )
}
