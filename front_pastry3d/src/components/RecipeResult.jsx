import { useState } from "react";
import { BookOpen, Clock, PackageSearch, Sparkles, Users } from "lucide-react";
import { apiClient } from "../api/apiClient";
import SceneViewer from "./SceneViewer";
import StatusBadge from "./StatusBadge";

function parseJson(value, fallback) {
  if (!value) return fallback;
  if (typeof value === "object") return value;

  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}

function readIngredients(recipe) {
  const ingredients = recipe?.ingredients;
  if (!Array.isArray(ingredients)) return [];
  return ingredients;
}

function readSteps(recipe) {
  const steps = recipe?.steps;
  if (!Array.isArray(steps)) return [];
  return steps;
}

function canGenerateFromScratch(result, missingAssets) {
  return (
    result?.recipeId &&
    result?.status === "PENDING_MANUAL_MODEL" &&
    Array.isArray(missingAssets) &&
    missingAssets.length > 0
  );
}

function cleanProviderError(message) {
  if (!message) {
    return "El proveedor externo no devolvió un mensaje detallado.";
  }

  return message
    .replaceAll("\\n", "\n")
    .replaceAll("\\\"", "\"")
    .trim();
}

export default function RecipeResult({ result }) {
  const [generationJob, setGenerationJob] = useState(null);
  const [generationLoading, setGenerationLoading] = useState(false);
  const [generationError, setGenerationError] = useState("");

  if (!result) {
    return (
      <section className="panel empty-result">
        <h2>Tu receta aparecerá aquí</h2>
        <p>
          Escribe una idea de postre. Gemini generará la receta y el backend intentará componer la escena con los GLB disponibles.
        </p>
      </section>
    );
  }

  const recipe = parseJson(result.recipeJson, {});
  const missingAssets = parseJson(result.missingAssetsJson, []);
  const ingredients = readIngredients(recipe);
  const steps = readSteps(recipe);
  const activeStatus = generationJob?.status || result.status;
  const showGenerateButton = canGenerateFromScratch(result, missingAssets);

  async function handleGenerateFromScratch() {
    const confirmed = window.confirm(
      "Esto enviará el prompt técnico a Tripo para generar un GLB. Puede consumir créditos de tu cuenta Tripo. ¿Continuar?"
    );

    if (!confirmed) return;

    try {
      setGenerationLoading(true);
      setGenerationError("");

      const response = await apiClient.startTripoGeneration(result.recipeId);
      setGenerationJob(response);
    } catch (err) {
      setGenerationError(err.message || "No se pudo iniciar la generación externa.");
    } finally {
      setGenerationLoading(false);
    }
  }

  async function handleSyncJob() {
    if (!generationJob?.id) return;

    try {
      setGenerationLoading(true);
      setGenerationError("");

      const response = await apiClient.syncGenerationJob(generationJob.id);
      setGenerationJob(response);
    } catch (err) {
      setGenerationError(err.message || "No se pudo sincronizar el job de generación.");
    } finally {
      setGenerationLoading(false);
    }
  }

  return (
    <section className="result-grid">
      <div className="panel recipe-panel">
        <div className="result-title-row">
          <div>
            <p className="eyebrow">Resultado</p>
            <h2>{result.title || recipe.title || "Receta generada"}</h2>
          </div>
          <StatusBadge status={activeStatus} />
        </div>

        <div className="recipe-meta">
          <span>
            <BookOpen size={16} />
            {recipe.difficulty || "Dificultad no definida"}
          </span>
          <span>
            <Users size={16} />
            {recipe.servings || "?"} porciones
          </span>
          <span>
            <Clock size={16} />
            {recipe.estimatedMinutes || "?"} min
          </span>
        </div>

        {recipe.description && (
          <p className="recipe-description">{recipe.description}</p>
        )}

        {missingAssets.length > 0 && (
          <div className="warning-box">
            <PackageSearch size={18} />
            <div>
              <strong>Faltan modelos para completar la escena</strong>
              <p>{missingAssets.join(", ")}</p>

              <p className="muted">
                Pastry3D dejó preparado el prompt técnico. Si activas Tripo, el modelo puede generarse como GLB y guardarse en la biblioteca.
              </p>

              {showGenerateButton && (
                <div className="generation-actions">
                  <button
                    className="primary-button small"
                    type="button"
                    onClick={handleGenerateFromScratch}
                    disabled={generationLoading}
                  >
                    <Sparkles size={17} />
                    {generationLoading ? "Enviando a Tripo..." : "Generar con Tripo"}
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {generationError && (
          <div className="error-box large">
            <strong>Error de generación</strong>
            <p>{generationError}</p>
          </div>
        )}

        {generationJob && (
          <div className="generation-box">
            <strong>Job de generación 3D</strong>
            <p>Estado: {generationJob.status}</p>

            {generationJob.status === "FAILED" && (
              <div className="error-box large">
                <strong>Tripo rechazó o falló la solicitud.</strong>
                <p>{cleanProviderError(generationJob.errorMessage)}</p>
              </div>
            )}

            {generationJob.modelAssetId ? (
              <p>Modelo generado y guardado en la biblioteca.</p>
            ) : generationJob.status === "FAILED" ? (
              <p className="muted">
                La generación quedó registrada, pero Tripo no aceptó o no completó el modelo con la configuración actual. La experiencia principal sigue funcionando con modelos GLB locales y composición por assets.
              </p>
            ) : (
              <p>
                La generación fue enviada. Si el modelo aún no aparece, sincroniza el estado en unos segundos.
              </p>
            )}

            <button
              className="secondary-button"
              type="button"
              onClick={handleSyncJob}
              disabled={generationLoading || !generationJob.id}
            >
              {generationLoading ? "Consultando..." : "Sincronizar generación"}
            </button>
          </div>
        )}

        <div className="recipe-columns">
          <div>
            <h3>Ingredientes</h3>
            {ingredients.length === 0 ? (
              <p className="muted">La IA no devolvió ingredientes estructurados.</p>
            ) : (
              <ul className="clean-list">
                {ingredients.map((item, index) => (
                  <li key={`${item.name || "ingredient"}-${index}`}>
                    <span>{item.name || item.ingredient || "Ingrediente"}</span>
                    <small>
                      {[item.quantity, item.unit].filter(Boolean).join(" ") ||
                        "Cantidad al gusto"}
                    </small>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <h3>Preparación</h3>
            {steps.length === 0 ? (
              <p className="muted">La IA no devolvió pasos estructurados.</p>
            ) : (
              <ol className="steps-list">
                {steps.map((step, index) => (
                  <li key={`${step.title || "step"}-${index}`}>
                    <strong>{step.title || `Paso ${index + 1}`}</strong>
                    <p>{step.description || step.text || "Sin descripción"}</p>
                  </li>
                ))}
              </ol>
            )}
          </div>
        </div>
      </div>

      <div className="panel visual-panel">
        <div className="panel-header compact">
          <div>
            <p className="eyebrow">Vista 3D</p>
            <h2>Composición visual</h2>
          </div>
        </div>

        <SceneViewer
          sceneJson={result.sceneJson}
          modelUrl={result.modelUrl}
          status={activeStatus}
        />
      </div>
    </section>
  );
}