// ============================================================
// PÁGINA: INTENTOS INVÁLIDOS
// ============================================================
//
// Muestra todos los intentos de pago inválidos registrados.
// Usa: GET /admin/invalid-attempts
//
// Un intento inválido ocurre cuando:
// - Se intenta reusar un token ya utilizado
// - El token no existe
// - Los tokens vienen de clientes distintos
// - El paquete de tokens está incompleto
//
// Esta sección sirve para detectar fraude o errores.
// ============================================================

import { useState, useEffect } from 'react'
import { fetchInvalidAttempts } from '../api/offpay.js'

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString('es-CO', {
    year: 'numeric', month: 'short', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

function shortId(id) {
  if (!id) return '—'
  return id.split('-')[0] + '…'
}

// Colores por tipo de razón del rechazo
function reasonBadge(reason) {
  if (!reason) return 'badge-neutral'
  if (reason.includes('REUSE') || reason.includes('USED'))    return 'badge-error'
  if (reason.includes('MIXED'))  return 'badge-warn'
  if (reason.includes('INCOMPLETE')) return 'badge-info'
  return 'badge-neutral'
}

export default function InvalidAttemptsPage() {
  const [attempts, setAttempts] = useState(null)
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchInvalidAttempts()
      setAttempts(data.invalid_attempts || [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  // Agrupar por razón para el resumen
  const byReason = attempts
    ? attempts.reduce((acc, a) => {
        const r = a.reason || 'UNKNOWN'
        acc[r] = (acc[r] || 0) + 1
        return acc
      }, {})
    : {}

  return (
    <div>
      <div className="page-header">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div>
            <div className="page-title">⚠ Intentos inválidos</div>
            <div className="page-subtitle">
              Registro de rechazos y posibles intentos de fraude
            </div>
          </div>
          <button className="btn btn-ghost" onClick={load} disabled={loading}>
            {loading ? <><span className="spinner" /> Cargando</> : '↺ Actualizar'}
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {/* Resumen */}
      {attempts && (
        <div className="cards-grid mb-24">
          <div className="card">
            <div className="card-title">Total intentos inválidos</div>
            <div className="stat-value text-error">{attempts.length}</div>
            <div className="stat-label">Registrados en el sistema</div>
          </div>

          {/* Desglose por razón */}
          <div className="card" style={{ gridColumn: 'span 2' }}>
            <div className="card-title">Por razón de rechazo</div>
            {Object.keys(byReason).length === 0 ? (
              <span className="text-muted text-sm">Sin datos</span>
            ) : (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {Object.entries(byReason).map(([reason, count]) => (
                  <div key={reason} className={`badge ${reasonBadge(reason)}`} style={{ fontSize: 12 }}>
                    {reason}: {count}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Alerta si hay intentos */}
      {attempts && attempts.length > 0 && (
        <div className="alert alert-warn mb-24">
          ⚠ Se detectaron {attempts.length} intento(s) inválido(s).
          Revisa los registros para detectar posible reutilización de tokens o fraude.
        </div>
      )}

      {/* Tabla */}
      {loading && (
        <div className="empty-state">
          <div className="spinner" style={{ width: 32, height: 32, margin: '0 auto 12px' }} />
          <div className="empty-text">Cargando intentos inválidos...</div>
        </div>
      )}

      {attempts !== null && !loading && (
        attempts.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon" style={{ color: 'var(--ok)' }}>✓</div>
            <div className="empty-text" style={{ color: 'var(--ok)' }}>
              No hay intentos inválidos registrados. Sistema limpio.
            </div>
          </div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Razón</th>
                  <th>Vendedor ID</th>
                  <th>Token Hash</th>
                  <th>IP / Info</th>
                  <th>Fecha</th>
                </tr>
              </thead>
              <tbody>
                {attempts.map((attempt, i) => (
                  <tr key={attempt.id || i}>
                    <td>
                      <span className="mono text-sm" title={attempt.id}>{shortId(attempt.id)}</span>
                    </td>
                    <td>
                      <span className={`badge ${reasonBadge(attempt.reason)}`}>
                        <span className="badge-dot" />
                        {attempt.reason || 'UNKNOWN'}
                      </span>
                    </td>
                    <td>
                      <span className="mono text-sm" title={attempt.seller_id}>
                        {shortId(attempt.seller_id)}
                      </span>
                    </td>
                    <td>
                      <span className="mono text-sm td-truncate" title={attempt.token_hash}>
                        {attempt.token_hash
                          ? attempt.token_hash.substring(0, 16) + '…'
                          : '—'}
                      </span>
                    </td>
                    <td>
                      <span className="mono text-sm">{attempt.ip_address || attempt.extra_info || '—'}</span>
                    </td>
                    <td>{formatDate(attempt.created_at)}</td>
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
