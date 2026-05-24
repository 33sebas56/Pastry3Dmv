import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { AlertTriangle, ArrowRight, CheckCircle2, MailCheck } from "lucide-react";
import { apiClient } from "../api/apiClient";

export default function ConfirmEmailPage() {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("Confirmando tu cuenta...");
  const token = searchParams.get("token") || "";

  useEffect(() => {
    let active = true;

    async function confirmEmail() {
      if (!token) {
        setStatus("error");
        setMessage("El enlace de confirmación no contiene un token válido.");
        return;
      }

      try {
        const response = await apiClient.confirmEmail(token);

        if (!active) return;

        setStatus("success");
        setMessage(response?.message || "Tu cuenta fue confirmada correctamente.");
      } catch (err) {
        if (!active) return;

        setStatus("error");
        setMessage(err.message || "No se pudo confirmar la cuenta. Solicita un nuevo enlace o intenta registrarte otra vez.");
      }
    }

    confirmEmail();

    return () => {
      active = false;
    };
  }, [token]);

  const isLoading = status === "loading";
  const isSuccess = status === "success";

  return (
    <main className="auth-page">
      <section className="auth-card confirmation-card" aria-labelledby="confirm-title">
        <div className={`brand-mark ${isSuccess ? "brand-mark-success" : status === "error" ? "brand-mark-danger" : ""}`}>
          {isLoading ? (
            <MailCheck size={28} aria-hidden="true" />
          ) : isSuccess ? (
            <CheckCircle2 size={28} aria-hidden="true" />
          ) : (
            <AlertTriangle size={28} aria-hidden="true" />
          )}
        </div>
        <p className="eyebrow">Verificación de correo</p>
        <h1 id="confirm-title">
          {isLoading ? "Confirmando cuenta" : isSuccess ? "Cuenta confirmada" : "Enlace no válido"}
        </h1>
        <p className="muted">{message}</p>

        {isLoading && (
          <div className="loading-row" role="status" aria-live="polite">
            <div className="spinner" />
            <span>Validando enlace seguro...</span>
          </div>
        )}

        {!isLoading && (
          <div className="confirmation-actions">
            <Link className="primary-button" to="/login">
              Ir a iniciar sesión
              <ArrowRight size={17} aria-hidden="true" />
            </Link>
            {!isSuccess && (
              <Link className="secondary-button" to="/register">
                Crear cuenta nueva
              </Link>
            )}
          </div>
        )}
      </section>
    </main>
  );
}
