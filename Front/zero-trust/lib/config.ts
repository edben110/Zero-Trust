// Next.js solo sustituye process.env.NEXT_PUBLIC_* cuando se escribe literal; por eso cada variable va por separado.
export const config = {
  votingApiUrl: (process.env.NEXT_PUBLIC_VOTING_API_URL ?? "").replace(/\/+$/, ""),
  wsGlobalUrl: process.env.NEXT_PUBLIC_WS_GLOBAL_URL ?? "",
  wsRegionalUrl: process.env.NEXT_PUBLIC_WS_REGIONAL_URL ?? "",
  demo: process.env.NEXT_PUBLIC_DEMO === "1",
} as const;
