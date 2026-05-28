import { Wand2 } from "lucide-react";
import { FIELD_LIMITS, clampText, getTextLength, validatePrompt } from "../utils/validation";

const EXAMPLES = [
  "Milhojas con rosa azul",
  "Pastel vainilla con fresas",
  "Cupcake con crema y vela",
  "Pastel con chocolate",
];

export default function PromptComposer({ prompt, setPrompt, onSubmit, loading }) {
  const promptLength = getTextLength(prompt);
  const validationError = prompt ? validatePrompt(prompt) : "";

  function handleChange(event) {
    setPrompt(clampText(event.target.value, FIELD_LIMITS.PROMPT_MAX));
  }

  return (
    <section className="panel prompt-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Generador IA</p>
          <h2>Describe el postre que quieres crear</h2>
        </div>
        <div className="round-icon">
          <Wand2 size={22} />
        </div>
      </div>

      <form onSubmit={onSubmit} className="prompt-form">
        <textarea
          value={prompt}
          onChange={handleChange}
          placeholder="Ejemplo: milhojas con una rosa azul encima..."
          rows={5}
          maxLength={FIELD_LIMITS.PROMPT_MAX}
          aria-describedby="prompt-counter prompt-error"
          aria-invalid={Boolean(validationError)}
        />

        <div className="field-meta" id="prompt-counter">
          <span className="field-hint">Solo postres, toppings y repostería.</span>
          <span className="char-counter">
            {promptLength}/{FIELD_LIMITS.PROMPT_MAX}
          </span>
        </div>

        {validationError && (
          <div className="error-box" id="prompt-error" role="alert">
            {validationError}
          </div>
        )}

        <button className="primary-button" type="submit" disabled={loading || Boolean(validationError) || !prompt.trim()}>
          {loading ? "Pensando receta..." : "Generar receta y escena"}
        </button>
      </form>

      <div className="example-list">
        {EXAMPLES.map((example) => (
          <button key={example} type="button" onClick={() => setPrompt(clampText(example, FIELD_LIMITS.PROMPT_MAX))}>
            {example}
          </button>
        ))}
      </div>
    </section>
  );
}