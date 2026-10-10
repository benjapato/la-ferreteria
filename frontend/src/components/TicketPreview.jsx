import { useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { apiRequest } from '../api/client'

export default function TicketPreview({ sale }) {
  const [pages, setPages] = useState(1)
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    setPage(0)
    setError('')
    apiRequest('/api/printing/sales/' + sale.id + '/preview')
      .then((data) => { if (active) setPages(data.paginas) })
      .catch(() => { if (active) setError('No se pudo cargar la vista previa del ticket.') })
    return () => { active = false }
  }, [sale.id])
  return <div className="ticket-preview">
    {error ? <div role="alert">{error}</div> : <img
      src={'/api/printing/sales/' + sale.id + '/preview/' + page}
      alt={'Ticket de venta ' + sale.id + '. Total: ' + sale.total + '. Cliente: ' + sale.cliente}
      onError={() => setError('No se pudo cargar la imagen del ticket.')} />}
    {pages > 1 && <nav className="ticket-preview__pages" aria-label="Paginas del ticket">
      <button type="button" className="icon-button" title="Pagina anterior" aria-label="Pagina anterior" disabled={page === 0} onClick={() => setPage(page - 1)}><ChevronLeft size={18} /></button>
      <span>{page + 1} / {pages}</span>
      <button type="button" className="icon-button" title="Pagina siguiente" aria-label="Pagina siguiente" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}><ChevronRight size={18} /></button>
    </nav>}
  </div>
}
