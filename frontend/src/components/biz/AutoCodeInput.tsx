import { Input } from 'antd'
import MesTooltip from './MesTooltip'

const tooltipText = '\u7cfb\u7edf\u81ea\u52a8\u751f\u6210\uff0c\u4e0d\u652f\u6301\u624b\u5de5\u4fee\u6539'
const placeholderText = '\u4fdd\u5b58\u540e\u7cfb\u7edf\u81ea\u52a8\u751f\u6210'

interface AutoCodeInputProps {
  editing: boolean
}

export default function AutoCodeInput({ editing }: AutoCodeInputProps) {
  return (
    <MesTooltip title={tooltipText}>
      <span className="auto-code-input">
        <Input placeholder={editing ? undefined : placeholderText} disabled readOnly />
      </span>
    </MesTooltip>
  )
}
