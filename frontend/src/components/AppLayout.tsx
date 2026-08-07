import { NavLink, Outlet } from 'react-router-dom'

const navClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : undefined)

export function AppLayout() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-brand">
          <strong>SRM Credit Engine</strong>
          <small>Mesa de operacoes &middot; cessao de credito multimoedas</small>
        </div>
        <nav className="app-nav">
          <NavLink to="/painel" className={navClass}>
            Painel do operador
          </NavLink>
          <NavLink to="/transacoes" className={navClass}>
            Transacoes
          </NavLink>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
