import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/AppLayout'
import { OperatorPanelPage } from './pages/OperatorPanelPage'
import { TransactionsPage } from './pages/TransactionsPage'

export function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/painel" replace />} />
        <Route path="painel" element={<OperatorPanelPage />} />
        <Route path="transações" element={<TransactionsPage />} />
        <Route path="*" element={<Navigate to="/painel" replace />} />
      </Route>
    </Routes>
  )
}
