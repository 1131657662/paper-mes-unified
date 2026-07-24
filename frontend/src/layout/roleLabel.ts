const ROLE_LABELS: Record<string, string> = {
  admin: '管理员',
  finance: '财务',
  operator: '录单员（兼容）',
  order_clerk: '制单员',
  recorder: '回录员',
  viewer: '只读人员',
  warehouse: '出库员',
}

export function roleLabel(roleCode?: string): string {
  return roleCode ? ROLE_LABELS[roleCode] ?? '访客' : '访客'
}
