import { Wand2 } from "lucide-react";

const EXAMPLES = [
  "Quiero una milhojas con una rosa azul encima",
  "Hazme un pastel de vainilla con fresas arriba",
  "Quiero un cupcake elegante con crema y una vela",
  "Crea un pastel redondo con decoración de chocolate",
];

export default function PromptComposer({ prompt, setPrompt, onSubmit, loading }) {
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
          onChange={(event) => setPrompt(event.target.value)}
          placeholder="Ejemplo: quiero una milhojas con una rosa azul encima..."
          rows={5}
        />

        <button className="primary-button" type="submit" disabled={loading || !prompt.trim()}>
          {loading ? "Pensando receta..." : "Generar receta y escena"}
        </button>
      </form>

      <div className="example-list">
        {EXAMPLES.map((example) => (
          <button key={example} type="button" onClick={() => setPrompt(example)}>
            {example}
          </button>
        ))}
      </div>
    </section>
  );
}