import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'

export default function Modal({ title, description, onClose, children, footer, size = 'medium', initialFocusRef }) {
  const dialogRef = useRef(null)
  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    const previousFocus = document.activeElement
    document.body.style.overflow = 'hidden'
    initialFocusRef?.current?.focus()

    function handleKeyDown(event) {
      if (event.key === 'Escape') onClose()
      if (!initialFocusRef) return
      if (event.key === 'Enter' && event.repeat) event.preventDefault()
      if (event.key === 'Tab') {
        const controls = [...dialogRef.current.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex="0"]')]
        const first = controls[0]
        const last = controls[controls.length - 1]
        if (!first) { event.preventDefault(); return }
        if (!dialogRef.current.contains(document.activeElement) || (event.shiftKey && document.activeElement === first)) {
          event.preventDefault()
          const target = event.shiftKey ? last : first
          target.focus()
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault()
          first.focus()
        }
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleKeyDown)
      if (initialFocusRef && previousFocus?.isConnected) previousFocus.focus()
    }
  }, [onClose, initialFocusRef])

  return (
    <div className="modal-backdrop" role="presentation">
      <section ref={dialogRef} className={`modal-dialog modal-dialog--${size}`} role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <header className="modal-header">
          <div><h2 id="modal-title">{title}</h2>{description && <p>{description}</p>}</div>
          <button className="icon-button modal-close" type="button" aria-label="Cerrar" title="Cerrar" onClick={onClose}><X size={20} /></button>
        </header>
        <div className="modal-body">{children}</div>
        {footer && <footer className="modal-footer">{footer}</footer>}
      </section>
    </div>
  )
}
