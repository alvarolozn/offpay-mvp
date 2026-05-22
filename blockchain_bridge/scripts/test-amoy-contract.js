import "dotenv/config";
import { readFileSync } from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { ethers } from "ethers";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, "..");

const abiPath = path.join(
  projectRoot,
  "abi",
  "OffPayTokenRegistry.abi.json"
);

function statusName(value) {
  const names = ["NONE", "AVAILABLE", "USED", "RETURNED"];
  return names[Number(value)] ?? `UNKNOWN_${value}`;
}

async function main() {
  const rpcUrl = process.env.BLOCKCHAIN_RPC_URL;
  const privateKey = process.env.DEPLOYER_PRIVATE_KEY;
  const contractAddress = process.env.CONTRACT_ADDRESS;

  if (!rpcUrl) throw new Error("BLOCKCHAIN_RPC_URL no está configurada");
  if (!privateKey) throw new Error("DEPLOYER_PRIVATE_KEY no está configurada");
  if (!contractAddress) throw new Error("CONTRACT_ADDRESS no está configurada");

  const abi = JSON.parse(readFileSync(abiPath, "utf8"));

  const provider = new ethers.JsonRpcProvider(rpcUrl);
  const wallet = new ethers.Wallet(privateKey, provider);
  const contract = new ethers.Contract(contractAddress, abi, wallet);

  const network = await provider.getNetwork();

  console.log("Conexion RPC OK");
  console.log("Chain ID:", Number(network.chainId));
  console.log("Wallet:", wallet.address);
  console.log("Contrato:", contractAddress);

  const owner = await contract.owner();
  console.log("Owner del contrato:", owner);

  if (owner.toLowerCase() !== wallet.address.toLowerCase()) {
    console.log("ADVERTENCIA: Esta wallet no es owner del contrato");
  }

  const tokenHash = ethers.hexlify(ethers.randomBytes(32));

  console.log("");
  console.log("Token hash de prueba:", tokenHash);

  let status = await contract.getTokenStatusNumber(tokenHash);
  console.log("Estado inicial:", statusName(status));

  console.log("");
  console.log("Registrando token en blockchain real...");
  let tx = await contract.registerToken(tokenHash);
  console.log("registerToken tx:", tx.hash);
  await tx.wait();

  status = await contract.getTokenStatusNumber(tokenHash);
  console.log("Estado despues de registrar:", statusName(status));

  console.log("");
  console.log("Marcando token como USED en blockchain real...");
  tx = await contract.markTokenUsed(tokenHash);
  console.log("markTokenUsed tx:", tx.hash);
  await tx.wait();

  status = await contract.getTokenStatusNumber(tokenHash);
  console.log("Estado final:", statusName(status));

  console.log("");
  console.log("PRUEBA EN AMOY COMPLETADA CORRECTAMENTE");
}

main().catch((error) => {
  console.error("ERROR:", error.message);
  process.exit(1);
});