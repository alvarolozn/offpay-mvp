import "dotenv/config";
import { readFileSync, writeFileSync, mkdirSync } from "fs";
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

const abiDir = path.join(projectRoot, "abi");
const abiOutputPath = path.join(abiDir, "OffPayTokenRegistry.abi.json");
const deploymentOutputPath = path.join(abiDir, "deployment-info.json");

async function main() {
  const rpcUrl = process.env.BLOCKCHAIN_RPC_URL;
  const privateKey = process.env.DEPLOYER_PRIVATE_KEY;
  const expectedChainIdRaw = process.env.BLOCKCHAIN_EXPECTED_CHAIN_ID;

  if (!rpcUrl) {
    throw new Error("BLOCKCHAIN_RPC_URL no está configurada en .env");
  }

  if (!privateKey) {
    throw new Error("DEPLOYER_PRIVATE_KEY no está configurada en .env");
  }

  const expectedChainId = expectedChainIdRaw
    ? Number(expectedChainIdRaw)
    : null;

  const artifact = JSON.parse(readFileSync(artifactPath, "utf8"));

  const provider = new ethers.JsonRpcProvider(rpcUrl);
  const wallet = new ethers.Wallet(privateKey, provider);

  const network = await provider.getNetwork();
  const chainId = Number(network.chainId);
  const latestBlock = await provider.getBlockNumber();
  const balanceWei = await provider.getBalance(wallet.address);

  console.log("Conexion RPC OK");
  console.log("Wallet deployer:", wallet.address);
  console.log("Chain ID:", chainId);
  console.log("Expected Chain ID:", expectedChainId);
  console.log("Latest block:", latestBlock);
  console.log("Balance:", ethers.formatEther(balanceWei));

  if (expectedChainId !== null && chainId !== expectedChainId) {
    throw new Error("El chain_id no coincide con el esperado");
  }

  if (balanceWei === 0n) {
    throw new Error("La wallet no tiene fondos para pagar gas");
  }

  const factory = new ethers.ContractFactory(
    artifact.abi,
    artifact.bytecode,
    wallet
  );

  console.log("");
  console.log("Desplegando OffPayTokenRegistry en Polygon Amoy...");

  const contract = await factory.deploy();
  const deployTx = contract.deploymentTransaction();

  console.log("Deploy tx hash:", deployTx.hash);

  await contract.waitForDeployment();

  const contractAddress = await contract.getAddress();
  const receipt = await deployTx.wait();

  console.log("Contrato desplegado correctamente");
  console.log("Contract address:", contractAddress);
  console.log("Block number:", receipt.blockNumber);

  mkdirSync(abiDir, { recursive: true });

  writeFileSync(
    abiOutputPath,
    JSON.stringify(artifact.abi, null, 2),
    "utf8"
  );

  writeFileSync(
    deploymentOutputPath,
    JSON.stringify(
      {
        contractName: "OffPayTokenRegistry",
        contractAddress,
        deployTxHash: deployTx.hash,
        blockNumber: receipt.blockNumber,
        chainId,
        deployer: wallet.address,
        deployedAt: new Date().toISOString()
      },
      null,
      2
    ),
    "utf8"
  );

  console.log("");
  console.log("ABI guardado en:", abiOutputPath);
  console.log("Deployment info guardado en:", deploymentOutputPath);
  console.log("");
  console.log("IMPORTANTE: copia esta direccion en tu .env como CONTRACT_ADDRESS:");
  console.log("CONTRACT_ADDRESS=" + contractAddress);
}

main().catch((error) => {
  console.error("ERROR DESPLEGANDO CONTRATO:", error.message);
  process.exit(1);
});