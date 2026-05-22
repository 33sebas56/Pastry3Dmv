import { useState } from "react";
import { apiClient } from "../api/apiClient";
import PromptComposer from "../components/PromptComposer";
import RecipeResult from "../components/RecipeResult";

export default function DashboardPage() {
  const [prompt, setPrompt] = useState("Quiero una milhojas con una rosa azul encima");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleGenerate(event) {
    event.preventDefault();
    if (!prompt.trim()) return;

    setLoading(true);
    setError("");

    try {
      const response = await apiClient.generateRecipe(prompt.trim());
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

      <PromptComposer prompt={prompt} setPrompt={setPrompt} onSubmit={handleGenerate} loading={loading} />

      {error && <div className="error-box large">{error}</div>}

      <RecipeResult result={result} />
    </div>
  );
}