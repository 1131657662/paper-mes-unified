import { message, Modal } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BizError } from '../../api/request'
import { authorizeDeliveryConfirmation } from './deliveryConfirmAuthorization'

vi.mock('antd', () => ({
  Modal: { confirm: vi.fn() },
  message: { error: vi.fn(), warning: vi.fn() },
}))

describe('出库签收现结风险授权', () => {
  beforeEach(() => vi.clearAllMocks())

  it('无风险时只提交一次且不携带授权', async () => {
    const submit = vi.fn().mockResolvedValue(undefined)

    await expect(authorizeDeliveryConfirmation(submit)).resolves.toBe(true)

    expect(submit).toHaveBeenCalledTimes(1)
    expect(submit).toHaveBeenCalledWith(false)
    expect(Modal.confirm).not.toHaveBeenCalled()
  })

  it('遇到E010且确认后使用授权重试', async () => {
    confirmModalWith('ok')
    const submit = vi.fn()
      .mockRejectedValueOnce(new BizError('需要授权', 400, 'E010'))
      .mockResolvedValueOnce(undefined)

    await expect(authorizeDeliveryConfirmation(submit, 2)).resolves.toBe(true)

    expect(submit.mock.calls).toEqual([[false], [true]])
  })

  it('遇到E010但取消时不重试', async () => {
    confirmModalWith('cancel')
    const submit = vi.fn().mockRejectedValue(new BizError('需要授权', 400, 'E010'))

    await expect(authorizeDeliveryConfirmation(submit)).resolves.toBe(false)

    expect(submit).toHaveBeenCalledTimes(1)
  })

  it('无放行权限遇到E010时提示转交且不重试', async () => {
    const submit = vi.fn().mockRejectedValue(new BizError('需要授权', 400, 'E010'))

    await expect(authorizeDeliveryConfirmation(submit, 1, false)).resolves.toBe(false)

    expect(submit).toHaveBeenCalledTimes(1)
    expect(Modal.confirm).not.toHaveBeenCalled()
    expect(message.warning).toHaveBeenCalledWith(
      '该出库单存在未结清现结款项，请由财务或管理员账号签收放行',
    )
  })

  it('非E010错误继续抛给调用方', async () => {
    const error = new BizError('数据已变化', 400, 'E006')
    const submit = vi.fn().mockRejectedValue(error)

    await expect(authorizeDeliveryConfirmation(submit)).rejects.toBe(error)

    expect(Modal.confirm).not.toHaveBeenCalled()
  })
})

function confirmModalWith(action: 'ok' | 'cancel') {
  vi.mocked(Modal.confirm).mockImplementation((config) => {
    const callback = action === 'ok' ? config.onOk : config.onCancel
    void callback?.(() => undefined)
    return { destroy: vi.fn(), update: vi.fn() }
  })
}
