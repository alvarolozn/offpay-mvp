// ============================================================
// PÁGINA: TOKENS DEL CLIENTE
// ============================================================
//
// Lista todos los tokens generados por un cliente específico.
// Usa: GET /tokens/client/{client_id}
//
// Cada token representa $10.000 COP bloqueados.
// Puede estar en uno de estos estados:
// - AVAILABLE: listo para usar en un pago
// - USED: ya fue usado en un pago
// - RETURNED: fue devuelto sin usarse
// ============================================================

import { useState, useEffect } from 'react'
import { fetchClientTokens } from '../api/offpay.js'
import { getConfig } from '../config.js'

// Formatea fechas de manera legible
function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString('es-CO', {
    year: 'numeric', month: 'short', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

// Devuelve la clase de badge según el status del token
function tokenBadge(status) {
  switch (status) {
    case 'AVAILABLE': return 'badge-ok'
    case 'USED':      return 'badge-warn'
    case 'RETURNED':  return 'badge-info'
    default:          return 'badge-neutral'
  }
}

// Devuelve la clase según el blockchain_status
function blockchainBadge(status) {
  switch (status) {
    case 'CONFIRMED': return 'badge-ok'
    case 'PENDING':   return 'badge-warn'
    case 'FAILED':    return 'badge-error'
    default:          return 'badge-neutral'
  }
}

export default function TokensPage() {
  const [clientId, setClientId] = useState('')
  const [tokens, setTokens]     = useState(null)
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState(null)

  // Pre-rellena con el clientIdDemo si está configurado
  useEffect(() => {
    const { clientIdDemo } = getConfig()
    if (clientIdDemo) setClientId(clientIdDemo)
  }, [])

  async function handleConsult() {
    if (!clientId.trim()) return
    setLoading(true)
    setError(null)
    setTokens(null)
    try {
      const data = await fetchClientTokens(clientId)
      setTokens(data.tokens || [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter') handleConsult()
  }

  // Conteo por estado
  const counts = tokens ? {
    AVAILABLE: tokens.filter(t => t.status === 'AVAILABLE').length,
    USED:      tokens.filter(t => t.status === 'USED').length,
    RETURNED:  tokens.filter(t => t.status === 'RETURNED').length,
  } : null

  return (
    <div>
      <div className="page-header">
        <div className="page-title">◆ Tokens del cliente</div>
        <div className="page-subtitle">
          Lista de tokens generados por un cliente específico
        </div>
      </div>

      {/* Búsqueda */}
      <div className="card mb-24">
        <div className="card-title">GET /tokens/client/{'{client_id}'}</div>
        <div className="form-group">
          <label className="form-label">Client ID (UUID)</label>
          <div className="row">
            <input
              className="form-input"
              style={{ flex: 1 }}
              value={clientId}
              onChange={e => setClientId(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            />
            <button
              className="btn btn-primary"
              onClick={handleConsult}
              disabled={loading || !clientId.trim()}
            >
              {loading ? <><span className="spinner" /> Consultando</> : '→ Consultar'}
            </button>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {/* Resumen de estados */}
      {counts && (
        <div className="cards-grid mb-24">
          <div className="card">
            <div className="card-title">Disponibles</div>
            <div className="stat-value text-ok">{counts.AVAILABLE}</div>
            <div className="stat-label">Listos para pago</div>
          </div>
          <div className="card">
            <div className="card-title">Usados</div>
            <div className="stat-value text-warn">{counts.USED}</div>
            <div className="stat-label">Ya procesados</div>
          </div>
          <div className="card">
            <div className="card-title">Devueltos</div>
            <div className="stat-value" style={{ color: 'var(--info)' }}>{counts.RETURNED}</div>
            <div className="stat-label">Sin usar, reintegrados</div>
          </div>
          <div className="card">
            <div className="card-title">Total</div>
            <div className="stat-value">{tokens.length}</div>
            <div className="stat-label">Tokens históricos</div>
          </div>
        </div>
      )}

      {/* Tabla de tokens */}
      {tokens !== null && (
        tokens.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">◆</div>
            <div className="empty-text">Este cliente no tiene tokens registrados</div>
          </div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Payment Code</th>
                  <th>Status</th>
                  <th>Blockchain</th>
                  <th>Creado</th>
                  <th>Usado</th>
                  <th>Devuelto</th>
                </tr>
              </thead>
              <tbody>
                {tokens.map((token, i) => (
                  <tr key={token.id || i}>
                    <td className="text-muted">{i + 1}</td>
                    <td>
                      <span className="mono td-truncate" style={{ maxWidth: 140 }} title={token.payment_code}>
                        {token.payment_code || '—'}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${tokenBadge(token.status)}`}>
                        <span className="badge-dot" />
                        {token.status}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${blockchainBadge(token.blockchain_status)}`}>
                        {token.blockchain_status || '—'}
                      </span>
                    </td>
                    <td>{formatDate(token.created_at)}</td>
                    <td>{formatDate(token.used_at)}</td>
                    <td>{formatDate(token.returned_at)}</td>
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
