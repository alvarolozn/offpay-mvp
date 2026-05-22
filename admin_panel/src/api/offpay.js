// ============================================================
// OFFPAY ADMIN PANEL — CAPA DE API (actualizada)
// ============================================================
//
// Todas las funciones que hablan con el backend.
// Incluye los nuevos endpoints admin del backend actualizado.
// ============================================================

import { getBaseUrl } from '../config.js'

// ============================================================
// FUNCIÓN BASE — fetch con header ngrok + manejo de errores
// ============================================================

async function apiFetch(path, options = {}) {
  const url = `${getBaseUrl()}${path}`

  const response = await fetch(url, {
    ...options,
    headers: {
      'ngrok-skip-browser-warning': 'true',
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  })

  if (!response.ok) {
    let detail = `Error ${response.status}`
    try {
      const body = await response.json()
      detail = body.detail || detail
    } catch { /* no era JSON */ }
    throw new Error(detail)
  }

  return response.json()
}

// ============================================================
// GET /health
// ============================================================
export async function fetchHealth() {
  return apiFetch('/health')
}

// ============================================================
// GET /db-test
// ============================================================
export async function fetchDbTest() {
  return apiFetch('/db-test')
}

// ============================================================
// GET /wallets/{user_id}
// ============================================================
export async function fetchWallet(userId) {
  if (!userId?.trim()) throw new Error('Debes ingresar un user_id')
  return apiFetch(`/wallets/${userId.trim()}`)
}

// ============================================================
// GET /tokens/client/{client_id}?sort=created_desc
// sort: counter_asc | created_desc
// ============================================================
export async function fetchClientTokens(clientId, sort = 'created_desc') {
  if (!clientId?.trim()) throw new Error('Debes ingresar un client_id')
  return apiFetch(`/tokens/client/${clientId.trim()}?sort=${sort}`)
}

// ============================================================
// GET /admin/transactions  (con filtros y paginación)
// params: { client_id, seller_id, status, date_from, date_to, page, page_size }
// ============================================================
export async function fetchAdminTransactions(params = {}) {
  const qs = new URLSearchParams()
  if (params.client_id)  qs.set('client_id',  params.client_id)
  if (params.seller_id)  qs.set('seller_id',  params.seller_id)
  if (params.status)     qs.set('status',     params.status)
  if (params.date_from)  qs.set('date_from',  params.date_from)
  if (params.date_to)    qs.set('date_to',    params.date_to)
  qs.set('page',      params.page      || 1)
  qs.set('page_size', params.page_size || 20)
  return apiFetch(`/admin/transactions?${qs.toString()}`)
}

// ============================================================
// GET /admin/transactions/export  → devuelve CSV como texto
// ============================================================
export async function exportTransactionsCSV(params = {}) {
  const qs = new URLSearchParams()
  if (params.client_id) qs.set('client_id', params.client_id)
  if (params.seller_id) qs.set('seller_id', params.seller_id)
  if (params.status)    qs.set('status',    params.status)
  if (params.date_from) qs.set('date_from', params.date_from)
  if (params.date_to)   qs.set('date_to',   params.date_to)

  const url = `${getBaseUrl()}/admin/transactions/export?${qs.toString()}`
  const response = await fetch(url, {
    headers: { 'ngrok-skip-browser-warning': 'true' }
  })
  if (!response.ok) throw new Error(`Error ${response.status} al exportar`)
  return response.blob()
}

// ============================================================
// GET /admin/invalid-attempts  (con filtros y paginación)
// params: { seller_id, reason, only_unreviewed, page, page_size }
// ============================================================
export async function fetchInvalidAttempts(params = {}) {
  const qs = new URLSearchParams()
  if (params.seller_id)       qs.set('seller_id',       params.seller_id)
  if (params.reason)          qs.set('reason',           params.reason)
  if (params.only_unreviewed) qs.set('only_unreviewed',  'true')
  qs.set('page',      params.page      || 1)
  qs.set('page_size', params.page_size || 20)
  return apiFetch(`/admin/invalid-attempts?${qs.toString()}`)
}

// ============================================================
// GET /admin/fraud-alerts  (con filtros y paginación)
// params: { seller_id, only_unreviewed, min_attempts, page, page_size }
// ============================================================
export async function fetchFraudAlerts(params = {}) {
  const qs = new URLSearchParams()
  if (params.seller_id)       qs.set('seller_id',       params.seller_id)
  if (params.only_unreviewed) qs.set('only_unreviewed',  'true')
  qs.set('min_attempts', params.min_attempts || 1)
  qs.set('page',         params.page         || 1)
  qs.set('page_size',    params.page_size    || 20)
  return apiFetch(`/admin/fraud-alerts?${qs.toString()}`)
}

// ============================================================
// POST /admin/invalid-attempts/{attempt_id}/review
// ============================================================
export async function reviewInvalidAttempt(attemptId, reviewNote = '') {
  return apiFetch(`/admin/invalid-attempts/${attemptId}/review`, {
    method: 'POST',
    body: JSON.stringify({ review_note: reviewNote }),
  })
}

// ============================================================
// GET /transactions  (endpoint original, sin filtros)
// Se mantiene por compatibilidad
// ============================================================
export async function fetchTransactions() {
  return apiFetch('/transactions')
}
