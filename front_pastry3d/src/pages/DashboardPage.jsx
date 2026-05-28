import { useState } from "react";
import { apiClient } from "../api/apiClient";
import PromptComposer from "../components/PromptComposer";
import RecipeResult from "../components/RecipeResult";
import { FIELD_LIMITS, clampText, validatePrompt } from "../utils/validation";

export default function DashboardPage() {
  const [prompt, setPrompt] = useState("Milhojas con rosa azul");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  function updatePrompt(value) {
    setPrompt(clampText(value, FIELD_LIMITS.PROMPT_MAX));
  }

  async function handleGenerate(event) {
    event.preventDefault();

    const safePrompt = clampText(prompt, FIELD_LIMITS.PROMPT_MAX).trim();
    const validationError = validatePrompt(safePrompt);

    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);
    setError("");

    try {
      const response = await apiClient.generateRecipe(safePrompt);
      setResult(response);
    } catch (err) {
      setError(err.message || "No se pudo generar la receta");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Panel principal</p>
          <h1>Genera postres con IA y modelos 3D reutilizables</h1>
          <p>
            La IA interpreta la receta. El backend busca piezas GLB disponibles y compone una escena 3D.
          </p>
        </div>
      </header>

      <PromptComposer prompt={prompt} setPrompt={updatePrompt} onSubmit={handleGenerate} loading={loading} />

      {error && <div className="error-box large">{error}</div>}

      <RecipeResult result={result} />
    </div>
  );
}
