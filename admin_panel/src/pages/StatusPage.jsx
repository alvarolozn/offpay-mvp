// ============================================================
// PÁGINA: ESTADO DEL SISTEMA
// ============================================================
//
// Llama a /health y /db-test para verificar que
// el backend y la base de datos estén respondiendo.
//
// ¿Qué es useEffect?
// Es una función de React que se ejecuta automáticamente
// cuando la página carga (o cuando cambia algo específico).
// Lo usamos para hacer las peticiones al backend
// apenas el usuario entra a esta pantalla.
// ============================================================

import { useState, useEffect } from 'react'
import { fetchHealth, fetchDbTest } from '../api/offpay.js'
import { getConfig } from '../config.js'

// Componente de tarjeta de estado individual
function StatusCard({ title, loading, data, error, fields }) {
  return (
    <div className="card">
      <div className="card-title">{title}</div>

      {loading && (
        <div className="row">
          <div className="spinner" />
          <span className="text-muted">Consultando...</span>
        </div>
      )}

      {error && (
        <div>
          <span className="badge badge-error">
            <span className="badge-dot" />
            Sin respuesta
          </span>
          <div className="alert alert-error mt-8" style={{ marginBottom: 0 }}>
            {error}
          </div>
        </div>
      )}

      {data && !loading && (
        <div>
          <span className="badge badge-ok mb-16">
            <span className="badge-dot" />
            Respondiendo
          </span>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 12 }}>
            {fields.map(({ key, label }) => (
              <div key={key} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border)', paddingBottom: 8 }}>
                <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>{label}</span>
                <span className="mono" style={{ fontSize: 12, color: 'var(--text-primary)' }}>
                  {String(data[key] ?? '—')}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default function StatusPage() {
  const [health, setHealth]     = useState(null)
  const [healthErr, setHealthErr] = useState(null)
  const [healthLoading, setHealthLoading] = useState(true)

  const [db, setDb]             = useState(null)
  const [dbErr, setDbErr]       = useState(null)
  const [dbLoading, setDbLoading] = useState(true)

  const config = getConfig()

  // Función que hace ambas peticiones
  async function checkAll() {
    setHealthLoading(true)
    setDbLoading(true)
    setHealthErr(null)
    setDbErr(null)
    setHealth(null)
    setDb(null)

    // /health
    try {
      const h = await fetchHealth()
      setHealth(h)
    } catch (e) {
      setHealthErr(e.message)
    } finally {
      setHealthLoading(false)
    }

    // /db-test
    try {
      const d = await fetchDbTest()
      setDb(d)
    } catch (e) {
      setDbErr(e.message)
    } finally {
      setDbLoading(false)
    }
  }

  // Se ejecuta automáticamente cuando carga la página
  useEffect(() => { checkAll() }, [])

  // Estado general: ok solo si ambos respondieron bien
  const allOk = health && db && !healthErr && !dbErr
  const someError = healthErr || dbErr

  return (
    <div>
      <div className="page-header">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div>
            <div className="page-title">◈ Estado del sistema</div>
            <div className="page-subtitle">
              Verificación en tiempo real de backend y base de datos
            </div>
          </div>
          <button className="btn btn-ghost" onClick={checkAll}>
            ↺ Actualizar
          </button>
        </div>
      </div>

      {/* Estado general */}
      <div className="card mb-24">
        <div className="card-title">Estado general</div>
        <div className="row">
          {(healthLoading || dbLoading) && (
            <>
              <div className="spinner" />
              <span className="text-muted">Verificando sistema...</span>
            </>
          )}
          {!healthLoading && !dbLoading && allOk && (
            <span className="badge badge-ok" style={{ fontSize: 14, padding: '6px 14px' }}>
              <span className="badge-dot" />
              Sistema operativo
            </span>
          )}
          {!healthLoading && !dbLoading && someError && (
            <span className="badge badge-error" style={{ fontSize: 14, padding: '6px 14px' }}>
              <span className="badge-dot" />
              Hay problemas en el sistema
            </span>
          )}
        </div>
        <div className="form-hint mt-8">
          Backend: <span className="mono">{config.backendUrl}</span>
        </div>
      </div>

      {/* Tarjetas individuales */}
      <div className="cards-grid">
        <StatusCard
          title="GET /health — Backend API"
          loading={healthLoading}
          data={health}
          error={healthErr}
          fields={[
            { key: 'status',  label: 'Status' },
            { key: 'app',     label: 'Aplicación' },
            { key: 'version', label: 'Versión' },
          ]}
        />

        <StatusCard
          title="GET /db-test — Base de datos"
          loading={dbLoading}
          data={db}
          error={dbErr}
          fields={[
            { key: 'status',      label: 'Status' },
            { key: 'message',     label: 'Mensaje' },
            { key: 'server_time', label: 'Hora del servidor' },
          ]}
        />
      </div>

      {/* Ayuda si hay error */}
      {(healthErr || dbErr) && (
        <div className="card">
          <div className="card-title">¿Cómo resolver esto?</div>
          <ol style={{ color: 'var(--text-secondary)', fontSize: 13, paddingLeft: 20, lineHeight: 2 }}>
            <li>Verifica que el backend de FastAPI esté corriendo.</li>
            <li>Confirma que la URL en <strong>Configuración</strong> sea correcta.</li>
            <li>Si usas <span className="mono">localhost</span>, asegúrate de estar en la misma máquina.</li>
            <li>Revisa que no haya un firewall o CORS bloqueando la conexión.</li>
          </ol>
        </div>
      )}
    </div>
  )
}
