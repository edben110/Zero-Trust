export function NuevaVotacion({ deshabilitado, onReiniciar }: { deshabilitado: boolean; onReiniciar: () => void }) {
  return (
    <section className="mt-8 border border-sello p-6" aria-label="Nueva votación">
      <h2 className="font-serif text-2xl">Iniciar otra votación</h2>
      <p className="mt-2">Conserva los candidatos y el padrón, reinicia los resultados y habilita un nuevo voto por persona.</p>
      <button type="button" disabled={deshabilitado}
        onClick={onReiniciar}
        className="mt-4 border-2 border-sello px-5 py-2 font-medium text-sello disabled:opacity-50">
        Reiniciar votación
      </button>
    </section>
  );
}
