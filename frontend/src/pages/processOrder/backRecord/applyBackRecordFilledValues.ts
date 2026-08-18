import type { FormInstance } from 'antd/es/form'
import type { BackRecordFormValues } from './backRecordUtils'

interface Options {
  form: Pick<FormInstance<BackRecordFormValues>, 'getFieldsValue' | 'setFieldsValue'>
  onDirty?: () => void
  onValuesFilled?: (values: BackRecordFormValues) => void
  values: BackRecordFormValues
}

export function applyBackRecordFilledValues(options: Options): void {
  const current = options.form.getFieldsValue(true) as BackRecordFormValues
  const values = mergeBackRecordFilledValues(current, options.values)
  options.form.setFieldsValue(values)
  options.onValuesFilled?.(values)
  options.onDirty?.()
}

/** Keep an operator-confirmed source-roll entry when a later fill is requested. */
export function mergeBackRecordFilledValues(
  current: BackRecordFormValues,
  filled: BackRecordFormValues,
): BackRecordFormValues {
  const currentRolls = current.rolls ?? {}
  const filledRolls = filled.rolls ?? {}
  const rollUuids = new Set([...Object.keys(currentRolls), ...Object.keys(filledRolls)])
  const rolls: NonNullable<BackRecordFormValues['rolls']> = {}
  for (const uuid of rollUuids) {
    const currentRoll = currentRolls[uuid]
    const filledRoll = filledRolls[uuid]
    const value = !filledRoll ? currentRoll
      : isConfirmedRoll(currentRoll) ? { ...filledRoll, ...currentRoll } : filledRoll
    if (value) rolls[uuid] = value
  }
  return { ...filled, rolls }
}

function isConfirmedRoll(value?: NonNullable<BackRecordFormValues['rolls']>[string]) {
  return value?.weightEntryMode === 'MEASURED' || value?.weightEntryMode === 'CONFIRM_REFERENCE'
}
