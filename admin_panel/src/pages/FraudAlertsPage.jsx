// ============================================================
// PÁGINA: ALERTAS DE FRAUDE (nueva)
// ============================================================
//
// Usa GET /admin/fraud-alerts
// Agrupa intentos inválidos por token + vendedor + razón.
// Muestra nivel de riesgo: HIGH / MEDIUM / LOW
// ============================================================

import { useState, useEffect } from 'react'
import { fetchFraudAlerts } from '../api/offpay.js'

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString('es-CO', {
    year: 'numeric', month: 'short', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

function riskBadge(level) {
  if (level === 'HIGH')   return 'badge-error'
  if (level === 'MEDIUM') return 'badge-warn'
  return 'badge-info'
}

function reasonBadge(reason) {
  if (!reason) return 'badge-neutral'
  if (reason.includes('USED') || reason.includes('RETURNED')) return 'badge-error'
  if (reason.includes('MIXED'))      return 'badge-warn'
  if (reason.includes('INCOMPLETE')) return 'badge-info'
  return 'badge-neutral'
}

export default function FraudAlertsPage() {
  const [data, setData]       = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState(null)
  const [page, setPage]       = useState(1)

  const [filters, setFilters] = useState({
    seller_id: '', only_unreviewed: false, min_attempts: 1,
  })

  async function load(f = filters, p = page) {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchFraudAlerts({ ...f, page: p, page_size: 20 })
      setData(result)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  function handleFilterChange(e) {
    const { name, value, type, checked } = e.target
    setFilters(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  function handleSearch() { setPage(1); load(filters, 1) }
  function handleClear() {
    const empty = { seller_id: '', only_unreviewed: false, min_attempts: 1 }
    setFilters(empty); setPage(1); load(empty, 1)
  }
  function handlePage(p) { setPage(p); load(filters, p) }

  const alerts     = data?.alerts || []
  const pagination = data?.pagination
  const highRisk   = alerts.filter(a => a.risk_level === 'HIGH').length

  return (
    <div>
      <div className="page-header">
        <div className="row" style={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <div className="page-title">🚨 Alertas de fraude</div>
            <div className="page-subtitle">
              Agrupación de intentos inválidos repetidos por token y vendedor
            </div>
          </div>
          <button className="btn btn-ghost" onClick={() => load()} disabled={loading}>
            {loading ? <><span className="spinner" /> Cargando</> : '↺ Actualizar'}
          </button>
        </div>
      </div>

      {/* Explicación */}
      <div className="card mb-24" style={{ borderColor: 'var(--warn)', borderLeftWidth: 3 }}>
        <div className="card-title">¿Qué muestra esta sección?</div>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.7 }}>
          Cada fila representa un token que fue rechazado múltiples veces por el mismo vendedor.
          Si un token aparece 3 o más veces → riesgo <strong style={{ color: 'var(--error)' }}>ALTO</strong>.
          Puede indicar que el vendedor está intentando reusar un token ya utilizado o que hay
          un problema en la app del vendedor.
        </p>
      </div>

      {/* Filtros */}
      <div className="card mb-24">
        <div className="card-title">Filtros</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12, alignItems: 'end' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Vendedor ID</label>
            <input className="form-input" name="seller_id" value={filters.seller_id}
              onChange={handleFilterChange} placeholder="UUID…" />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Mínimo de intentos</label>
            <input className="form-input" type="number" name="min_attempts"
              value={filters.min_attempts} min={1} onChange={handleFilterChange} />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, paddingBottom: 2 }}>
            <input type="checkbox" id="only_unreviewed" name="only_unreviewed"
              checked={filters.only_unreviewed} onChange={handleFilterChange}
              style={{ accentColor: 'var(--accent)', width: 16, height: 16 }} />
            <label htmlFor="only_unreviewed" style={{ fontSize: 13, color: 'var(--text-secondary)', cursor: 'pointer' }}>
              Solo con pendientes sin revisar
            </label>
          </div>
        </div>
        <div className="row mt-16" style={{ gap: 8 }}>
          <button className="btn btn-primary" onClick={handleSearch}>→ Buscar</button>
          <button className="btn btn-ghost" onClick={handleClear}>✕ Limpiar</button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {/* Resumen */}
      {data && (
        <div className="cards-grid mb-24">
          <div className="card">
            <div className="card-title">Alertas totales</div>
            <div className="stat-value">{pagination?.total ?? 0}</div>
            <div className="stat-label">Grupos detectados</div>
          </div>
          <div className="card">
            <div className="card-title">Riesgo ALTO</div>
            <div className="stat-value text-error">{highRisk}</div>
            <div className="stat-label">3+ intentos en esta página</div>
          </div>
        </div>
      )}

      {highRisk > 0 && (
        <div className="alert alert-error mb-24">
          🚨 {highRisk} alerta(s) de riesgo ALTO detectadas. Revisa los intentos inválidos relacionados.
        </div>
      )}

      {loading && (
        <div className="empty-state">
          <div className="spinner" style={{ width: 32, height: 32, margin: '0 auto 12px' }} />
          <div className="empty-text">Analizando patrones de fraude...</div>
        </div>
      )}

      {!loading && alerts.length === 0 && (
        <div className="empty-state">
          <div className="empty-icon" style={{ color: 'var(--ok)' }}>✓</div>
          <div className="empty-text" style={{ color: 'var(--ok)' }}>
            No se detectaron patrones sospechosos con estos filtros.
          </div>
        </div>
      )}

      {!loading && alerts.length > 0 && (
        <>
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Riesgo</th>
                  <th>Token Hash</th>
                  <th>Vendedor</th>
                  <th>Razón</th>
                  <th>Intentos</th>
                  <th>Sin revisar</th>
                  <th>Primer intento</th>
                  <th>Último intento</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((alert, i) => (
                  <tr key={i}>
                    <td>
                      <span className={`badge ${riskBadge(alert.risk_level)}`}>
                        <span className="badge-dot" />{alert.risk_level}
                      </span>
                    </td>
                    <td>
                      <span className="mono text-sm" title={alert.token_hash}>
                        {alert.token_hash ? alert.token_hash.substring(0, 16) + '…' : '—'}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontSize: 12, color: 'var(--text-primary)' }}>{alert.seller_name || '—'}</div>
                      <div className="mono text-sm text-muted">{alert.seller_commerce_name || ''}</div>
                    </td>
                    <td>
                      <span className={`badge ${reasonBadge(alert.reason)}`} style={{ fontSize: 10 }}>
                        {alert.reason}
                      </span>
                    </td>
                    <td>
                      <span className="stat-value" style={{ fontSize: 22 }}>{alert.attempt_count}</span>
                    </td>
                    <td>
                      {alert.unreviewed_count > 0
                        ? <span className="badge badge-warn">{alert.unreviewed_count} pendientes</span>
                        : <span className="badge badge-ok">Todos revisados</span>}
                    </td>
                    <td className="mono text-sm">{formatDate(alert.first_attempt_at)}</td>
                    <td className="mono text-sm">{formatDate(alert.last_attempt_at)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {pagination && pagination.total_pages > 1 && (
            <div className="row mt-16" style={{ justifyContent: 'center', gap: 8 }}>
              <button className="btn btn-ghost" onClick={() => handlePage(page - 1)} disabled={page <= 1}>← Anterior</button>
              <span style={{ color: 'var(--text-secondary)', fontSize: 13, padding: '8px 12px' }}>
                Página {page} de {pagination.total_pages}
              </span>
              <button className="btn btn-ghost" onClick={() => handlePage(page + 1)} disabled={page >= pagination.total_pages}>Siguiente →</button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
