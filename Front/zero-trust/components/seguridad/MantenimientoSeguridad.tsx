type Props = {
  ocupado: boolean;
  actualizadoEn: string | null;
  onActualizar: () => void;
  onPurgar: () => void;
};

export function MantenimientoSeguridad({ ocupado, actualizadoEn, onActualizar, onPurgar }: Props) {
  return (
    <div className="mt-6 border-2 border-sello p-4 sm:flex sm:items-center sm:justify-between sm:gap-6">
      <div>
        <h3 className="font-serif text-lg">Mantenimiento</h3>
        <p className="mt-1 max-w-2xl text-sm text-tinta-suave">
          <strong>Borrar incidentes y estados</strong> vacía el histórico de incidentes y deja en cero
          los contadores de las IPs observadas. No borra votos, candidatos ni el padrón.
        </p>
        <p className="mt-1 text-xs text-tinta-suave">
          Última lectura: <span className="cifras">{actualizadoEn ?? "sin datos"}</span>
        </p>
      </div>
      <div className="mt-3 flex flex-wrap items-center gap-3 sm:mt-0 sm:justify-end">
        <button type="button" onClick={onActualizar} disabled={ocupado}
          className="border-2 border-tinta px-5 py-2 font-medium text-tinta hover:bg-tinta hover:text-papel disabled:opacity-50">
          {ocupado ? "Actualizando…" : "Actualizar ahora"}
        </button>
        <button type="button" onClick={onPurgar} disabled={ocupado}
          className="border-2 border-sello bg-sello px-5 py-2 font-medium text-papel hover:bg-papel hover:text-sello disabled:opacity-50">
          Borrar incidentes y estados
        </button>
      </div>
    </div>
  );
}
