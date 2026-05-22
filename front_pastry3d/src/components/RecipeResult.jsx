import { BookOpen, Clock, PackageSearch, Users } from "lucide-react";
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

export default function RecipeResult({ result }) {
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

  return (
    <section className="result-grid">
      <div className="panel recipe-panel">
        <div className="result-title-row">
          <div>
            <p className="eyebrow">Resultado</p>
            <h2>{result.title || recipe.title || "Receta generada"}</h2>
          </div>
          <StatusBadge status={result.status} />
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

        {recipe.description && <p className="recipe-description">{recipe.description}</p>}

        {missingAssets.length > 0 && (
          <div className="warning-box">
            <PackageSearch size={18} />
            <div>
              <strong>Faltan modelos para completar la escena</strong>
              <p>{missingAssets.join(", ")}</p>
            </div>
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
                      {[item.quantity, item.unit].filter(Boolean).join(" ") || "Cantidad al gusto"}
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
        <SceneViewer sceneJson={result.sceneJson} modelUrl={result.modelUrl} status={result.status} />
      </div>
    </section>
  );
}