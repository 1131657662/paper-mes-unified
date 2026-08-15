import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ExecutionActions, {
  buildMoreItems,
  type ExecutionActionHandlers,
  type ExecutionCapabilities,
} from './OrderExecutionActions'

const capabilities: ExecutionCapabilities = {
  canBackRecord: false,
  canCreateOrder: false,
  canManageDelivery: false,
  canManageOrder: true,
  canManageSettlement: false,
  canManageRollDisposition: false,
}

const actions: ExecutionActionHandlers = {
  onBackRecord: () => undefined,
  onCalcFee: () => undefined,
  onChangeStatus: () => undefined,
  onEditDraft: () => undefined,
  onGoDelivery: () => undefined,
  onGoSettle: () => undefined,
  onManageRolls: () => undefined,
  onConfirmPrintAndToRecord: () => undefined,
  onPrepareReissue: () => undefined,
  onPrint: () => undefined,
  onSnapshotDiff: () => undefined,
  onVoidOrder: () => undefined,
  onRollDisposition: () => undefined,
}

describe('下发后变更入口', () => {
  it('加工中管理员看到变更并重新下发命令', () => {
    const markup = renderToStaticMarkup(
      <ExecutionActions actions={actions} capabilities={capabilities} hasPrinted loading={{}} status={2} />,
    )
    const menuItems = buildMoreItems(2, actions, capabilities)
    const reissueItem = menuItems.find((item) => item?.key === 'reissue')

    expect(markup).toContain('更多操作')
    expect(reissueItem && 'label' in reissueItem ? reissueItem.label : undefined).toBe('申请变更并重新下发')
  })
})
