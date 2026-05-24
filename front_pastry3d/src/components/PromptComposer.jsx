import { Wand2 } from "lucide-react";
import { FIELD_LIMITS, clampText, getTextLength, validatePrompt } from "../utils/validation";

const EXAMPLES = [
  "Quiero una milhojas con una rosa azul encima",
  "Hazme un pastel de vainilla con fresas arriba",
  "Quiero un cupcake elegante con crema y una vela",
  "Crea un pastel redondo con decoración de chocolate",
];

export default function PromptComposer({ prompt, setPrompt, onSubmit, loading }) {
  const promptLength = getTextLength(prompt);
  const promptError = prompt ? validatePrompt(prompt) : "";
  const remaining = FIELD_LIMITS.PROMPT_MAX - promptLength;
  const canSubmit = !loading && !promptError && prompt.trim().length > 0;

  function handlePromptChange(event) {
    setPrompt(clampText(event.target.value, FIELD_LIMITS.PROMPT_MAX));
  }

  return (
    <section className="panel prompt-panel" aria-labelledby="prompt-title">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Generador IA</p>
          <h2 id="prompt-title">Describe el postre que quieres crear</h2>
          <p className="muted panel-subtitle">
            Escribe una idea clara. Evita datos personales; solo necesitamos la descripción del postre.
          </p>
        </div>
        <div className="round-icon">
          <Wand2 size={22} aria-hidden="true" />
        </div>
      </div>

      <form onSubmit={onSubmit} className="prompt-form" noValidate>
        <label htmlFor="dessert-prompt" className="sr-only">
          Descripción del postre
        </label>
        <textarea
          id="dessert-prompt"
          value={prompt}
          onChange={handlePromptChange}
          placeholder="Ejemplo: quiero una milhojas con una rosa azul encima..."
          rows={5}
          minLength={FIELD_LIMITS.PROMPT_MIN}
          maxLength={FIELD_LIMITS.PROMPT_MAX}
          aria-describedby="prompt-counter prompt-hint"
          aria-invalid={Boolean(promptError)}
        />
        <div className="field-meta" id="prompt-counter">
          <span className="field-hint" id="prompt-hint">
            Mínimo {FIELD_LIMITS.PROMPT_MIN} caracteres. Máximo {FIELD_LIMITS.PROMPT_MAX}.
          </span>
          <span className={`char-counter ${remaining < 80 ? "is-warning" : ""}`}>
            {promptLength}/{FIELD_LIMITS.PROMPT_MAX}
          </span>
        </div>

        {promptError && <p className="field-error">{promptError}</p>}

        <button className="primary-button" type="submit" disabled={!canSubmit}>
          {loading ? "Pensando receta..." : "Generar receta y escena"}
        </button>
      </form>

      <div className="example-list" aria-label="Ejemplos de prompts">
        {EXAMPLES.map((example) => (
          <button key={example} type="button" onClick={() => setPrompt(example)}>
            {example}
          </button>
        ))}
      </div>
    </section>
  );
}
