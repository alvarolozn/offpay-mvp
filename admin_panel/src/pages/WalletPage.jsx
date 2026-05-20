// ============================================================
// PÁGINA: CONSULTA DE WALLET
// ============================================================
//
// Permite consultar la wallet de cualquier usuario
// ingresando su user_id (un UUID).
//
// Usa el endpoint: GET /wallets/{user_id}
//
// ¿Qué es un UUID?
// Es un identificador único universal. Se ve así:
// 550e8400-e29b-41d4-a716-446655440000
// Cada usuario de OffPay tiene uno asignado en la base de datos.
// ============================================================

import { useState, useEffect } from 'react'
import { fetchWallet } from '../api/offpay.js'
import { getConfig } from '../config.js'

// Formatea números como pesos colombianos
function formatCOP(amount) {
  if (amount === undefined || amount === null) return '—'
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    minimumFractionDigits: 0,
  }).format(amount)
}

export default function WalletPage() {
  const [userId, setUserId] = useState('')
  const [wallet, setWallet] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // Pre-rellena con el Client ID demo si está configurado
  useEffect(() => {
    const { clientIdDemo } = getConfig()
    if (clientIdDemo) setUserId(clientIdDemo)
  }, [])

  async function handleConsult() {
    if (!userId.trim()) return
    setLoading(true)
    setError(null)
    setWallet(null)
    try {
      const data = await fetchWallet(userId)
      setWallet(data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  // Permite consultar con Enter
  function handleKeyDown(e) {
    if (e.key === 'Enter') handleConsult()
  }

  return (
    <div>
      <div className="page-header">
        <div className="page-title">◎ Consulta de Wallet</div>
        <div className="page-subtitle">
          Ingresa un user_id para ver el estado de su wallet
        </div>
      </div>

      {/* Formulario de búsqueda */}
      <div className="card mb-24">
        <div className="card-title">GET /wallets/{'{user_id}'}</div>
        <div className="form-group">
          <label className="form-label">User ID (UUID)</label>
          <div className="row">
            <input
              className="form-input"
              style={{ flex: 1 }}
              value={userId}
              onChange={e => setUserId(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            />
            <button
              className="btn btn-primary"
              onClick={handleConsult}
              disabled={loading || !userId.trim()}
            >
              {loading ? <><span className="spinner" /> Consultando</> : '→ Consultar'}
            </button>
          </div>
          <div className="form-hint">
            Puedes configurar un ID de demo en la sección Configuración.
          </div>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="alert alert-error">{error}</div>
      )}

      {/* Resultado */}
      {wallet && (
        <div>
          {/* Encabezado del usuario */}
          <div className="card mb-16">
            <div className="row" style={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 4 }}>
                  {wallet.full_name}
                </div>
                <div className="row" style={{ gap: 8 }}>
                  <span className={`badge ${wallet.role === 'CLIENT' ? 'badge-info' : 'badge-warn'}`}>
                    {wallet.role}
                  </span>
                  {wallet.commerce_name && (
                    <span className="text-muted mono text-sm">{wallet.commerce_name}</span>
                  )}
                </div>
                <div className="form-hint mt-8">
                  ID: <span className="mono">{wallet.user_id}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Saldos */}
          <div className="cards-grid">
            <div className="card">
              <div className="card-title">Saldo disponible</div>
              <div className="stat-value text-ok">
                {formatCOP(wallet.available_balance_cop)}
              </div>
              <div className="stat-label">Listo para usar</div>
            </div>

            <div className="card">
              <div className="card-title">Saldo bloqueado</div>
              <div className="stat-value text-warn">
                {formatCOP(wallet.blocked_balance_cop)}
              </div>
              <div className="stat-label">En tokens generados</div>
            </div>

            <div className="card">
              <div className="card-title">Saldo total</div>
              <div className="stat-value">
                {formatCOP(wallet.available_balance_cop + wallet.blocked_balance_cop)}
              </div>
              <div className="stat-label">Disponible + Bloqueado</div>
            </div>
          </div>

          {/* Detalle técnico */}
          <div className="card">
            <div className="card-title">Detalle completo (respuesta del backend)</div>
            <div className="json-block">{JSON.stringify(wallet, null, 2)}</div>
          </div>
        </div>
      )}
    </div>
  )
}
