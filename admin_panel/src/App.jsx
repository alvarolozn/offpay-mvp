// ============================================================
// OFFPAY ADMIN PANEL — APP (Raíz de la aplicación)
// ============================================================
//
// Este archivo une todo. Define qué página mostrar
// según la URL que esté activa en el navegador.
//
// ¿Qué es React Router?
// Es una librería que permite cambiar de "página" sin recargar
// el navegador. Cuando haces clic en "Transacciones" en el
// sidebar, la URL cambia a /transactions y este archivo
// sabe qué componente mostrar.
// ============================================================

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Sidebar from './components/Sidebar.jsx'

// Páginas — cada una es una sección del panel
import ConfigPage           from './pages/ConfigPage.jsx'
import StatusPage           from './pages/StatusPage.jsx'
import WalletPage           from './pages/WalletPage.jsx'
import TokensPage           from './pages/TokensPage.jsx'
import TransactionsPage     from './pages/TransactionsPage.jsx'
import InvalidAttemptsPage  from './pages/InvalidAttemptsPage.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <div className="layout">
        {/* Sidebar siempre visible */}
        <Sidebar />

        {/* Contenido principal — cambia según la ruta */}
        <main className="main-content">
          <Routes>
            {/* Redirige la raíz "/" al estado del sistema */}
            <Route path="/"                  element={<Navigate to="/status" replace />} />
            <Route path="/status"            element={<StatusPage />} />
            <Route path="/config"            element={<ConfigPage />} />
            <Route path="/wallet"            element={<WalletPage />} />
            <Route path="/tokens"            element={<TokensPage />} />
            <Route path="/transactions"      element={<TransactionsPage />} />
            <Route path="/invalid-attempts"  element={<InvalidAttemptsPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
