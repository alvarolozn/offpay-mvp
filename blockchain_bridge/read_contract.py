import json
import os
from pathlib import Path

from dotenv import load_dotenv
from web3 import Web3


def main() -> None:
    load_dotenv()

    rpc_url = os.getenv("POLYGON_AMOY_RPC_URL")
    chain_id = os.getenv("POLYGON_AMOY_CHAIN_ID")
    contract_address = os.getenv("CONTRACT_ADDRESS")

    if not rpc_url:
        raise ValueError("Falta POLYGON_AMOY_RPC_URL en el .env")

    if not chain_id:
        raise ValueError("Falta POLYGON_AMOY_CHAIN_ID en el .env")

    if not contract_address:
        raise ValueError("Falta CONTRACT_ADDRESS en el .env")

    w3 = Web3(Web3.HTTPProvider(rpc_url))

    print("=== PRUEBA DE CONEXIÓN OFFPAY ===")
    print(f"RPC URL: {rpc_url}")
    print(f"Chain ID esperado: {chain_id}")
    print(f"Conectado: {w3.is_connected()}")

    if not w3.is_connected():
        raise ConnectionError("No se pudo conectar al RPC de Polygon Amoy")

    real_chain_id = w3.eth.chain_id
    print(f"Chain ID real: {real_chain_id}")

    if str(real_chain_id) != str(chain_id):
        raise ValueError(
            f"Chain ID incorrecto. Esperado: {chain_id}, real: {real_chain_id}"
        )

    abi_path = Path("abi") / "OffPayTokenRegistry.json"

    if not abi_path.exists():
        raise FileNotFoundError(
            "No existe abi/OffPayTokenRegistry.json"
        )

    with abi_path.open("r", encoding="utf-8") as f:
        abi = json.load(f)

    checksum_address = Web3.to_checksum_address(contract_address)
    contract = w3.eth.contract(address=checksum_address, abi=abi)

    print(f"Contrato: {checksum_address}")

    hash_1 = "0x1111111111111111111111111111111111111111111111111111111111111111"
    hash_2 = "0x2222222222222222222222222222222222222222222222222222222222222222"

    state_1 = contract.functions.getTokenState(hash_1).call()
    state_2 = contract.functions.getTokenState(hash_2).call()

    print("\n=== RESULTADOS ===")
    print(f"Estado hash 1: {state_1}")
    print(f"Estado hash 2: {state_2}")

    print("\n=== INTERPRETACIÓN ===")
    print("0 = NONE")
    print("1 = AVAILABLE")
    print("2 = USED")
    print("3 = RETURNED")


if __name__ == "__main__":
    main()