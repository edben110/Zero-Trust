import Link from "next/link";

type Props = { valor: string; error?: string; alCambiar: (valor: string) => void };

export function CampoCodigo({ valor, error, alCambiar }: Props) {
  return (
    <fieldset>
      <legend className="font-serif text-xl">1. Su código de votante</legend>
      <label htmlFor="voterId" className="mt-3 block text-sm text-tinta-suave">
        Si no lo tiene, obténgalo con su cédula en{" "}
        <Link href="/mi-codigo" className="underline hover:text-tinta">
          Mi código de votante
        </Link>
        .
      </label>
      <input
        id="voterId"
        name="voterId"
        value={valor}
        onChange={(evento) => alCambiar(evento.target.value)}
        autoComplete="off"
        spellCheck={false}
        aria-invalid={error !== undefined}
        aria-describedby={error ? "error-voterId" : undefined}
        placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
        className="mt-2 w-full border-b-2 border-tinta bg-transparent px-1 py-2 font-mono text-base placeholder:text-tinta/30 focus:bg-papel-hondo focus:outline-none"
      />
      {error && (
        <p id="error-voterId" className="mt-2 text-sm text-sello">
          {error}
        </p>
      )}
    </fieldset>
  );
}
