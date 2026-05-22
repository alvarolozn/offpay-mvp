import "dotenv/config";
import { ethers } from "ethers";

async function main() {
  const rpcUrl = process.env.BLOCKCHAIN_RPC_URL;
  const expectedChainIdRaw = process.env.BLOCKCHAIN_EXPECTED_CHAIN_ID;

  if (!rpcUrl) {
    throw new Error("BLOCKCHAIN_RPC_URL no está configurada en .env");
  }

  const provider = new ethers.JsonRpcProvider(rpcUrl);

  const network = await provider.getNetwork();
  const latestBlock = await provider.getBlockNumber();

  const chainId = Number(network.chainId);
  const expectedChainId = expectedChainIdRaw
    ? Number(expectedChainIdRaw)
    : null;

  console.log("Conexion RPC OK");
  console.log("Chain ID:", chainId);
  console.log("Expected Chain ID:", expectedChainId);
  console.log("Latest block:", latestBlock);

  if (expectedChainId !== null && chainId !== expectedChainId) {
    console.log("ADVERTENCIA: El chain_id no coincide con el esperado");
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error("Error verificando red:", error.message);
  process.exit(1);
});
