import { boton, campo } from "@/lib/estilos";

type Props = {
  editando: string | null;
  nombre: string;
  lista: string;
  ocupado: boolean;
  aviso: string;
  setNombre: (valor: string) => void;
  setLista: (valor: string) => void;
  onGuardar: () => void;
  onCancelar: () => void;
};

export function FormularioCandidato(props: Props) {
  const { editando, nombre, lista, ocupado, aviso, setNombre, setLista, onGuardar, onCancelar } = props;
  return (
    <form onSubmit={(e) => { e.preventDefault(); onGuardar(); }}
      className="h-fit border border-tinta p-6">
      <h2 className="font-serif text-2xl">{editando ? "Editar candidato" : "Agregar candidato"}</h2>
      <label className="mt-5 block text-sm font-medium">Nombre
        <input className={`${campo} mt-2`} maxLength={120} value={nombre} onChange={(e) => setNombre(e.target.value)} required />
      </label>
      <label className="mt-5 block text-sm font-medium">Lista o partido
        <input className={`${campo} mt-2`} maxLength={120} value={lista} onChange={(e) => setLista(e.target.value)} required />
      </label>
      <button className={`${boton} mt-6`} disabled={ocupado}>{editando ? "Guardar cambios" : "Crear candidato"}</button>
      {editando && <button type="button" onClick={onCancelar}
        className="ml-3 text-sm underline">Cancelar</button>}
      {aviso && <p role="status" className="mt-4 text-sello">{aviso}</p>}
    </form>
  );
}
