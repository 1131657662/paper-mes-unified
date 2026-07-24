import type { FormInstance } from 'antd/es/form'
import type { BackRecordFormValues } from './backRecordUtils'

interface Options {
  form: Pick<FormInstance<BackRecordFormValues>, 'setFieldsValue'>
  onDirty?: () => void
  onValuesFilled?: (values: BackRecordFormValues) => void
  values: BackRecordFormValues
}

export function applyBackRecordFilledValues(options: Options): void {
  options.form.setFieldsValue(options.values)
  options.onValuesFilled?.(options.values)
  options.onDirty?.()
}
