import { AlertTriangle, Edit3, Plus, Power, PowerOff, Search, Truck, X } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { apiRequest } from '../api/client'
import ProviderFormModal from './ProviderFormModal'
import StatusChangeModal from './StatusChangeModal'

export default function ProvidersPanel() {
  const [providers, setProviders] = useState(null)
  const [query, setQuery] = useState('')
  const [modal, setModal] = useState(null)
  const [loading, setLoading] = useState(true)
  const [statusSubmitting, setStatusSubmitting] = useState(false)
  const [statusError, setStatusError] = useState('')
  const [error, setError] = useState('')

  const loadProviders = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setProviders(await apiRequest('/api/catalog/providers'))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadProviders()
  }, [loadProviders])

  const visibleProviders = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    if (!normalized) return providers || []
    return (providers || []).filter((provider) =>
      ['nombre', 'razonSocial', 'cuit', 'telefono', 'email']
        .some((field) => String(provider[field] || '').toLowerCase().includes(normalized)))
  }, [providers, query])

  function handleSaved(saved) {
    setProviders((current) => [...(current || []).filter((item) => item.id !== saved.id), saved]
      .sort((first, second) => first.nombre.localeCompare(second.nombre, 'es', { sensitivity: 'base' })))
    setModal(null)
  }

  async function confirmStatusChange() {
    setStatusSubmitting(true)
    setStatusError('')
    try {
      const updated = await apiRequest(`/api/catalog/providers/${modal.item.id}/status`, { method: 'POST' })
      setProviders((current) => current.map((item) => item.id === updated.id ? updated : item))
      setModal(null)
    } catch (requestError) {
      setStatusError(requestError.message)
    } finally {
      setStatusSubmitting(false)
    }
  }

  return (
    <>
      {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span><button className="icon-button" type="button" aria-label="Cerrar error" onClick={() => setError('')}><X size={18} /></button></div>}
      <section className="catalog-panel directory-panel">
        <header className="directory-toolbar">
          <div><h2>Proveedores registrados</h2><span>{visibleProviders.length} registros visibles</span></div>
          <div className="directory-toolbar__actions">
            <div className="catalog-search"><Search size={17} /><input aria-label="Buscar proveedores" placeholder="Nombre, CUIT o contacto" value={query} onChange={(event) => setQuery(event.target.value)} /></div>
            <button className="primary-button" type="button" aria-label="Nuevo proveedor" title="Nuevo proveedor" onClick={() => setModal({ type: 'provider' })}><Plus size={18} /><span>Nuevo proveedor</span></button>
          </div>
        </header>
        {loading ? <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div> : visibleProviders.length ? (
          <div className="table-scroll">
            <table className="directory-table providers-table">
              <thead><tr><th>Proveedor</th><th>CUIT</th><th>Contacto</th><th>Direccion</th><th>Estado</th><th className="align-right">Acciones</th></tr></thead>
              <tbody>{visibleProviders.map((provider) => <ProviderRow key={provider.id} provider={provider} onEdit={() => setModal({ type: 'provider', item: provider })} onStatus={() => setModal({ type: 'status', item: provider })} />)}</tbody>
            </table>
          </div>
        ) : (
          <div className="catalog-empty"><Truck size={34} /><strong>{query ? 'No hay coincidencias' : 'Todavia no hay proveedores'}</strong><span>{query ? 'Prueba con otra busqueda.' : 'Crea un proveedor para poder registrar sus compras.'}</span>{!query && <button className="primary-button" type="button" onClick={() => setModal({ type: 'provider' })}><Plus size={17} />Crear proveedor</button>}</div>
        )}
      </section>

      {modal?.type === 'provider' && <ProviderFormModal provider={modal.item} onClose={() => setModal(null)} onSaved={handleSaved} />}
      {modal?.type === 'status' && <StatusChangeModal label={modal.item.nombre} active={modal.item.activo} submitting={statusSubmitting} error={statusError} onClose={() => { setModal(null); setStatusError('') }} onConfirm={confirmStatusChange} />}
    </>
  )
}

function ProviderRow({ provider, onEdit, onStatus }) {
  return (
    <tr className={provider.activo ? '' : 'catalog-row--inactive'}>
      <td><div className="catalog-primary"><strong>{provider.nombre}</strong><span>{provider.razonSocial || 'Sin razon social'}</span></div></td>
      <td>{provider.cuit || '-'}</td>
      <td><div className="catalog-primary"><strong>{provider.telefono || 'Sin telefono'}</strong><span>{provider.email || 'Sin correo'}</span></div></td>
      <td className="directory-description">{provider.direccion || '-'}</td>
      <td><span className={provider.activo ? 'record-status' : 'record-status record-status--inactive'}>{provider.activo ? 'Activo' : 'Inactivo'}</span></td>
      <td><div className="catalog-actions"><button className="icon-button" type="button" title="Editar proveedor" aria-label={`Editar ${provider.nombre}`} onClick={onEdit}><Edit3 size={17} /></button><button className={provider.activo ? 'icon-button catalog-action--off' : 'icon-button catalog-action--on'} type="button" title={provider.activo ? 'Desactivar proveedor' : 'Activar proveedor'} aria-label={`${provider.activo ? 'Desactivar' : 'Activar'} ${provider.nombre}`} onClick={onStatus}>{provider.activo ? <PowerOff size={17} /> : <Power size={17} />}</button></div></td>
    </tr>
  )
}
