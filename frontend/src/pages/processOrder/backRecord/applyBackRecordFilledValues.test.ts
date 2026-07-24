import { describe, expect, it, vi } from 'vitest'
import { applyBackRecordFilledValues } from './applyBackRecordFilledValues'
import type { BackRecordFormValues } from './backRecordUtils'

describe('apply back-record filled values', () => {
  it('marks programmatic theory fill as dirty', () => {
    const setFieldsValue = vi.fn()
    const onDirty = vi.fn()
    const onValuesFilled = vi.fn()
    const values: BackRecordFormValues = { rolls: { 'roll-1': { actualWeight: 100 } } }

    applyBackRecordFilledValues({ form: { setFieldsValue }, onDirty, onValuesFilled, values })

    expect(setFieldsValue).toHaveBeenCalledWith(values)
    expect(onValuesFilled).toHaveBeenCalledWith(values)
    expect(onDirty).toHaveBeenCalledOnce()
  })
})
