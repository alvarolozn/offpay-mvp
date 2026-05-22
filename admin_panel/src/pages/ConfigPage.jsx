// ============================================================
// PÁGINA: CONFIGURACIÓN
// ============================================================
//
// Aquí el administrador puede cambiar:
// - La URL del backend (ej: http://localhost:8000)
// - El Client ID que se usará como demo en el panel
// - El Seller ID que se usará como demo en el panel
//
// Los datos se guardan en localStorage (en tu navegador).
// No se van a ningún servidor.
//
// ¿Qué es useState?
// Es la forma que tiene React de "recordar" datos mientras
// la app está abierta. Si cambias un estado, la pantalla
// se actualiza automáticamente.
// ============================================================

import { useState, useEffect } from 'react'
import { getConfig, saveConfig } from '../config.js'

export default function ConfigPage() {
  // Estado local: lo que hay en los inputs del formulario
  const [form, setForm] = useState({ backendUrl: '', clientIdDemo: '', sellerIdDemo: '' })
  const [saved, setSaved] = useState(false)

  // Al cargar la página, lee la configuración guardada y la pone en los inputs
  useEffect(() => {
    setForm(getConfig())
  }, [])

  // Se ejecuta cada vez que el usuario escribe en un input
  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
    setSaved(false) // quita el mensaje de "guardado" si cambia algo
  }

  // Se ejecuta cuando el usuario hace clic en "Guardar"
  function handleSave() {
    saveConfig(form)
    setSaved(true)
    // El mensaje de confirmación desaparece después de 3 segundos
    setTimeout(() => setSaved(false), 3000)
  }

  return (
    <div>
      <div className="page-header">
        <div className="page-title">⚙ Configuración</div>
        <div className="page-subtitle">
          Ajusta la conexión al backend y los IDs de demo. Se guarda en tu navegador.
        </div>
      </div>

      {/* Mensaje de éxito */}
      {saved && (
        <div className="alert alert-ok">
          ✓ Configuración guardada correctamente
        </div>
      )}

      <div className="card">
        <div className="card-title">Conexión al backend</div>

        <div className="form-group">
          <label className="form-label">Backend URL</label>
          <input
            className="form-input"
            name="backendUrl"
            value={form.backendUrl}
            onChange={handleChange}
            placeholder="http://localhost:8000"
          />
          <div className="form-hint">
            URL base donde corre FastAPI. Sin barra al final.
            Ejemplo: <span className="mono">http://localhost:8000</span>
          </div>
        </div>

        <hr className="divider" />
        <div className="card-title">IDs de demo</div>
        <div className="form-hint mb-16">
          Estos IDs se usan como valores por defecto en las pantallas de Wallet y Tokens.
          Puedes cambiarlos en cualquier momento desde esas pantallas también.
        </div>

        <div className="form-group">
          <label className="form-label">Client ID (demo)</label>
          <input
            className="form-input"
            name="clientIdDemo"
            value={form.clientIdDemo}
            onChange={handleChange}
            placeholder="UUID del cliente de prueba"
          />
          <div className="form-hint">
            El UUID del cliente registrado en tu base de datos.
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Seller ID (demo)</label>
          <input
            className="form-input"
            name="sellerIdDemo"
            value={form.sellerIdDemo}
            onChange={handleChange}
            placeholder="UUID del vendedor de prueba"
          />
          <div className="form-hint">
            El UUID del vendedor registrado en tu base de datos.
          </div>
        </div>

        <button className="btn btn-primary" onClick={handleSave}>
          ✓ Guardar configuración
        </button>
      </div>

      {/* Info explicativa */}
      <div className="card mt-16">
        <div className="card-title">¿Cómo funciona esto?</div>
        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7, fontSize: 13 }}>
          El panel admin consume directamente los endpoints del backend de OffPay.
          No tiene base de datos propia ni servidor propio.
          Todo lo que ves en las otras secciones viene de llamadas HTTP a la URL que configures aquí.
          <br /><br />
          Si el backend cambia de puerto o de servidor, solo cambia la URL aquí y todo seguirá funcionando.
        </p>
      </div>
    </div>
  )
}
