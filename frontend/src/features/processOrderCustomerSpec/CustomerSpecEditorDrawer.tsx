import { Button, Drawer, Form, Input, Space, message } from 'antd'
import { useState } from 'react'
import { useUnsavedChangesGuard } from '../../hooks/useUnsavedChangesGuard'
import {
  applyBulkSpecification,
  applyToSamePhysicalSpec,
  applyWeightRule,
  buildFinishCustomerRevisionRequest,
  createCustomerSpecDrafts,
  fillSelectedFromFirst,
  isWeightRuleReady,
  prepareCustomerSpecPreviewRows,
  updateCustomerSpecDraft,
  type BulkSpecificationValues,
  type CustomerSpecDraft,
  type WeightRule,
} from './customerSpecDraftModel'
import { applyPastedCustomerSpecs } from './customerSpecPasteModel'
import type { FinishCustomerRevisionPreview, FinishCustomerRevisionRequest } from './customerSpecTypes'
import CustomerSpecBatchToolbar from './CustomerSpecBatchToolbar'
import CustomerSpecEditTable from './CustomerSpecEditTable'
import CustomerSpecPasteModal from './CustomerSpecPasteModal'
import CustomerSpecPreviewBand from './CustomerSpecPreviewBand'
import CustomerWeightRulePanel from './CustomerWeightRulePanel'
import { usePreviewFinishCustomerSpecs, usePublishFinishCustomerSpecs } from './useFinishCustomerSpecs'
import './CustomerSpecEditor.css'

interface Props { data: FinishCustomerRevisionPreview; open: boolean; orderUuid: string; onClose: () => void }
interface PreviewBundle { result: FinishCustomerRevisionPreview; request: FinishCustomerRevisionRequest }
interface PreviewValidation { reason: string; selected: string[]; rule: WeightRule; rulePending: boolean }

export default function CustomerSpecEditorDrawer({ data, open, orderUuid, onClose }: Props) {
  const [rows, setRows] = useState(() => createCustomerSpecDrafts(data.items))
  const [selected, setSelectedState] = useState(() => data.items.map((item) => item.finishUuid))
  const [bulk, setBulk] = useState<BulkSpecificationValues>({})
  const [weightRule, setWeightRule] = useState<WeightRule>({ mode: 'KEEP', skipZero: true })
  const [weightRulePending, setWeightRulePending] = useState(false)
  const [reason, setReasonState] = useState('')
  const [reasonForm] = Form.useForm<{ reason: string }>()
  const [pasteOpen, setPasteOpen] = useState(false)
  const [bundle, setBundle] = useState<PreviewBundle>()
  const [requestId] = useState(createRequestId)
  const previewMutation = usePreviewFinishCustomerSpecs()
  const publishMutation = usePublishFinishCustomerSpecs()
  const { clearDirty, markDirty, runIfClean } = useUnsavedChangesGuard()

  const changeRows = (next: CustomerSpecDraft[]) => { markDirty(); setRows(next); setBundle(undefined) }
  const setSelected = (next: string[]) => { markDirty(); setSelectedState(next); setBundle(undefined) }
  const setReason = (next: string) => { markDirty(); setReasonState(next); setBundle(undefined) }
  const changeWeightRule = (next: WeightRule) => { markDirty(); setWeightRule(next); setWeightRulePending(true); setBundle(undefined) }
  const applyCurrentWeightRule = () => { changeRows(applyWeightRule(rows, selected, weightRule)); setWeightRulePending(false) }
  const buildRequest = (nextRows: CustomerSpecDraft[]) => buildFinishCustomerRevisionRequest(data.orderVersion, reason, requestId, nextRows, selected)

  const preview = async () => {
    try {
      await reasonForm.validateFields(['reason'])
    } catch {
      return
    }
    if (!validate({ reason, selected, rule: weightRule, rulePending: weightRulePending })) return
    const previewRows = prepareCustomerSpecPreviewRows({
      rows, selected, rule: weightRule, applyPendingRule: weightRulePending,
    })
    changeRows(previewRows)
    setWeightRulePending(false)
    const request = buildRequest(previewRows)
    const result = await previewMutation.mutateAsync({ orderUuid, values: request })
    setBundle({ result, request })
  }

  const publish = async () => {
    if (!bundle || bundle.result.hasErrors) return
    const result = await publishMutation.mutateAsync({ orderUuid, values: bundle.request })
    message.success(`客户口径 V${result.revisionNo} 已发布`)
    clearDirty()
    onClose()
  }

  const guardedClose = () => runIfClean(onClose)

  return (
    <>
      <Drawer
        className="customer-spec-editor" destroyOnClose footer={<Footer bundle={bundle} previewing={previewMutation.isPending} publishing={publishMutation.isPending} onCancel={guardedClose} onPreview={preview} onPublish={publish} />}
        open={open} placement="right" title={`批量维护客户口径 · ${data.orderNo ?? ''}`} width="min(1280px, calc(100vw - 24px))" onClose={guardedClose}
      >
        <div className="customer-spec-editor__body">
          <CustomerSpecBatchToolbar disabled={!selected.length} values={bulk} onChange={setBulk} onApply={() => changeRows(applyBulkSpecification(rows, selected, bulk))} onFillDown={() => changeRows(fillSelectedFromFirst(rows, selected))} onSameSpec={() => changeRows(applyToSamePhysicalSpec(rows, selected))} onPaste={() => setPasteOpen(true)} />
          <CustomerWeightRulePanel disabled={!selected.length} pending={weightRulePending} value={weightRule} onChange={changeWeightRule} onApply={applyCurrentWeightRule} />
          <CustomerSpecEditTable rows={rows} selected={selected} previewItems={bundle?.result.items} onSelect={setSelected} onUpdate={(uuid, values) => changeRows(updateCustomerSpecDraft(rows, uuid, values))} />
          <div className="customer-spec-editor__review">
            <Form form={reasonForm} initialValues={{ reason }} onValuesChange={(_, values) => setReason(values.reason ?? '')}>
              <Form.Item name="reason" label="本次调整原因" rules={[{ required: true, whitespace: true, message: '请填写本次调整原因' }]}>
                <Input.TextArea maxLength={255} rows={2} showCount />
              </Form.Item>
            </Form>
            <CustomerSpecPreviewBand data={bundle?.result} />
          </div>
        </div>
      </Drawer>
      {pasteOpen && <CustomerSpecPasteModal open onCancel={() => setPasteOpen(false)} onApply={(text) => { changeRows(applyPastedCustomerSpecs(rows, text)); setPasteOpen(false) }} />}
    </>
  )
}

function Footer({ bundle, previewing, publishing, onCancel, onPreview, onPublish }: { bundle?: PreviewBundle; previewing: boolean; publishing: boolean; onCancel: () => void; onPreview: () => void; onPublish: () => void }) {
  return <div className="customer-spec-editor__footer"><Button onClick={onCancel}>取消</Button><Space><Button loading={previewing} onClick={onPreview}>预览计算</Button><Button type="primary" disabled={!bundle || bundle.result.hasErrors} loading={publishing} onClick={onPublish}>发布版本</Button></Space></div>
}

function validate(values: PreviewValidation) {
  if (!values.selected.length) { message.warning('请至少选择一件成品'); return false }
  if (!values.reason.trim()) { message.warning('请填写本次调整原因'); return false }
  if (values.rulePending && !isWeightRuleReady(values.rule)) { message.warning('请先完整填写当前重量规则'); return false }
  return true
}

function createRequestId() {
  return globalThis.crypto?.randomUUID?.() ?? `customer-spec-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
