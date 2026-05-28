import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CakeSlice, CheckCircle2, Eye, EyeOff, MailCheck } from "lucide-react";
import { useAuth } from "./AuthContext";
import {
  FIELD_LIMITS,
  clampText,
  getTextLength,
  normalizeEmail,
  passwordChecks,
  validateDisplayName,
  validateEmail,
  validatePassword,
} from "../utils/validation";

export default function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const checks = useMemo(() => passwordChecks(password), [password]);
  const displayNameLength = getTextLength(displayName);
  const passwordError = password ? validatePassword(password) : "";
  const canSubmit = !submitting && displayName.trim() && email.trim() && password && !passwordError;

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setNotice("");

    const safeDisplayName = clampText(displayName, FIELD_LIMITS.DISPLAY_NAME_MAX).trim();
    const safeEmail = normalizeEmail(email);
    const safePassword = clampText(password, FIELD_LIMITS.PASSWORD_MAX);

    const validationError =
      validateDisplayName(safeDisplayName) ||
      validateEmail(safeEmail) ||
      validatePassword(safePassword);

    if (validationError) {
      setError(validationError);
      return;
    }

    setSubmitting(true);

    try {
      const response = await register({
        displayName: safeDisplayName,
        email: safeEmail,
        password: safePassword,
      });

      if (response?.pendingEmailConfirmation) {
        setNotice(`Cuenta creada. Revisa ${response.email || safeEmail} para confirmar tu correo antes de iniciar sesión.`);
        setPassword("");
        return;
      }

      navigate("/dashboard");
    } catch (err) {
      setError(err.message || "No se pudo crear la cuenta");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="register-title">
        <div className="brand-mark">
          <CakeSlice size={28} aria-hidden="true" />
        </div>
        <p className="eyebrow">Pastry3D</p>
        <h1 id="register-title">Crear cuenta</h1>
        <p className="muted">
          Empieza a construir recetas y escenas 3D con una biblioteca de modelos reutilizables.
        </p>

        {notice && (
          <div className="notice-box" role="status" aria-live="polite">
            <MailCheck size={20} aria-hidden="true" />
            <div>
              <strong>Confirma tu correo</strong>
              <p>{notice}</p>
            </div>
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <label htmlFor="register-name">
            Nombre visible
            <input
              id="register-name"
              value={displayName}
              onChange={(event) => setDisplayName(clampText(event.target.value, FIELD_LIMITS.DISPLAY_NAME_MAX))}
              onBlur={() => setDisplayName((current) => current.trim())}
              placeholder="Sebastián"
              autoComplete="name"
              minLength={FIELD_LIMITS.DISPLAY_NAME_MIN}
              maxLength={FIELD_LIMITS.DISPLAY_NAME_MAX}
              aria-describedby="register-name-counter"
              required
            />
            <span className="field-meta" id="register-name-counter">
              <span className="field-hint">Será visible dentro de tu cuenta.</span>
              <span className="char-counter">
                {displayNameLength}/{FIELD_LIMITS.DISPLAY_NAME_MAX}
              </span>
            </span>
          </label>

          <label htmlFor="register-email">
            Correo
            <input
              id="register-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(clampText(event.target.value, FIELD_LIMITS.EMAIL_MAX))}
              onBlur={() => setEmail((current) => normalizeEmail(current))}
              placeholder="tu@email.com"
              autoComplete="email"
              inputMode="email"
              maxLength={FIELD_LIMITS.EMAIL_MAX}
              required
            />
          </label>

          <label htmlFor="register-password">
            Contraseña
            <div className="password-field">
              <input
                id="register-password"
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(event) => setPassword(clampText(event.target.value, FIELD_LIMITS.PASSWORD_MAX))}
                placeholder="Mínimo 8 caracteres"
                autoComplete="new-password"
                minLength={FIELD_LIMITS.PASSWORD_MIN}
                maxLength={FIELD_LIMITS.PASSWORD_MAX}
                aria-describedby="password-rules"
                aria-invalid={Boolean(passwordError)}
                required
              />
              <button
                className="password-toggle"
                type="button"
                onClick={() => setShowPassword((current) => !current)}
                aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
              >
                {showPassword ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
              </button>
            </div>
          </label>

          <div className="password-rules" id="password-rules" aria-label="Requisitos de contraseña">
            {checks.map((check) => (
              <span className={check.valid ? "is-valid" : ""} key={check.label}>
                <CheckCircle2 size={14} aria-hidden="true" />
                {check.label}
              </span>
            ))}
          </div>

          {error && (
            <div className="error-box" role="alert" aria-live="polite">
              {error}
            </div>
          )}

          <button className="primary-button" type="submit" disabled={!canSubmit}>
            {submitting ? "Creando cuenta..." : "Registrarme"}
          </button>
        </form>

        <p className="auth-switch">
          ¿Ya tienes cuenta? <Link to="/login">Iniciar sesión</Link>
        </p>
      </section>
    </main>
  );
}