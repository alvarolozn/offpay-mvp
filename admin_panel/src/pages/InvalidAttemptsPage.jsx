// ============================================================
// PÁGINA: INTENTOS INVÁLIDOS (actualizada)
// ============================================================
//
// Usa GET /admin/invalid-attempts con filtros y paginación.
// Permite marcar intentos como revisados con nota.
// ============================================================

import { useState, useEffect } from 'react'
import { fetchInvalidAttempts, reviewInvalidAttempt } from '../api/offpay.js'

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

function reasonBadge(reason) {
  if (!reason) return 'badge-neutral'
  if (reason.includes('USED') || reason.includes('RETURNED')) return 'badge-error'
  if (reason.includes('MIXED'))    return 'badge-warn'
  if (reason.includes('INCOMPLETE')) return 'badge-info'
  return 'badge-neutral'
}

// Modal para marcar como revisado
function ReviewModal({ attempt, onClose, onSaved }) {
  const [note, setNote]         = useState('')
  const [saving, setSaving]     = useState(false)
  const [error, setError]       = useState(null)

  async function handleSave() {
    setSaving(true)
    setError(null)
    try {
      await reviewInvalidAttempt(attempt.id, note)
      onSaved()
    } catch (e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 999,
    }}>
      <div className="card" style={{ width: 480, maxWidth: '90vw' }}>
        <div className="card-title">Marcar como revisado</div>
        <div className="form-hint mb-16">
          Intento ID: <span className="mono">{attempt.id}</span><br />
          Razón: <span className="mono">{attempt.reason}</span>
        </div>
        {error && <div className="alert alert-error">{error}</div>}
        <div className="form-group">
          <label className="form-label">Nota de revisión (opcional)</label>
          <textarea
            className="form-input"
            rows={3}
            value={note}
            onChange={e => setNote(e.target.value)}
            placeholder="Ej: Token reutilizado por error de la app. Verificado."
            style={{ resize: 'vertical' }}
          />
        </div>
        <div className="row" style={{ gap: 8 }}>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? <><span className="spinner" /> Guardando</> : '✓ Confirmar revisión'}
          </button>
          <button className="btn btn-ghost" onClick={onClose} disabled={saving}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}

