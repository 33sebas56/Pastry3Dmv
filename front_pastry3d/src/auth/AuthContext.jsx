import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { apiClient, clearToken, getToken, setToken } from "../api/apiClient";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(apiClient.getStoredUser());
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState("");

  async function refreshUser() {
    const token = getToken();

    if (!token) {
      setUser(null);
      apiClient.clearStoredUser();
      return null;
    }

    const currentUser = await apiClient.me();
    setUser(currentUser);
    apiClient.setStoredUser(currentUser);
    return currentUser;
  }

  useEffect(() => {
    async function bootstrap() {
      try {
        setAuthError("");

        if (!getToken()) {
          setUser(null);
          apiClient.clearStoredUser();
          return;
        }

        await refreshUser();
      } catch {
        clearToken();
        apiClient.clearStoredUser();
        setUser(null);
      } finally {
        setLoading(false);
      }
    }

    bootstrap();
  }, []);

  async function login(credentials) {
    setAuthError("");

    const response = await apiClient.login(credentials);

    if (!response?.token) {
      throw new Error("No se recibió token de autenticación");
    }

    setToken(response.token);

    const currentUser = await refreshUser();
    return currentUser;
  }

  async function register(payload) {
    setAuthError("");

    const response = await apiClient.register(payload);

    /*
      Caso normal con SMTP activo:
      El backend crea el usuario, manda correo y devuelve token=null / enabled=false.
      En ese caso NO debemos llamar /auth/me todavía.
    */
    if (!response?.token || response?.enabled === false) {
      clearToken();
      apiClient.clearStoredUser();
      setUser(null);

      return {
        pendingEmailConfirmation: true,
        email: response?.email || payload?.email,
        message: "Cuenta creada. Revisa tu correo para confirmar tu cuenta antes de iniciar sesión.",
        raw: response,
      };
    }

    setToken(response.token);

    const currentUser = await refreshUser();

    return {
      pendingEmailConfirmation: false,
      user: currentUser,
      raw: response,
    };
  }

  function logout() {
    clearToken();
    apiClient.clearStoredUser();
    setUser(null);
  }

  const value = useMemo(
    () => ({
      user,
      loading,
      authError,
      isAuthenticated: Boolean(user),
      login,
      register,
      logout,
      refreshUser,
    }),
    [user, loading, authError]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth debe usarse dentro de AuthProvider");
  }

  return context;
}