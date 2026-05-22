// ============================================================
// SIDEBAR — actualizado con nueva sección de fraude
// ============================================================

import { NavLink } from 'react-router-dom'

const NAV_ITEMS = [
  {
    section: 'Sistema',
    links: [
      { to: '/status',  icon: '◈', label: 'Estado del sistema' },
      { to: '/config',  icon: '⚙', label: 'Configuración'      },
    ]
  },
  {
    section: 'Usuarios',
    links: [
      { to: '/wallet',  icon: '⛁', label: 'Consulta Wallet'    },
      { to: '/tokens',  icon: '◆', label: 'Tokens del cliente' },
    ]
  },
  {
    section: 'Registros',
    links: [
      { to: '/transactions',      icon: '≡', label: 'Transacciones'      },
      { to: '/invalid-attempts',  icon: '⚠', label: 'Intentos inválidos' },
      { to: '/fraud-alerts',      icon: '⚔', label: 'Alertas de fraude'  },
    ]
  },
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-label">Admin Panel</div>
        <div className="brand-name">OffPay</div>
        <div className="brand-sub">MVP · v1.0</div>
      </div>

      <nav className="sidebar-nav">
        {NAV_ITEMS.map(({ section, links }) => (
          <div key={section}>
            <div className="nav-section-label">{section}</div>
            {links.map(({ to, icon, label }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
              >
                <span className="nav-icon">{icon}</span>
                {label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        Solo lectura<br />
        No modifica datos
      </div>
    </aside>
  )
}
