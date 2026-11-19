import type { FormEvent } from "react";
import { boton, campo } from "@/lib/estilos";

type Props = {
  usuario: string;
  password: string;
  ocupado: boolean;
  aviso: string;
  setUsuario: (valor: string) => void;
  setPassword: (valor: string) => void;
  onIniciar: (evento: FormEvent) => void;
};

export function AccesoAdmin({ usuario, password, ocupado, aviso, setUsuario, setPassword, onIniciar }: Props) {
  return (
    <form onSubmit={onIniciar} className="mt-10 max-w-md border border-tinta p-6 sm:p-8">
      <h2 className="font-serif text-2xl">Acceso administrativo</h2>
      <p className="mt-3 text-sm text-sello">
        Acceda solo por http://localhost:3001/admin con el túnel SSM abierto.
      </p>
      <label className="mt-6 block text-sm font-medium">Usuario
        <input className={`${campo} mt-2`} autoComplete="username" value={usuario} onChange={(e) => setUsuario(e.target.value)} required />
      </label>
      <label className="mt-5 block text-sm font-medium">Contraseña
        <input className={`${campo} mt-2`} type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
      </label>
      <button className={`${boton} mt-6`} disabled={ocupado}>Ingresar</button>
      {aviso && <p role="alert" className="mt-4 text-sello">{aviso}</p>}
    </form>
  );
}
