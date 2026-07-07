import type { Rule } from 'antd/es/form'

export const PASSWORD_RULE_TEXT = '密码需为8-32位，且包含字母和数字'

export function strongPasswordRules(requiredMessage = '请输入密码'): Rule[] {
  return [
    { required: true, message: requiredMessage },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/, message: PASSWORD_RULE_TEXT },
  ]
}
