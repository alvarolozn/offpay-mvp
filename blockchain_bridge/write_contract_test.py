import json
import os
from pathlib import Path

from dotenv import load_dotenv
from web3 import Web3


def main() -> None:
    load_dotenv()

    rpc_url = os.getenv("POLYGON_AMOY_RPC_URL")
    chain_id = os.getenv("POLYGON_AMOY_CHAIN_ID")
    wallet_address = os.getenv("WALLET_ADDRESS")
    private_key = os.getenv("WALLET_PRIVATE_KEY")
    contract_address = os.getenv("CONTRACT_ADDRESS")

    if not rpc_url:
        raise ValueError("Falta POLYGON_AMOY_RPC_URL en el .env")

    if not chain_id:
        raise ValueError("Falta POLYGON_AMOY_CHAIN_ID en el .env")

    if not wallet_address:
        raise ValueError("Falta WALLET_ADDRESS en el .env")

    if not private_key:
        raise ValueError("Falta WALLET_PRIVATE_KEY en el .env")

    if not contract_address:
        raise ValueError("Falta CONTRACT_ADDRESS en el .env")

    w3 = Web3(Web3.HTTPProvider(rpc_url))

    print("=== PRUEBA DE ESCRITURA OFFPAY ===")
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
        raise FileNotFoundError("No existe abi/OffPayTokenRegistry.json")

    with abi_path.open("r", encoding="utf-8") as f:
        abi = json.load(f)

    checksum_wallet = Web3.to_checksum_address(wallet_address)
    checksum_contract = Web3.to_checksum_address(contract_address)

    contract = w3.eth.contract(address=checksum_contract, abi=abi)

    # Hash nuevo de prueba para esta escritura desde Python
    test_hash = "0x3333333333333333333333333333333333333333333333333333333333333333"

    print(f"Wallet: {checksum_wallet}")
    print(f"Contrato: {checksum_contract}")
    print(f"Hash de prueba: {test_hash}")

    # Estado antes
    state_before = contract.functions.getTokenState(test_hash).call()
    print(f"Estado antes: {state_before}")

    if state_before != 0:
        raise ValueError(
            "El hash de prueba ya existe en el contrato. "
            "No vuelvas a correr este script con el mismo hash."
        )

    nonce = w3.eth.get_transaction_count(checksum_wallet)
    gas_price = w3.eth.gas_price

    print(f"Nonce: {nonce}")
    print(f"Gas price: {gas_price}")

    tx = contract.functions.registerTokens([test_hash]).build_transaction(
        {
            "from": checksum_wallet,
            "chainId": int(chain_id),
            "nonce": nonce,
            "gas": 300000,
            "gasPrice": gas_price,
        }
    )

    signed_tx = w3.eth.account.sign_transaction(tx, private_key=private_key)
    tx_hash = w3.eth.send_raw_transaction(signed_tx.raw_transaction)

    print(f"Tx enviada: {tx_hash.hex()}")
    print("Esperando confirmación...")

    receipt = w3.eth.wait_for_transaction_receipt(tx_hash)

    print("=== RECIBO ===")
    print(f"Status: {receipt.status}")
    print(f"Block number: {receipt.blockNumber}")
    print(f"Gas usado: {receipt.gasUsed}")
    print(f"Transaction hash: {tx_hash.hex()}")

    # Estado después
    state_after = contract.functions.getTokenState(test_hash).call()
    print(f"Estado después: {state_after}")

    print("\n=== INTERPRETACIÓN ===")
    print("0 = NONE")
    print("1 = AVAILABLE")
    print("2 = USED")
    print("3 = RETURNED")


if __name__ == "__main__":
    main()