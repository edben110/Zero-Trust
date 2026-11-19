export type Candidato = {
  candidateId: string;
  name: string;
  list: string;
};

export type Catalogo = {
  candidates: Candidato[];
  regions: string[];
};

export type SolicitudVoto = {
  voterId: string;
  candidateId: string;
  region: string;
};

export type Comprobante = {
  voteId: string;
  receivedAt: string;
};

export type ResultadoCandidato = {
  candidateId: string;
  candidateName: string;
  voteCount: number;
};

export type ResultadosGlobales = {
  timestamp: string;
  results: ResultadoCandidato[];
};

export type ResultadosRegionales = ResultadosGlobales & {
  region: string;
};

export type ResultadoVoto =
  | { tipo: "aceptado"; comprobante: Comprobante }
  | { tipo: "invalido"; campos: string[] }
  | { tipo: "no-habilitado" }
  | { tipo: "ya-voto" }
  | { tipo: "region-incorrecta" }
  | { tipo: "anulado" }
  | { tipo: "no-disponible" }
  | { tipo: "error" };

// POST /voter-id: la cédula va en el cuerpo y nunca en la URL; la respuesta es el código para votar.
export type ResultadoConsulta =
  | { tipo: "encontrado"; voterId: string }
  | { tipo: "no-registrado" }
  | { tipo: "invalido" }
  | { tipo: "no-disponible" }
  | { tipo: "error" };

export type EstadoConexion = "conectando" | "en-vivo" | "reconectando" | "sin-conexion";
