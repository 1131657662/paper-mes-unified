import { Tag } from 'antd'
import dayjs from 'dayjs'

const ACTION_TONE: Record<string, string> = {
  回退: 'warning',
  超差放行: 'error',
  作废卷号: 'default',
  回录: 'processing',
  补打: 'default',
  出库确认: 'success',
  出库放行: 'warning',
  结算: 'success',
  收款: 'success',
  字段修改: 'blue',
  新增用户: 'success',
  编辑用户: 'blue',
  账号启停: 'warning',
  重置密码: 'error',
}

export function actionTag(value: string) {
  return <Tag className="mes-data-tag" color={ACTION_TONE[value] || 'default'}>{value || '-'}</Tag>
}

export function dateText(value?: string) {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}

export function logText(value?: string | number | null) {
  if (value == null || value === '') return '-'
  const text = String(value)
  return looksMojibake(text) ? decodeMojibakeSegments(text) : text
}

function looksMojibake(text: string) {
  return /[\u0080-\u00ffÃÂåæçèéäöü]/.test(text)
}

function decodeMojibakeSegments(text: string) {
  return text.replace(/[\u0080-\u00ffÃÂåæçèéäöü]+/g, (segment) => decodeMojibakeSegment(segment))
}

function decodeMojibakeSegment(segment: string) {
  try {
    const bytes = Uint8Array.from(Array.from(segment, (char) => char.charCodeAt(0) & 0xff))
    const decoded = new TextDecoder('utf-8', { fatal: true }).decode(bytes)
    return shouldUseDecoded(decoded, segment) ? decoded : segment
  } catch {
    return segment
  }
}

function shouldUseDecoded(decoded: string, source: string) {
  return hasCjk(decoded) && replacementScore(decoded) < replacementScore(source)
}

function hasCjk(text: string) {
  return /[\u4e00-\u9fff]/.test(text)
}

function replacementScore(text: string) {
  return (text.match(/[�ÃÂåæçèé\u0080-\u00ff]/g) ?? []).length
}
