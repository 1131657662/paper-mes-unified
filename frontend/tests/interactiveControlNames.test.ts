import { readdirSync, readFileSync } from 'node:fs'
import path from 'node:path'
import ts from 'typescript'
import { describe, expect, it } from 'vitest'

interface Finding {
  control: string
  file: string
  line: number
}

const fieldControlTags = new Set([
  'Checkbox',
  'Checkbox.Group',
  'DatePicker',
  'Input',
  'Input.Search',
  'Input.TextArea',
  'InputNumber',
  'Radio.Group',
  'Segmented',
  'Select',
])

describe('交互控件可访问名称', () => {
  it('图标按钮、独立开关和表单控件均提供可读名称', () => {
    expect(scanFiles(path.resolve('src'))).toEqual([])
  })
})

function scanFiles(root: string) {
  return tsxFiles(root).flatMap(scanFile)
}

function tsxFiles(root: string): string[] {
  return readdirSync(root, { withFileTypes: true }).flatMap((entry) => {
    const file = path.join(root, entry.name)
    if (entry.isDirectory()) return tsxFiles(file)
    return entry.name.endsWith('.tsx') && !entry.name.endsWith('.test.tsx') ? [file] : []
  })
}

function scanFile(file: string): Finding[] {
  const source = ts.createSourceFile(
    file,
    readFileSync(file, 'utf8'),
    ts.ScriptTarget.Latest,
    true,
    ts.ScriptKind.TSX,
  )
  const findings: Finding[] = []

  function visit(node: ts.Node) {
    const opening = openingElement(node)
    if (opening) inspectControl(node, opening, source, findings)
    ts.forEachChild(node, visit)
  }

  visit(source)
  return findings
}

function inspectControl(
  node: ts.Node,
  opening: ts.JsxOpeningLikeElement,
  source: ts.SourceFile,
  findings: Finding[],
) {
  const tag = opening.tagName.getText().replace(/\s/g, '')
  const names = attributeNames(opening.attributes)
  const hasName = names.has('aria-label') || names.has('aria-labelledby') || names.has('title')
  if (tag === 'Button' && !hasName && !hasVisibleChild(node)) addFinding('Button', opening, source, findings)
  if (tag === 'Switch' && !hasName && !insideLabelledFormItem(node)) addFinding('Switch', opening, source, findings)
  if (tag === 'div' && names.has('onClick') && !isKeyboardClickable(names)) {
    addFinding('clickable div', opening, source, findings)
  }
  if (fieldControlTags.has(tag) && !hasFieldName(tag, names, node) && !insideLabelledFormItem(node)) {
    addFinding(tag, opening, source, findings)
  }
}

function isKeyboardClickable(names: Set<string>) {
  return names.has('role') && names.has('tabIndex') && names.has('onKeyDown')
}

function openingElement(node: ts.Node) {
  if (ts.isJsxElement(node)) return node.openingElement
  if (ts.isJsxSelfClosingElement(node)) return node
  return undefined
}

function attributeNames(attributes: ts.JsxAttributes) {
  return new Set(
    attributes.properties
      .filter(ts.isJsxAttribute)
      .map((attribute) => attribute.name.getText()),
  )
}

function hasVisibleChild(node: ts.Node) {
  if (!ts.isJsxElement(node)) return false
  return node.children.some((child) => {
    if (ts.isJsxText(child)) return child.getText().trim().length > 0
    if (ts.isJsxExpression(child)) return Boolean(child.expression)
    return true
  })
}

function hasFieldName(tag: string, names: Set<string>, node: ts.Node) {
  if (names.has('aria-label') || names.has('aria-labelledby') || names.has('placeholder')) return true
  return tag === 'Checkbox' && hasVisibleChild(node)
}

function insideLabelledFormItem(node: ts.Node) {
  for (let parent = node.parent; parent; parent = parent.parent) {
    if (!ts.isJsxElement(parent)) continue
    const tag = parent.openingElement.tagName.getText().replace(/\s/g, '')
    if (tag === 'Form.Item') {
      const names = attributeNames(parent.openingElement.attributes)
      return names.has('label') || names.has('hidden')
    }
  }
  return false
}

function addFinding(
  control: string,
  node: ts.Node,
  source: ts.SourceFile,
  findings: Finding[],
) {
  findings.push({
    control,
    file: path.relative(process.cwd(), source.fileName),
    line: source.getLineAndCharacterOfPosition(node.getStart()).line + 1,
  })
}
