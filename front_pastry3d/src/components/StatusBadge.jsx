const STATUS_LABELS = {
  READY: "Escena lista",
  REUSED: "Modelo reutilizado",
  PENDING: "Pendiente",
  PENDING_MANUAL_MODEL: "Modelo pendiente",
  GENERATING: "Generando modelo",
  FAILED: "Error",
  IMPORTED: "Importado",
};

export default function StatusBadge({ status }) {
  const normalized = status || "PENDING";
  const label = STATUS_LABELS[normalized] || normalized;

  return <span className={`status-badge status-${normalized.toLowerCase()}`}>{label}</span>;
}