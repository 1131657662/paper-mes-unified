import { Form } from 'antd'
import type { BackRecordFormValues } from './backRecordUtils'

export function useBackRecordDisplayValues(
  form: ReturnType<typeof Form.useForm<BackRecordFormValues>>[0],
  fallback: BackRecordFormValues,
): BackRecordFormValues {
  const watched: BackRecordFormValues = {
    finishes: Form.useWatch('finishes', { form, preserve: true }),
    onSiteOutputs: Form.useWatch('onSiteOutputs', { form, preserve: true }),
    rolls: Form.useWatch('rolls', { form, preserve: true }),
    steps: Form.useWatch('steps', { form, preserve: true }),
    trims: Form.useWatch('trims', { form, preserve: true }),
  }
  return mergeBackRecordDisplayValues(fallback, watched)
}

export function mergeBackRecordDisplayValues(
  fallback: BackRecordFormValues,
  watched: BackRecordFormValues,
): BackRecordFormValues {
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
