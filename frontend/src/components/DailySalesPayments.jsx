import { ArrowUpRight, Banknote, BookUser, Building2, CreditCard } from 'lucide-react'
import { Link } from 'react-router-dom'

const methods = [
  { key: 'efectivo', label: 'Efectivo', icon: Banknote, tone: 'green' },
  { key: 'transferencia', label: 'Transferencia', icon: Building2, tone: 'blue' },
  { key: 'debito', label: 'Debito', icon: CreditCard, tone: 'teal' },
  { key: 'credito', label: 'Credito', icon: CreditCard, tone: 'amber' },
]
const money = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export default function DailySalesPayments({ payments }) {
  const total = Number(payments.totalCobrado || 0)
  const account = Number(payments.cuentaCorriente || 0)

  return (
    <section className="daily-payments" aria-labelledby="daily-payments-title">
      <header className="daily-payments__header">
        <h3 id="daily-payments-title">Cobros de ventas de hoy</h3>
        <dl className="daily-payments__collected"><dt>Total cobrado</dt><dd>{money.format(total)}</dd></dl>
      </header>

      <ul className="daily-payments__methods">
        {methods.map(({ key, label, icon: Icon, tone }) => {
          const amount = Number(payments[key] || 0)
          return (
            <li key={key} className={`daily-payment daily-payment--${key} daily-payment--${tone}`}>
              <span className="daily-payment__label"><span className="daily-payment__icon"><Icon size={18} aria-hidden="true" /></span>{label}</span>
              <strong className="daily-payment__amount">{money.format(amount)}</strong>
            </li>
          )
        })}
      </ul>

      <footer className="daily-payments__footer">
        <dl className="daily-payments__account"><dt><BookUser size={17} aria-hidden="true" />Cuenta corriente neta</dt><dd>{money.format(account)}</dd></dl>
        <Link className="daily-payments__history" to="/ventas?period=today">Ver ventas de hoy<ArrowUpRight size={17} aria-hidden="true" /></Link>
      </footer>
    </section>
  )
}
