// ============================================================
// OFFPAY ADMIN PANEL — CAPA DE API
// ============================================================
//
// Aquí están TODAS las funciones que hablan con el backend.
// Si quieres cambiar cómo se llama un endpoint, lo cambias aquí.
// El resto de la app solo llama estas funciones.
//
// ¿Qué es fetch()?
// Es la función nativa del navegador para hacer peticiones HTTP.
// Es como hacer clic en un enlace, pero desde código.
// ============================================================

import { getBaseUrl } from '../config.js'

// ============================================================
// FUNCIÓN BASE — envuelve todos los fetch con manejo de errores
// ============================================================

async function apiFetch(path) {
  const url = `${getBaseUrl()}${path}`

  const response = await fetch(url, {
    headers: {
      'ngrok-skip-browser-warning': 'true',
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    let detail = `Error ${response.status}`
    try {
      const body = await response.json()
      detail = body.detail || detail
    } catch {
      // no era JSON
    }
    throw new Error(detail)
  }

  return response.json()
}

// ============================================================
// GET /health
// Verifica que el backend esté vivo.
// Retorna: { status, app, version }
// ============================================================

export async function fetchHealth() {
  return apiFetch('/health')
}

// ============================================================
// GET /db-test
// Verifica que la base de datos esté conectada.
// Retorna: { status, message, server_time }
// ============================================================

export async function fetchDbTest() {
  return apiFetch('/db-test')
}

// ============================================================
// GET /wallets/{user_id}
// Obtiene la wallet de un usuario específico.
// Retorna: { wallet_id, user_id, full_name, role,
//            commerce_name, available_balance_cop, blocked_balance_cop }
// ============================================================

export async function fetchWallet(userId) {
  if (!userId || !userId.trim()) {
    throw new Error('Debes ingresar un user_id')
  }
  return apiFetch(`/wallets/${userId.trim()}`)
}

// ============================================================
// GET /tokens/client/{client_id}
// Lista todos los tokens de un cliente.
// Retorna: { tokens: [...] }
// ============================================================

export async function fetchClientTokens(clientId) {
  if (!clientId || !clientId.trim()) {
    throw new Error('Debes ingresar un client_id')
  }
  return apiFetch(`/tokens/client/${clientId.trim()}`)
}

// ============================================================
// GET /transactions
// Lista todas las transacciones del sistema.
// Retorna: { transactions: [...] }
// ============================================================

export async function fetchTransactions() {
  return apiFetch('/transactions')
}

// ============================================================
// GET /admin/invalid-attempts
// Lista todos los intentos inválidos registrados.
// Retorna: { invalid_attempts: [...] }
// ============================================================

export async function fetchInvalidAttempts() {
  return apiFetch('/admin/invalid-attempts')
}
