// ============================================================
// PÁGINA: TRANSACCIONES
// ============================================================
//
// Muestra todas las transacciones del sistema.
// Usa: GET /transactions
//
// Una transacción se crea cuando un vendedor valida
// un paquete de tokens y el backend lo aprueba.
// Cada token usado genera una transacción separada.
// ============================================================

import { useState, useEffect } from 'react'
import { fetchTransactions } from '../api/offpay.js'

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString('es-CO', {
    year: 'numeric', month: 'short', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

function formatCOP(amount) {
  if (amount === undefined || amount === null) return '—'
  return new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', minimumFractionDigits: 0,
  }).format(amount)
}

function statusBadge(status) {
  switch (status) {
    case 'APPROVED': return 'badge-ok'
    case 'REJECTED': return 'badge-error'
    default:         return 'badge-neutral'
  }
}

// Trunca un UUID para mostrar solo la parte inicial
function shortId(id) {
  if (!id) return '—'
  return id.split('-')[0] + '…'
}

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState(null)
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchTransactions()
      setTransactions(data.transactions || [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  // Carga automáticamente al entrar a la página
  useEffect(() => { load() }, [])

  // Totales
  const totalAmount = transactions
    ? transactions.reduce((sum, t) => sum + (t.amount_cop || 0), 0)
    : 0

  const approved = transactions
    ? transactions.filter(t => t.status === 'APPROVED').length
    : 0

  return (
    <div>
      <div className="page-header">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div>
            <div className="page-title">≡ Transacciones</div>
            <div className="page-subtitle">
              Historial de pagos procesados en el sistema
            </div>
          </div>
          <button className="btn btn-ghost" onClick={load} disabled={loading}>
            {loading ? <><span className="spinner" /> Cargando</> : '↺ Actualizar'}
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {/* Resumen */}
      {transactions && (
        <div className="cards-grid mb-24">
          <div className="card">
            <div className="card-title">Total transacciones</div>
            <div className="stat-value">{transactions.length}</div>
            <div className="stat-label">Registradas en el sistema</div>
          </div>
          <div className="card">
            <div className="card-title">Aprobadas</div>
            <div className="stat-value text-ok">{approved}</div>
            <div className="stat-label">Pagos exitosos</div>
          </div>
          <div className="card">
            <div className="card-title">Volumen total</div>
            <div className="stat-value">{formatCOP(totalAmount)}</div>
            <div className="stat-label">COP procesados</div>
          </div>
        </div>
      )}

      {/* Tabla */}
      {loading && (
        <div className="empty-state">
          <div className="spinner" style={{ width: 32, height: 32, margin: '0 auto 12px' }} />
          <div className="empty-text">Cargando transacciones...</div>
        </div>
      )}

      {transactions !== null && !loading && (
        transactions.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">≡</div>
            <div className="empty-text">No hay transacciones registradas</div>
          </div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Status</th>
                  <th>Monto</th>
                  <th>Cliente ID</th>
                  <th>Vendedor ID</th>
                  <th>Token ID</th>
                  <th>Resp. (ms)</th>
                  <th>Fecha</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx, i) => (
                  <tr key={tx.id || i}>
                    <td>
                      <span className="mono text-sm" title={tx.id}>{shortId(tx.id)}</span>
                    </td>
                    <td>
                      <span className={`badge ${statusBadge(tx.status)}`}>
                        <span className="badge-dot" />
                        {tx.status || '—'}
                      </span>
                    </td>
                    <td className="td-highlight">{formatCOP(tx.amount_cop)}</td>
                    <td>
                      <span className="mono text-sm" title={tx.client_id}>{shortId(tx.client_id)}</span>
                    </td>
                    <td>
                      <span className="mono text-sm" title={tx.seller_id}>{shortId(tx.seller_id)}</span>
                    </td>
                    <td>
                      <span className="mono text-sm" title={tx.token_id}>{shortId(tx.token_id)}</span>
                    </td>
                    <td>{tx.response_time_ms ?? '—'}</td>
                    <td>{formatDate(tx.created_at)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
    </div>
  )
}
