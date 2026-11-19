import { nombreRegion } from "@/lib/formato";

type Props = { regiones: string[]; valor: string; error?: string; alElegir: (region: string) => void };

export function SelectorRegion({ regiones, valor, error, alElegir }: Props) {
  return (
    <fieldset aria-describedby={error ? "error-region" : undefined}>
      <legend className="font-serif text-xl">2. Su región</legend>
      <div className="mt-4 flex flex-wrap gap-2">
        {regiones.map((opcion) => (
          <label
            key={opcion}
            className="cursor-pointer border border-linea px-4 py-2 text-sm has-[:checked]:border-tinta has-[:checked]:bg-tinta has-[:checked]:text-papel has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-tinta"
          >
            <input
              type="radio"
              name="region"
              value={opcion}
              checked={valor === opcion}
              onChange={() => alElegir(opcion)}
              className="sr-only"
            />
            {nombreRegion(opcion)}
          </label>
        ))}
      </div>
      {error && (
        <p id="error-region" className="mt-2 text-sm text-sello">
          {error}
        </p>
      )}
    </fieldset>
  );
}
