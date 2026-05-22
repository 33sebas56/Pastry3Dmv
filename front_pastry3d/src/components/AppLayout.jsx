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
      <aside className="sidebar">
        <Link className="sidebar-brand" to="/dashboard">
          <div className="brand-icon">
            <CakeSlice size={24} />
          </div>
          <div>
            <strong>Pastry3D</strong>
            <span>AI Dessert Lab</span>
          </div>
        </Link>

        <nav className="sidebar-nav">
          <NavLink to="/dashboard">
            <Sparkles size={18} />
            Generar
          </NavLink>
          <NavLink to="/history">
            <History size={18} />
            Historial
          </NavLink>
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip">
            <span>{user?.displayName || user?.email || "Usuario"}</span>
            <small>{user?.role || "USER"}</small>
          </div>
          <button className="ghost-button full" onClick={handleLogout}>
            <LogOut size={17} />
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