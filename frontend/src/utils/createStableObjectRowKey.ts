export function createStableObjectRowKey(prefix: string): (row: object) => string {
  const keys = new WeakMap<object, string>()
  let sequence = 1

  return (row) => {
    const existing = keys.get(row)
    if (existing) return existing
    const key = `${prefix}-${sequence++}`
    keys.set(row, key)
    return key
  }
}
