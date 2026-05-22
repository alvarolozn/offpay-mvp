// ============================================================
// APP — actualizado con ruta de alertas de fraude
// ============================================================

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Sidebar from './components/Sidebar.jsx'

import ConfigPage           from './pages/ConfigPage.jsx'
import StatusPage           from './pages/StatusPage.jsx'
import WalletPage           from './pages/WalletPage.jsx'
import TokensPage           from './pages/TokensPage.jsx'
import TransactionsPage     from './pages/TransactionsPage.jsx'
import InvalidAttemptsPage  from './pages/InvalidAttemptsPage.jsx'
import FraudAlertsPage      from './pages/FraudAlertsPage.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <div className="layout">
        <Sidebar />
        <main className="main-content">
          <Routes>
            <Route path="/"                  element={<Navigate to="/status" replace />} />
            <Route path="/status"            element={<StatusPage />} />
            <Route path="/config"            element={<ConfigPage />} />
            <Route path="/wallet"            element={<WalletPage />} />
            <Route path="/tokens"            element={<TokensPage />} />
            <Route path="/transactions"      element={<TransactionsPage />} />
            <Route path="/invalid-attempts"  element={<InvalidAttemptsPage />} />
            <Route path="/fraud-alerts"      element={<FraudAlertsPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
