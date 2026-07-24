import { Button, Drawer, Form, Input, Space, message } from 'antd'
import { useState } from 'react'
import CustomerSpecBatchToolbar from '../processOrderCustomerSpec/CustomerSpecBatchToolbar'
import CustomerSpecPasteModal from '../processOrderCustomerSpec/CustomerSpecPasteModal'
import CustomerWeightRulePanel from '../processOrderCustomerSpec/CustomerWeightRulePanel'
import {
  isWeightRuleReady,
  type BulkSpecificationValues,
  type WeightRule,
} from '../processOrderCustomerSpec/customerSpecDraftModel'
import {
  applyDeliveryBulkSpecification, applyDeliveryPaste, applyDeliverySamePhysical,
  applyDeliveryWeightRule, buildDeliveryCustomerRevisionRequest,
  createDeliveryCustomerDrafts, fillDeliverySelected, updateDeliveryCustomerDraft,
  prepareDeliveryCustomerPreviewRows,
  type DeliveryCustomerSpecDraft,
} from './deliveryCustomerDraftModel'
import type { DeliveryCustomerRevisionPreview, DeliveryCustomerRevisionRequest } from './deliveryCustomerSpecTypes'
import DeliveryCustomerPreviewBand from './DeliveryCustomerPreviewBand'
import DeliveryCustomerSpecEditTable from './DeliveryCustomerSpecEditTable'
import { usePreviewDeliveryCustomerSpecs, usePublishDeliveryCustomerSpecs } from './useDeliveryCustomerSpecs'
import '../processOrderCustomerSpec/CustomerSpecEditor.css'
import './DeliveryCustomerSpec.css'

interface Props { data: DeliveryCustomerRevisionPreview; open: boolean; uuid: string; onClose: () => void }
interface PreviewBundle { result: DeliveryCustomerRevisionPreview; request: DeliveryCustomerRevisionRequest }

export default function DeliveryCustomerRevisionDrawer({ data, open, uuid, onClose }: Props) {
  const [rows, setRows] = useState(() => createDeliveryCustomerDrafts(data.items))
  const [selected, setSelectedState] = useState(() => data.items.map((item) => item.deliveryDetailUuid))
  const [bulk, setBulk] = useState<BulkSpecificationValues>({})
  const [rule, setRule] = useState<WeightRule>({ mode: 'KEEP', skipZero: true })
  const [rulePending, setRulePending] = useState(false)
  const [reason, setReasonState] = useState('')
  const [pasteOpen, setPasteOpen] = useState(false)
  const [bundle, setBundle] = useState<PreviewBundle>()
  const [requestId] = useState(createRequestId)
  const previewMutation = usePreviewDeliveryCustomerSpecs()
  const publishMutation = usePublishDeliveryCustomerSpecs()

  const changeRows = (next: DeliveryCustomerSpecDraft[]) => { setRows(next); setBundle(undefined) }
  const setSelected = (next: string[]) => { setSelectedState(next); setBundle(undefined) }
  const setReason = (next: string) => { setReasonState(next); setBundle(undefined) }
  const changeRule = (next: WeightRule) => { setRule(next); setRulePending(true); setBundle(undefined) }
  const applyCurrentRule = () => { changeRows(applyDeliveryWeightRule(rows, selected, rule)); setRulePending(false) }
  const request = (nextRows: DeliveryCustomerSpecDraft[]) => buildDeliveryCustomerRevisionRequest(data.deliveryVersion, reason, requestId, nextRows, selected)
  const preview = async () => {
    if (!validate(reason, selected, rule, rulePending)) return
    const previewRows = prepareDeliveryCustomerPreviewRows(rows, selected, rule, rulePending)
    changeRows(previewRows)
    setRulePending(false)
    const values = request(previewRows)
    const result = await previewMutation.mutateAsync({ uuid, values })
    setBundle({ result, request: values })
  }
  const publish = async () => {
    if (!bundle || bundle.result.hasErrors) return
    const result = await publishMutation.mutateAsync({ uuid, values: bundle.request })
    message.success(`客户更正版 V${result.revisionNo} 已发布`)
    onClose()
  }

  return (
    <>
      <Drawer className="delivery-customer-editor" destroyOnClose footer={<Footer bundle={bundle} previewing={previewMutation.isPending} publishing={publishMutation.isPending} onCancel={onClose} onPreview={preview} onPublish={publish} />} open={open} title={`创建客户更正版 · ${data.deliveryNo}`} width="min(1320px, calc(100vw - 24px))" onClose={onClose}>
        <div className="delivery-customer-editor__body">
          <CustomerSpecBatchToolbar disabled={!selected.length} values={bulk} onChange={setBulk} onApply={() => changeRows(applyDeliveryBulkSpecification(rows, selected, bulk))} onFillDown={() => changeRows(fillDeliverySelected(rows, selected))} onSameSpec={() => changeRows(applyDeliverySamePhysical(rows, selected))} onPaste={() => setPasteOpen(true)} />
          <CustomerWeightRulePanel disabled={!selected.length} pending={rulePending} value={rule} onChange={changeRule} onApply={applyCurrentRule} />
          <DeliveryCustomerSpecEditTable rows={rows} selected={selected} previewItems={bundle?.result.items} onSelect={setSelected} onUpdate={(id, values) => changeRows(updateDeliveryCustomerDraft(rows, id, values))} />
          <div className="delivery-customer-editor__review"><Form.Item label="更正原因" required><Input.TextArea maxLength={255} rows={2} showCount value={reason} onChange={(event) => setReason(event.target.value)} /></Form.Item><DeliveryCustomerPreviewBand data={bundle?.result} /></div>
        </div>
      </Drawer>
      {pasteOpen && <CustomerSpecPasteModal open onCancel={() => setPasteOpen(false)} onApply={(text) => { changeRows(applyDeliveryPaste(rows, text)); setPasteOpen(false) }} />}
    </>
  )
}

function Footer({ bundle, previewing, publishing, onCancel, onPreview, onPublish }: { bundle?: PreviewBundle; previewing: boolean; publishing: boolean; onCancel: () => void; onPreview: () => void; onPublish: () => void }) {
  return <div className="delivery-customer-editor__footer"><Button onClick={onCancel}>取消</Button><Space><Button loading={previewing} onClick={onPreview}>预览更正版</Button><Button type="primary" disabled={!bundle || bundle.result.hasErrors} loading={publishing} onClick={onPublish}>发布更正版</Button></Space></div>
}

function validate(reason: string, selected: string[], rule: WeightRule, rulePending: boolean) {
  if (!selected.length) { message.warning('请至少选择一件出库成品'); return false }
  if (!reason.trim()) { message.warning('请填写更正原因'); return false }
  if (rulePending && !isWeightRuleReady(rule)) { message.warning('请先完整填写当前重量规则'); return false }
  return true
}

function createRequestId() { return globalThis.crypto?.randomUUID?.() ?? `delivery-customer-${Date.now()}-${Math.random().toString(16).slice(2)}` }
