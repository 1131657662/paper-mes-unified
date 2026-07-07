import { useEffect, useState } from 'react'
import { Image, Spin } from 'antd'
import { fetchProtectedFileBlob } from '../../api/protectedFile'
import './ProtectedImage.css'

interface ProtectedImageProps {
  alt?: string
  className?: string
  path?: string
}

interface ObjectUrlState {
  failed: boolean
  loading: boolean
  url?: string
}

const emptyState: ObjectUrlState = {
  failed: false,
  loading: false,
}

export default function ProtectedImage({ alt = '', className, path }: ProtectedImageProps) {
  const state = useProtectedObjectUrl(path)
  const rootClassName = ['protected-image', className].filter(Boolean).join(' ')

  if (!path) return null
  if (state.loading) {
    return <span className={`${rootClassName} protected-image--placeholder`}><Spin size="small" /></span>
  }
  if (state.failed || !state.url) {
    return <span className={`${rootClassName} protected-image--placeholder`}>图片不可用</span>
  }
  return <Image className={rootClassName} src={state.url} alt={alt} />
}

function useProtectedObjectUrl(path?: string): ObjectUrlState {
  const [state, setState] = useState<ObjectUrlState>(emptyState)

  useEffect(() => {
    if (!path) {
      setState(emptyState)
      return undefined
    }
    let cancelled = false
    let objectUrl: string | undefined
    setState({ failed: false, loading: true })
    fetchProtectedFileBlob(path)
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob)
        if (cancelled) {
          URL.revokeObjectURL(objectUrl)
          return
        }
        setState({ failed: false, loading: false, url: objectUrl })
      })
      .catch(() => {
        if (!cancelled) setState({ failed: true, loading: false })
      })

    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [path])

  return state
}
