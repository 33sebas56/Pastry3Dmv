import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Eye, Trash2 } from "lucide-react";
import { apiClient } from "../api/apiClient";
import StatusBadge from "../components/StatusBadge";

export default function HistoryPage() {
  const [recipes, setRecipes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadRecipes() {
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
  }

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
    loadRecipes();
  }, []);

  return (
    <div className="page-stack">
      <header className="page-header split">
        <div>
          <p className="eyebrow">Historial</p>
          <h1>Recetas generadas</h1>
          <p>Consulta recetas anteriores y revisa si tienen escena 3D lista o pendiente.</p>
        </div>
        <button className="secondary-button" onClick={loadRecipes}>Actualizar</button>
      </header>

      {error && <div className="error-box large">{error}</div>}

      <section className="panel">
        {loading ? (
          <p className="muted">Cargando historial...</p>
        ) : recipes.length === 0 ? (
          <p className="muted">Todavía no hay recetas generadas.</p>
        ) : (
          <div className="history-list">
            {recipes.map((recipe) => (
              <article className="history-card" key={recipe.id}>
                <div>
                  <div className="history-title-row">
                    <h3>{recipe.title || "Receta sin título"}</h3>
                    <StatusBadge status={recipe.status} />
                  </div>
                  <p>{recipe.prompt}</p>
                  <small>
                    {recipe.dessertType || "postre"} · {recipe.difficulty || "dificultad"} · {recipe.servings || "?"} porciones
                  </small>
                </div>

                <div className="history-actions">
                  <Link className="icon-button" to={`/recipes/${recipe.id}`} title="Ver detalle">
                    <Eye size={18} />
                  </Link>
                  <button className="icon-button danger" onClick={() => handleDelete(recipe.id)} title="Eliminar">
                    <Trash2 size={18} />
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