import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { CakeSlice, History, LogOut, Sparkles } from "lucide-react";
import { useAuth } from "../auth/AuthContext";

export default function AppLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="Navegación principal">
        <Link className="sidebar-brand" to="/dashboard" aria-label="Ir al panel principal de Pastry3D">
          <div className="brand-icon">
            <CakeSlice size={24} aria-hidden="true" />
          </div>
          <div>
            <strong>Pastry3D</strong>
            <span>AI Dessert Lab</span>
          </div>
        </Link>

        <nav className="sidebar-nav">
          <NavLink to="/dashboard">
            <Sparkles size={18} aria-hidden="true" />
            Generar
          </NavLink>
          <NavLink to="/history">
            <History size={18} aria-hidden="true" />
            Historial
          </NavLink>
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip" title={user?.email || "Usuario"}>
            <span>{user?.displayName || user?.email || "Usuario"}</span>
            <small>{user?.role || "USER"}</small>
          </div>
          <button className="ghost-button full" onClick={handleLogout} type="button">
            <LogOut size={17} aria-hidden="true" />
            Salir
          </button>
        </div>
      </aside>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
