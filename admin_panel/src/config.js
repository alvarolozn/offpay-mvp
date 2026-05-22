// ============================================================
// OFFPAY ADMIN PANEL — CONFIGURACIÓN CENTRAL
// ============================================================
//
// Este archivo maneja toda la configuración del panel.
// Los valores se guardan en localStorage para que persistan
// aunque cierres y vuelvas a abrir el navegador.
//
// ¿Qué es localStorage?
// Es una pequeña base de datos que vive en tu navegador.
// Los datos NO se van al servidor, solo viven en tu computador.
// ============================================================

const CONFIG_KEY = 'offpay_admin_config'

// Valores por defecto — cámbialos en la pantalla de Configuración
const DEFAULTS = {
  backendUrl: 'https://attempt-uncommon-matron.ngrok-free.dev',
  clientIdDemo: '',
  sellerIdDemo: '',
}

// Lee la configuración guardada. Si no hay nada guardado, usa los defaults.
export function getConfig() {
  try {
    const stored = localStorage.getItem(CONFIG_KEY)
    if (!stored) return { ...DEFAULTS }
    return { ...DEFAULTS, ...JSON.parse(stored) }
  } catch {
    return { ...DEFAULTS }
  }
}

// Guarda la configuración nueva.
export function saveConfig(newConfig) {
  const merged = { ...getConfig(), ...newConfig }
  localStorage.setItem(CONFIG_KEY, JSON.stringify(merged))
  return merged
}

// Obtiene solo la URL del backend.
export function getBaseUrl() {
  return getConfig().backendUrl.replace(/\/$/, '') // quita slash final si existe
}
