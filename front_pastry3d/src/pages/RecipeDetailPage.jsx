import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, RefreshCw } from "lucide-react";
import { apiClient } from "../api/apiClient";
import RecipeResult from "../components/RecipeResult";

export default function RecipeDetailPage() {
  const { id } = useParams();
  const [recipe, setRecipe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [rebuilding, setRebuilding] = useState(false);

  const loadRecipe = useCallback(async function loadRecipe() {
    try {
      setLoading(true);
      setError("");
      const response = await apiClient.getRecipe(id);
      setRecipe({
        recipeId: response.id,
        title: response.title,
        status: response.status,
        strategy: response.strategy,
        modelUrl: response.modelUrl,
        sceneJson: response.sceneJson,
        recipeJson: response.recipeJson,
        visualPlanJson: response.visualPlanJson,
        modelPrompt: response.modelPrompt,
      });
    } catch (err) {
      setError(err.message || "No se pudo cargar la receta");
    } finally {
      setLoading(false);
    }
  }, [id]);

  async function handleRebuild() {
    try {
      setRebuilding(true);
      setError("");
      const response = await apiClient.rebuildComposition(id);
      setRecipe(response);
    } catch (err) {
      setError(err.message || "No se pudo reconstruir la composición");
    } finally {
      setRebuilding(false);
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadRecipe();
  }, [loadRecipe]);

  return (
    <div className="page-stack">
      <header className="page-header split">
        <div>
          <Link className="back-link" to="/history">
            <ArrowLeft size={17} />
            Volver al historial
          </Link>
          <h1>Detalle de receta</h1>
          <p>Revisa la receta, la escena y el estado de la composición visual.</p>
        </div>

        <button className="secondary-button" onClick={handleRebuild} disabled={rebuilding}>
          <RefreshCw size={17} />
          {rebuilding ? "Reconstruyendo..." : "Reconstruir escena"}
        </button>
      </header>

      {error && <div className="error-box large">{error}</div>}

      {loading ? <div className="panel muted">Cargando receta...</div> : <RecipeResult result={recipe} />}
    </div>
  );
}