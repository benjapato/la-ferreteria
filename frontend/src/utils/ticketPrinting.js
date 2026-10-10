import { apiRequest } from '../api/client'

export async function sendTicket(saleId, requestId) {
  try {
    return await apiRequest('/api/printing/sales/' + saleId, { method: 'POST', body: { solicitudId: requestId || crypto.randomUUID() } })
  } catch {
    return { estado: 'REVISAR', mensaje: 'La venta esta guardada. No se pudo confirmar la impresion. Revise la cola de Windows antes de reimprimir para evitar duplicados.' }
  }
}