export default function InvalidAttemptsPage() {
  const [data, setData]         = useState(null)
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState(null)
  const [page, setPage]         = useState(1)
  const [reviewTarget, setReviewTarget] = useState(null)

  const [filters, setFilters] = useState({
    seller_id: '', reason: '', only_unreviewed: false,
  })

  async function load(f = filters, p = page) {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchInvalidAttempts({ ...f, page: p, page_size: 20 })
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
    const empty = { seller_id: '', reason: '', only_unreviewed: false }
    setFilters(empty); setPage(1); load(empty, 1)
  }
  function handlePage(p) { setPage(p); load(filters, p) }

  function handleReviewSaved() {
    setReviewTarget(null)
    load(filters, page)
  }

  const attempts   = data?.invalid_attempts || []
  const pagination = data?.pagination
  const unreviewed = attempts.filter(a => !a.is_reviewed).length

  return (
    <div>
      {reviewTarget && (
        <ReviewModal
          attempt={reviewTarget}
          onClose={() => setReviewTarget(null)}
          onSaved={handleReviewSaved}
        />
      )}

      <div className="page-header">
        <div className="row" style={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <div className="page-title">⚠ Intentos inválidos</div>
            <div className="page-subtitle">Registro de rechazos y posibles intentos de fraude</div>
          </div>
          <button className="btn btn-ghost" onClick={() => load()} disabled={loading}>
            {loading ? <><span className="spinner" /> Cargando</> : '↺ Actualizar'}
          </button>
        </div>
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
            <label className="form-label">Razón</label>
            <select className="form-input" name="reason" value={filters.reason} onChange={handleFilterChange}>
              <option value="">Todas</option>
              <option value="TOKEN_PACKAGE_INCOMPLETE">TOKEN_PACKAGE_INCOMPLETE</option>
              <option value="TOKEN_PACKAGE_MIXED_CLIENTS">TOKEN_PACKAGE_MIXED_CLIENTS</option>
              <option value="TOKEN_ALREADY_USED">TOKEN_ALREADY_USED</option>
              <option value="TOKEN_ALREADY_RETURNED">TOKEN_ALREADY_RETURNED</option>
            </select>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, paddingBottom: 2 }}>
            <input type="checkbox" id="only_unreviewed" name="only_unreviewed"
              checked={filters.only_unreviewed} onChange={handleFilterChange}
              style={{ accentColor: 'var(--accent)', width: 16, height: 16 }} />
            <label htmlFor="only_unreviewed" style={{ fontSize: 13, color: 'var(--text-secondary)', cursor: 'pointer' }}>
              Solo sin revisar
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
            <div className="card-title">Total encontrados</div>
            <div className="stat-value text-error">{pagination?.total ?? 0}</div>
            <div className="stat-label">Registrados en el sistema</div>
          </div>
          <div className="card">
            <div className="card-title">Sin revisar (página)</div>
            <div className="stat-value text-warn">{unreviewed}</div>
            <div className="stat-label">Requieren atención</div>
          </div>
        </div>
      )}

      {unreviewed > 0 && (
        <div className="alert alert-warn mb-24">
          ⚠ Hay {unreviewed} intento(s) sin revisar en esta página. Usa el botón "Revisar" para marcarlos.
        </div>
      )}

      {loading && (
        <div className="empty-state">
          <div className="spinner" style={{ width: 32, height: 32, margin: '0 auto 12px' }} />
          <div className="empty-text">Cargando intentos inválidos...</div>
        </div>
      )}

      {!loading && attempts.length === 0 && (
        <div className="empty-state">
          <div className="empty-icon" style={{ color: 'var(--ok)' }}>✓</div>
          <div className="empty-text" style={{ color: 'var(--ok)' }}>
            No hay intentos inválidos con estos filtros.
          </div>
        </div>
      )}

      {!loading && attempts.length > 0 && (
        <>
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Razón</th>
                  <th>Vendedor</th>
                  <th>Token Hash</th>
                  <th>Estado revisión</th>
                  <th>Fecha</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {attempts.map((a, i) => (
                  <tr key={a.id || i}>
                    <td><span className="mono text-sm" title={a.id}>{shortId(a.id)}</span></td>
                    <td>
                      <span className={`badge ${reasonBadge(a.reason)}`}>
                        <span className="badge-dot" />{a.reason || 'UNKNOWN'}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontSize: 12, color: 'var(--text-primary)' }}>{a.seller_name || '—'}</div>
                      <div className="mono text-sm text-muted">{a.seller_commerce_name || ''}</div>
                    </td>
                    <td>
                      <span className="mono text-sm td-truncate" title={a.token_hash} style={{ maxWidth: 120 }}>
                        {a.token_hash ? a.token_hash.substring(0, 14) + '…' : '—'}
                      </span>
                    </td>
                    <td>
                      {a.is_reviewed
                        ? <span className="badge badge-ok"><span className="badge-dot" />Revisado</span>
                        : <span className="badge badge-warn"><span className="badge-dot" />Pendiente</span>}
                      {a.review_note && (
                        <div className="text-muted text-sm mt-8" style={{ maxWidth: 160, wordBreak: 'break-word' }}>
                          {a.review_note}
                        </div>
                      )}
                    </td>
                    <td className="mono text-sm">{formatDate(a.created_at)}</td>
                    <td>
                      {!a.is_reviewed && (
                        <button
                          className="btn btn-ghost"
                          style={{ padding: '5px 10px', fontSize: 11 }}
                          onClick={() => setReviewTarget(a)}
                        >
                          ✓ Revisar
                        </button>
                      )}
                    </td>
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
