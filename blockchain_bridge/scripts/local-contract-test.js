import { readFileSync } from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { ethers } from "ethers";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, "..");

const artifactPath = path.join(
  projectRoot,
  "artifacts",
  "contracts",
  "OffPayTokenRegistry.sol",
  "OffPayTokenRegistry.json"
);

const artifact = JSON.parse(readFileSync(artifactPath, "utf8"));
const LOCAL_RPC_URL = "http://127.0.0.1:8545";

function statusName(value) {
  const names = ["NONE", "AVAILABLE", "USED", "RETURNED"];
  return names[Number(value)] ?? `UNKNOWN_${value}`;
}

async function main() {
  console.log("Conectando a nodo local:", LOCAL_RPC_URL);

  const provider = new ethers.JsonRpcProvider(LOCAL_RPC_URL);
  const signer = await provider.getSigner(0);

  console.log("Cuenta usada:", await signer.getAddress());

  const factory = new ethers.ContractFactory(
    artifact.abi,
    artifact.bytecode,
    signer
  );

  console.log("Desplegando contrato local...");
  const contract = await factory.deploy();
  await contract.waitForDeployment();

  console.log("Contrato desplegado en:", await contract.getAddress());

  const tokenHashUsed =
    "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

  const tokenHashReturned =
    "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

  console.log("\n=== PRUEBA TOKEN USADO ===");

  let status = await contract.getTokenStatusNumber(tokenHashUsed);
  console.log("Estado inicial:", statusName(status));

  let tx = await contract.registerToken(tokenHashUsed);
  console.log("registerToken tx:", tx.hash);
  await tx.wait();

  status = await contract.getTokenStatusNumber(tokenHashUsed);
  console.log("Estado despues de registrar:", statusName(status));

  tx = await contract.markTokenUsed(tokenHashUsed);
  console.log("markTokenUsed tx:", tx.hash);
  await tx.wait();

  status = await contract.getTokenStatusNumber(tokenHashUsed);
  console.log("Estado despues de usar:", statusName(status));

  console.log("\nIntentando devolver token ya usado...");
  try {
    tx = await contract.markTokenReturned(tokenHashUsed);
    await tx.wait();
    console.log("ERROR: El contrato permitio devolver un token usado");
  } catch {
    console.log("Rechazo correcto: no se puede devolver un token USED");
  }

  console.log("\n=== PRUEBA TOKEN DEVUELTO ===");

  status = await contract.getTokenStatusNumber(tokenHashReturned);
  console.log("Estado inicial:", statusName(status));

  tx = await contract.registerToken(tokenHashReturned);
  console.log("registerToken tx:", tx.hash);
  await tx.wait();

  status = await contract.getTokenStatusNumber(tokenHashReturned);
  console.log("Estado despues de registrar:", statusName(status));

  tx = await contract.markTokenReturned(tokenHashReturned);
  console.log("markTokenReturned tx:", tx.hash);
  await tx.wait();

  status = await contract.getTokenStatusNumber(tokenHashReturned);
  console.log("Estado despues de devolver:", statusName(status));

  console.log("\nPRUEBA LOCAL COMPLETADA CORRECTAMENTE");
}

main().catch((error) => {
  console.error("ERROR EN PRUEBA LOCAL:", error.message);
  process.exit(1);
});