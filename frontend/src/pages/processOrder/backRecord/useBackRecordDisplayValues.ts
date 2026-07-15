import { Form } from 'antd'
import type { BackRecordFormValues } from './backRecordUtils'

export function useBackRecordDisplayValues(
  form: ReturnType<typeof Form.useForm<BackRecordFormValues>>[0],
  fallback: BackRecordFormValues,
): BackRecordFormValues {
  const watched: BackRecordFormValues = {
    finishes: Form.useWatch('finishes', form),
    onSiteOutputs: Form.useWatch('onSiteOutputs', form),
    rolls: Form.useWatch('rolls', form),
    steps: Form.useWatch('steps', form),
    trims: Form.useWatch('trims', form),
  }
  return {
    ...fallback,
    ...watched,
    finishes: { ...fallback.finishes, ...watched.finishes },
    onSiteOutputs: { ...fallback.onSiteOutputs, ...watched.onSiteOutputs },
    rolls: { ...fallback.rolls, ...watched.rolls },
    steps: { ...fallback.steps, ...watched.steps },
    trims: { ...fallback.trims, ...watched.trims },
  }
}
