import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Eye, RefreshCw, Trash2 } from "lucide-react";
import { apiClient } from "../api/apiClient";
import StatusBadge from "../components/StatusBadge";
import { FIELD_LIMITS, truncateText } from "../utils/validation";

export default function HistoryPage() {
  const [recipes, setRecipes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadRecipes = useCallback(async function loadRecipes() {
    try {
      setLoading(true);
      setError("");
      const response = await apiClient.listRecipes();
      setRecipes(Array.isArray(response) ? response : []);
    } catch (err) {
      setError(err.message || "No se pudo cargar el historial");
    } finally {
      setLoading(false);
    }
  }, []);

  async function handleDelete(id) {
    const confirmed = window.confirm("¿Eliminar esta receta del historial?");
    if (!confirmed) return;

    try {
      await apiClient.deleteRecipe(id);
      setRecipes((current) => current.filter((recipe) => recipe.id !== id));
    } catch (err) {
      setError(err.message || "No se pudo eliminar la receta");
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadRecipes();
  }, [loadRecipes]);

  return (
    <div className="page-stack">
      <header className="page-header split">
        <div>
          <p className="eyebrow">Historial</p>
          <h1>Recetas generadas</h1>
          <p>Consulta recetas anteriores y revisa si tienen escena 3D lista o pendiente.</p>
        </div>
        <button className="secondary-button" onClick={loadRecipes} type="button" disabled={loading}>
          <RefreshCw size={17} aria-hidden="true" />
          {loading ? "Actualizando..." : "Actualizar"}
        </button>
      </header>

      {error && (
        <div className="error-box large" role="alert" aria-live="polite">
          {error}
        </div>
      )}

      <section className="panel">
        {loading ? (
          <div className="loading-row" role="status" aria-live="polite">
            <div className="spinner" />
            <span>Cargando historial...</span>
          </div>
        ) : recipes.length === 0 ? (
          <p className="muted">Todavía no hay recetas generadas.</p>
        ) : (
          <div className="history-list">
            {recipes.map((recipe) => (
              <article className="history-card" key={recipe.id}>
                <div className="history-content">
                  <div className="history-title-row">
                    <h3>{recipe.title || "Receta sin título"}</h3>
                    <StatusBadge status={recipe.status} />
                  </div>
                  <p className="history-prompt" title={recipe.prompt || ""}>
                    {truncateText(recipe.prompt, FIELD_LIMITS.HISTORY_PROMPT_PREVIEW) || "Sin descripción guardada"}
                  </p>
                  <small>
                    {recipe.dessertType || "postre"} · {recipe.difficulty || "dificultad"} · {recipe.servings || "?"} porciones
                  </small>
                </div>

                <div className="history-actions">
                  <Link className="icon-button" to={`/recipes/${recipe.id}`} title="Ver detalle" aria-label="Ver detalle de receta">
                    <Eye size={18} aria-hidden="true" />
                  </Link>
                  <button className="icon-button danger" onClick={() => handleDelete(recipe.id)} title="Eliminar" aria-label="Eliminar receta" type="button">
                    <Trash2 size={18} aria-hidden="true" />
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
