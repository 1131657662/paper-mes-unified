import type { KeyboardEvent } from 'react'

const FIELD_SELECTOR = '[data-back-record-field="true"]'

export function focusFirstBackRecordField() {
  focusField(document.querySelector<HTMLElement>(FIELD_SELECTOR))
}

export function focusNextBackRecordField(
  event: KeyboardEvent<HTMLElement>,
  onExhausted: () => void,
) {
  if (event.nativeEvent.isComposing) return
  event.preventDefault()

  const fields = backRecordFields(event.currentTarget)
  const currentIndex = fields.findIndex((field) => field === event.currentTarget)
  const next = fields[currentIndex + 1]
  if (next) {
    focusField(next)
    return
  }
  onExhausted()
}

function backRecordFields(current: HTMLElement) {
  const scope = current.closest('.back-record-active') ?? document
  return Array.from(scope.querySelectorAll<HTMLElement>(FIELD_SELECTOR))
    .filter((field) => !field.hasAttribute('disabled') && field.tabIndex !== -1)
}

function focusField(field: HTMLElement | null) {
  field?.focus()
  if (field instanceof HTMLInputElement) {
    field.select()
  }
}
