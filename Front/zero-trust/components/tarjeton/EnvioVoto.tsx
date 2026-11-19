type Props = { habilitada: boolean; rechazo: string | null; enviando: boolean };

export function EnvioVoto({ habilitada, rechazo, enviando }: Props) {
  return (
    <div className="grid gap-4">
      {!habilitada && <p role="status" className="border-l-4 border-sello bg-papel-hondo px-4 py-3">
        La votación aún no está habilitada: se requieren al menos dos candidatos.
      </p>}
      <div aria-live="assertive">
        {rechazo && (
          <p role="alert" className="border-l-4 border-sello bg-papel-hondo px-4 py-3 text-sm">
            {rechazo}
          </p>
        )}
      </div>
      <button
        type="submit"
        disabled={enviando || !habilitada}
        className="justify-self-start border-2 border-tinta bg-tinta px-6 py-3 font-medium text-papel hover:bg-papel hover:text-tinta disabled:cursor-wait disabled:opacity-60"
      >
        {enviando ? "Enviando su voto…" : "Depositar voto"}
      </button>
    </div>
  );
}
