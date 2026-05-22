// ============================================================
// PÁGINA: TRANSACCIONES (actualizada con /admin/transactions)
// ============================================================
//
// Usa el nuevo endpoint GET /admin/transactions que incluye:
// - Filtros por cliente, vendedor, estado y fechas
// - Paginación
// - Datos enriquecidos: nombre cliente, nombre vendedor
// - Transacciones REJECTED además de APPROVED
// - Exportación a CSV
// ============================================================

import { useState, useEffect } from 'react'
import { fetchAdminTransactions, exportTransactionsCSV } from '../api/offpay.js'

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString('es-CO', {
    year: 'numeric', month: 'short', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

function formatCOP(amount) {
  if (amount == null) return '—'
  return new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', minimumFractionDigits: 0,
  }).format(amount)
}

function shortId(id) {
  if (!id) return '—'
  return id.split('-')[0] + '…'
}

function statusBadge(status) {
  if (status === 'APPROVED') return 'badge-ok'
  if (status === 'REJECTED') return 'badge-error'
  return 'badge-neutral'
}

export default function TransactionsPage() {
  const [data, setData]         = useState(null)
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState(null)
  const [exporting, setExporting] = useState(false)

  // Filtros
  const [filters, setFilters] = useState({
    client_id: '', seller_id: '', status: '',
    date_from: '', date_to: '',
  })
  const [page, setPage] = useState(1)

  async function load(f = filters, p = page) {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchAdminTransactions({ ...f, page: p, page_size: 20 })
      setData(result)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  function handleFilterChange(e) {
    const { name, value } = e.target
    setFilters(prev => ({ ...prev, [name]: value }))
  }

  function handleSearch() {
    setPage(1)
    load(filters, 1)
  }

  function handleClear() {
    const empty = { client_id: '', seller_id: '', status: '', date_from: '', date_to: '' }
    setFilters(empty)
    setPage(1)
    load(empty, 1)
  }

  function handlePage(p) {
    setPage(p)
    load(filters, p)
  }

  async function handleExport() {
    setExporting(true)
    try {
      const blob = await exportTransactionsCSV(filters)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `offpay_transacciones_${new Date().toISOString().slice(0,10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      alert('Error al exportar: ' + e.message)
    } finally {
      setExporting(false)
    }
  }

  const pagination = data?.pagination
  const transactions = data?.transactions || []

  // Totales rápidos
  const approved = transactions.filter(t => t.status === 'APPROVED').length
  const rejected = transactions.filter(t => t.status === 'REJECTED').length
  const totalCOP = transactions
    .filter(t => t.status === 'APPROVED')
    .reduce((s, t) => s + (t.amount_cop || 0), 0)

  return (
    <div>
      <div className="page-header">
        <div className="row" style={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <div className="page-title">≡ Transacciones</div>
            <div className="page-subtitle">Historial completo — aprobadas y rechazadas</div>
          </div>
          <div className="row" style={{ gap: 8 }}>
            <button className="btn btn-ghost" onClick={() => load()} disabled={loading}>
              {loading ? <><span className="spinner" /> Cargando</> : '↺ Actualizar'}
            </button>
            <button className="btn btn-ghost" onClick={handleExport} disabled={exporting}>
              {exporting ? <><span className="spinner" /> Exportando</> : '⬇ CSV'}
            </button>
          </div>
        </div>
      </div>

      {/* Filtros */}
      <div className="card mb-24">
        <div className="card-title">Filtros</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Cliente ID</label>
            <input className="form-input" name="client_id" value={filters.client_id}
              onChange={handleFilterChange} placeholder="UUID…" />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Vendedor ID</label>
            <input className="form-input" name="seller_id" value={filters.seller_id}
              onChange={handleFilterChange} placeholder="UUID…" />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Estado</label>
            <select className="form-input" name="status" value={filters.status} onChange={handleFilterChange}>
              <option value="">Todos</option>
              <option value="APPROVED">APPROVED</option>
              <option value="REJECTED">REJECTED</option>
            </select>
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Desde</label>
            <input className="form-input" type="date" name="date_from" value={filters.date_from}
              onChange={handleFilterChange} />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Hasta</label>
            <input className="form-input" type="date" name="date_to" value={filters.date_to}
              onChange={handleFilterChange} />
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
            <div className="card-title">Total en página</div>
            <div className="stat-value">{pagination?.total ?? 0}</div>
            <div className="stat-label">Registros encontrados</div>
          </div>
          <div className="card">
            <div className="card-title">Aprobadas</div>
            <div className="stat-value text-ok">{approved}</div>
            <div className="stat-label">En esta página</div>
          </div>
          <div className="card">
            <div className="card-title">Rechazadas</div>
            <div className="stat-value text-error">{rejected}</div>
            <div className="stat-label">En esta página</div>
          </div>
          <div className="card">
            <div className="card-title">Volumen aprobado</div>
            <div className="stat-value">{formatCOP(totalCOP)}</div>
            <div className="stat-label">En esta página</div>
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

      {!loading && transactions.length === 0 && (
        <div className="empty-state">
          <div className="empty-icon">≡</div>
          <div className="empty-text">No hay transacciones con estos filtros</div>
        </div>
      )}

      {!loading && transactions.length > 0 && (
        <>
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Estado</th>
                  <th>Monto</th>
                  <th>Cliente</th>
                  <th>Vendedor</th>
                  <th>Motivo rechazo</th>
                  <th>Resp. ms</th>
                  <th>Fecha</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx, i) => (
                  <tr key={tx.id || i}>
                    <td><span className="mono text-sm" title={tx.id}>{shortId(tx.id)}</span></td>
                    <td>
                      <span className={`badge ${statusBadge(tx.status)}`}>
                        <span className="badge-dot" />{tx.status}
                      </span>
                    </td>
                    <td className="td-highlight">{formatCOP(tx.amount_cop)}</td>
                    <td>
                      <div style={{ fontSize: 12, color: 'var(--text-primary)' }}>{tx.client_name || '—'}</div>
                      <div className="mono text-sm text-muted" title={tx.client_id}>{shortId(tx.client_id)}</div>
                    </td>
                    <td>
                      <div style={{ fontSize: 12, color: 'var(--text-primary)' }}>{tx.seller_name || '—'}</div>
                      <div className="mono text-sm text-muted">{tx.seller_commerce_name || ''}</div>
                    </td>
                    <td>
                      {tx.rejection_reason
                        ? <span className="badge badge-error" style={{ fontSize: 10 }}>{tx.rejection_reason}</span>
                        : <span className="text-muted">—</span>}
                    </td>
                    <td className="mono text-sm">{tx.response_time_ms ?? '—'}</td>
                    <td className="mono text-sm">{formatDate(tx.created_at)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Paginación */}
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
