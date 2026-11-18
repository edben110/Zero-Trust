import { randomBytes, scryptSync } from "node:crypto";
import { readFileSync } from "node:fs";

// Leer desde stdin evita poner la contraseña en argv o en el historial del shell.
const password = readFileSync(0, "utf8").replace(/\r?\n$/, "");
if (password.length < 20) {
  console.error("La contraseña administrativa debe tener al menos 20 caracteres.");
  process.exit(1);
}
const salt = randomBytes(16);
const hash = scryptSync(password, salt, 64, { N: 16384, r: 8, p: 1, maxmem: 64 * 1024 * 1024 });
process.stdout.write(`scrypt:${salt.toString("hex")}:${hash.toString("hex")}\n`);
