import { useMutation } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { aiService } from '../services/aiService'
import type { AiAssistRequest } from '../types'

export function useAiAssist(contextEpoch: string) {
  const controllerRef = useRef<AbortController | null>(null)
  const mutation = useMutation({
    mutationFn: async (payload: AiAssistRequest) => {
      controllerRef.current?.abort()
      const controller = new AbortController()
      controllerRef.current = controller
      try {
        return await aiService.assist(payload, controller.signal)
      } finally {
        if (controllerRef.current === controller) controllerRef.current = null
      }
    },
  })

  useEffect(() => () => {
    controllerRef.current?.abort()
    controllerRef.current = null
  }, [contextEpoch])

  return mutation
}
