import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { apiClient, clearToken, getToken, setToken } from "../api/apiClient";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState("");

  const isAuthenticated = Boolean(getToken());

  async function loadMe() {
    const token = getToken();
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const me = await apiClient.me();
      setUser(me);
    } catch (error) {
      clearToken();
      setUser(null);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadMe();
  }, []);

  async function login(email, password) {
    setAuthError("");
    const response = await apiClient.login({ email, password });
    setToken(response.token);
    const me = await apiClient.me();
    setUser(me);
    return me;
  }

  async function register(displayName, email, password) {
    setAuthError("");
    const response = await apiClient.register({ displayName, email, password });
    setToken(response.token);
    const me = await apiClient.me();
    setUser(me);
    return me;
  }

  function logout() {
    clearToken();
    setUser(null);
  }

  const value = useMemo(
    () => ({
      user,
      loading,
      authError,
      setAuthError,
      isAuthenticated,
      login,
      register,
      logout,
      refreshUser: loadMe,
    }),
    [user, loading, authError, isAuthenticated]
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