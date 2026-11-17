# Mesa de votación (front)

Interfaz web de la plataforma de votación Zero Trust:

- `/` — Tarjetón: código de votante, región y candidato. Envía `POST /vote` a voting_api y muestra el comprobante.
- `/resultados` — Escrutinio nacional en vivo (STOMP `/topic/global` del global_dashboard_worker).
- `/resultados/regiones` — Escrutinio por región (STOMP `/topic/region/<región>` del regional_dashboard_worker).

Next.js 16, React 19 y Tailwind 4. Los contratos están en `docs/contratos.md` del repositorio.

## Variables

Copie `.env.example` a `.env.local` y ajuste los valores. Las variables `NEXT_PUBLIC_*` se fijan al ejecutar `next build`.

| Variable | Uso |
|---|---|
| `NEXT_PUBLIC_VOTING_API_URL` | URL base de voting_api |
| `NEXT_PUBLIC_WS_GLOBAL_URL` | STOMP del global_dashboard_worker (`ws://…/ws/results/websocket`) |
| `NEXT_PUBLIC_WS_REGIONAL_URL` | STOMP del regional_dashboard_worker |
| `NEXT_PUBLIC_DEMO` | `1` para usar datos simulados sin backend (siempre con el aviso "Datos simulados") |

## Ejecutar

```bash
npm install
npm run dev          # desarrollo en http://localhost:3000
npm run lint
npm run build && npm start
```

Modo demostración: `NEXT_PUBLIC_DEMO=1 npm run dev`.
