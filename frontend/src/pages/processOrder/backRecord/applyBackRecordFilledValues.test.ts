import { describe, expect, it, vi } from 'vitest'
import { applyBackRecordFilledValues } from './applyBackRecordFilledValues'
import type { BackRecordFormValues } from './backRecordUtils'

describe('apply back-record filled values', () => {
  it('marks programmatic theory fill as dirty', () => {
    const setFieldsValue = vi.fn()
    const getFieldsValue = vi.fn(() => ({}))
    const onDirty = vi.fn()
    const onValuesFilled = vi.fn()
    const values: BackRecordFormValues = { rolls: { 'roll-1': { actualWeight: 100 } } }

    applyBackRecordFilledValues({ form: { getFieldsValue, setFieldsValue }, onDirty, onValuesFilled, values })

    expect(setFieldsValue).toHaveBeenCalledWith(values)
    expect(onValuesFilled).toHaveBeenCalledWith(values)
    expect(onDirty).toHaveBeenCalledOnce()
  })

  it('preserves an already confirmed source roll when filling again', () => {
    const setFieldsValue = vi.fn()
    const getFieldsValue = vi.fn(() => ({
      rolls: {
        'roll-1': { actualWeight: 2000, weightEntryMode: 'MEASURED' as const, remark: '现场复称' },
      },
    }))

    applyBackRecordFilledValues({
      form: { getFieldsValue, setFieldsValue },
      values: { rolls: { 'roll-1': { actualWeight: 1, weightEntryMode: 'CONFIRM_REFERENCE' } } },
    })

    expect(setFieldsValue).toHaveBeenCalledWith({
      rolls: {
        'roll-1': { actualWeight: 2000, weightEntryMode: 'MEASURED', remark: '现场复称' },
      },
    })
  })
})
