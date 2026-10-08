import { useCallback, useEffect, useRef, useState } from 'react'

export type AsyncState<T> =
  | { status: 'loading' }
  | { status: 'error'; error: unknown }
  | { status: 'ready'; data: T }

type Settled<T> = { runId: string; state: AsyncState<T> }

// Runs `load` on mount and again whenever `key` changes or reload() is called.
// Results of an outdated run are dropped.
export function useAsync<T>(
  key: string,
  load: (signal: AbortSignal) => Promise<T>,
): AsyncState<T> & { reload: () => void } {
  const [version, setVersion] = useState(0)
  const runId = `${key}#${version}`
  const [settled, setSettled] = useState<Settled<T> | null>(null)

  const loadRef = useRef(load)
  useEffect(() => {
    loadRef.current = load
  })

  useEffect(() => {
    const controller = new AbortController()
    loadRef
      .current(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setSettled({ runId, state: { status: 'ready', data } })
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setSettled({ runId, state: { status: 'error', error } })
      })
    return () => controller.abort()
  }, [runId])

  const reload = useCallback(() => setVersion((v) => v + 1), [])

  const state: AsyncState<T> = settled?.runId === runId ? settled.state : { status: 'loading' }
  return { ...state, reload }
}
