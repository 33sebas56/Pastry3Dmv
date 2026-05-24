import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Sparkles } from "lucide-react";
import { useAuth } from "./AuthContext";
import {
  FIELD_LIMITS,
  clampText,
  normalizeEmail,
  validateEmail,
} from "../utils/validation";

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const emailError = email ? validateEmail(email) : "";
  const canSubmit = !submitting && !emailError && password.trim().length > 0;

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    const safeEmail = normalizeEmail(email);
    const safePassword = clampText(password, FIELD_LIMITS.PASSWORD_MAX);
    const validationError = validateEmail(safeEmail);

    if (validationError) {
      setError(validationError);
      return;
    }

    if (!safePassword) {
      setError("La contraseña es obligatoria");
      return;
    }

    setSubmitting(true);

    try {
      await login({ email: safeEmail, password: safePassword });
      navigate("/dashboard");
    } catch (err) {
      setError(err.message || "No se pudo iniciar sesión");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="login-title">
        <div className="brand-mark">
          <Sparkles size={28} aria-hidden="true" />
        </div>
        <p className="eyebrow">Pastry3D</p>
        <h1 id="login-title">Entrar al laboratorio dulce</h1>
        <p className="muted">
          Genera recetas con IA y visualiza composiciones 3D usando modelos reutilizables.
        </p>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <label htmlFor="login-email">
            Correo
            <input
              id="login-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(clampText(event.target.value, FIELD_LIMITS.EMAIL_MAX))}
              onBlur={() => setEmail((current) => normalizeEmail(current))}
              placeholder="tu@email.com"
              autoComplete="email"
              inputMode="email"
              maxLength={FIELD_LIMITS.EMAIL_MAX}
              aria-invalid={Boolean(emailError)}
              required
            />
            <span className="field-hint">Usa el correo con el que creaste tu cuenta.</span>
          </label>

          <label htmlFor="login-password">
            Contraseña
            <input
              id="login-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(clampText(event.target.value, FIELD_LIMITS.PASSWORD_MAX))}
              placeholder="Tu contraseña"
              autoComplete="current-password"
              maxLength={FIELD_LIMITS.PASSWORD_MAX}
              aria-invalid={Boolean(error && !password)}
              required
            />
          </label>

          {error && (
            <div className="error-box" role="alert" aria-live="polite">
              {error}
            </div>
          )}

          <button className="primary-button" type="submit" disabled={!canSubmit}>
            {submitting ? "Entrando..." : "Iniciar sesión"}
          </button>
        </form>

        <p className="auth-switch">
          ¿No tienes cuenta? <Link to="/register">Crear cuenta</Link>
        </p>
      </section>
    </main>
  );
}
