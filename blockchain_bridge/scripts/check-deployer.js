import "dotenv/config";
import { ethers } from "ethers";

async function main() {
  const rpcUrl = process.env.BLOCKCHAIN_RPC_URL;
  const expectedChainIdRaw = process.env.BLOCKCHAIN_EXPECTED_CHAIN_ID;
  const privateKey = process.env.DEPLOYER_PRIVATE_KEY;

  if (!rpcUrl) {
    throw new Error("BLOCKCHAIN_RPC_URL no está configurada en .env");
  }

  if (!privateKey) {
    throw new Error("DEPLOYER_PRIVATE_KEY no está configurada en .env");
  }

  const provider = new ethers.JsonRpcProvider(rpcUrl);
  const wallet = new ethers.Wallet(privateKey, provider);

  const network = await provider.getNetwork();
  const chainId = Number(network.chainId);
  const latestBlock = await provider.getBlockNumber();

  const expectedChainId = expectedChainIdRaw
    ? Number(expectedChainIdRaw)
    : null;

  const balanceWei = await provider.getBalance(wallet.address);
  const balanceNative = ethers.formatEther(balanceWei);

  console.log("Conexion RPC OK");
  console.log("Wallet deployer:", wallet.address);
  console.log("Chain ID:", chainId);
  console.log("Expected Chain ID:", expectedChainId);
  console.log("Latest block:", latestBlock);
  console.log("Balance:", balanceNative);

  if (expectedChainId !== null && chainId !== expectedChainId) {
    throw new Error("El chain_id no coincide con el esperado");
  }

  if (balanceWei === 0n) {
    console.log("");
    console.log("ADVERTENCIA: La wallet no tiene fondos de prueba.");
    console.log("Necesitas POL/MATIC de prueba en Polygon Amoy antes de desplegar.");
    return;
  }

  console.log("");
  console.log("Wallet lista para desplegar contrato en testnet.");
}

main().catch((error) => {
  console.error("ERROR:", error.message);
  process.exit(1);
});