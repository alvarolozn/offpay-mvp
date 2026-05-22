// ============================================================
// OFFPAY ADMIN PANEL — SIDEBAR (Navegación lateral)
// ============================================================
//
// Este componente es la barra de navegación izquierda.
// Siempre está visible sin importar en qué sección estés.
//
// ¿Qué son los "componentes" en React?
// Son bloques reutilizables de interfaz. Como piezas de LEGO.
// Este componente genera el sidebar y lo puedes usar en App.jsx
// sin volver a escribir todo el HTML.
// ============================================================

import { NavLink } from 'react-router-dom'

const NAV_ITEMS = [
  {
    section: 'Sistema',
    links: [
      { to: '/status',   icon: '◈', label: 'Estado del sistema' },
      { to: '/config',   icon: '⚙', label: 'Configuración'      },
    ]
  },
  {
    section: 'Usuarios',
    links: [
      { to: '/wallet',   icon: '◎', label: 'Consulta Wallet'    },
      { to: '/tokens',   icon: '◆', label: 'Tokens del cliente' },
    ]
  },
  {
    section: 'Registros',
    links: [
      { to: '/transactions',       icon: '≡', label: 'Transacciones'      },
      { to: '/invalid-attempts',   icon: '⚠', label: 'Intentos inválidos' },
    ]
  },
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      {/* Logo / Nombre del sistema */}
      <div className="sidebar-brand">
        <div className="brand-label">Admin Panel</div>
        <div className="brand-name">OffPay</div>
        <div className="brand-sub">MVP · v1.0</div>
      </div>

      {/* Navegación */}
      <nav className="sidebar-nav">
        {NAV_ITEMS.map(({ section, links }) => (
          <div key={section}>
            <div className="nav-section-label">{section}</div>
            {links.map(({ to, icon, label }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  'nav-link' + (isActive ? ' active' : '')
                }
              >
                <span className="nav-icon">{icon}</span>
                {label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      {/* Pie del sidebar */}
      <div className="sidebar-footer">
        Solo lectura<br />
        No modifica datos
      </div>
    </aside>
  )
}
