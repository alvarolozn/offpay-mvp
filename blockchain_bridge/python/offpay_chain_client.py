import json
import os
import re
from pathlib import Path

from dotenv import load_dotenv
from web3 import Web3

try:
    from web3.middleware import ExtraDataToPOAMiddleware
except ImportError:
    ExtraDataToPOAMiddleware = None


BASE_DIR = Path(__file__).resolve().parents[1]
ENV_PATH = BASE_DIR / ".env"
ABI_PATH = BASE_DIR / "abi" / "OffPayTokenRegistry.abi.json"

load_dotenv(ENV_PATH, encoding="utf-8-sig", override=True)

STATUS_NAMES = {
    0: "NONE",
    1: "AVAILABLE",
    2: "USED",
    3: "RETURNED",
}


def _get_env(name: str, required: bool = True):
    value = os.getenv(name)

    if required and not value:
        raise RuntimeError(f"{name} no estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ configurada en {ENV_PATH}")

    return value


def get_web3() -> Web3:
    rpc_url = _get_env("BLOCKCHAIN_RPC_URL")

    web3 = Web3(Web3.HTTPProvider(rpc_url))

    if ExtraDataToPOAMiddleware is not None:
        web3.middleware_onion.inject(ExtraDataToPOAMiddleware, layer=0)

    if not web3.is_connected():
        raise RuntimeError("No se pudo conectar al RPC blockchain")

    return web3


def get_contract():
    contract_address = _get_env("CONTRACT_ADDRESS")

    if not ABI_PATH.exists():
        raise RuntimeError(f"No existe el ABI en {ABI_PATH}")

    with open(ABI_PATH, "r", encoding="utf-8") as f:
        abi = json.load(f)

    web3 = get_web3()
    checksum_address = web3.to_checksum_address(contract_address)

    return web3.eth.contract(
        address=checksum_address,
        abi=abi
    )


def check_blockchain_health() -> dict:
    web3 = get_web3()

    chain_id = web3.eth.chain_id
    latest_block = web3.eth.block_number

    expected_chain_id_raw = os.getenv("BLOCKCHAIN_EXPECTED_CHAIN_ID")
    expected_chain_id = int(expected_chain_id_raw) if expected_chain_id_raw else None

    contract_address = os.getenv("CONTRACT_ADDRESS")

    result = {
        "connected": True,
        "chain_id": chain_id,
        "expected_chain_id": expected_chain_id,
        "latest_block": latest_block,
        "contract_address": contract_address,
    }

    if expected_chain_id is not None and chain_id != expected_chain_id:
        result["status"] = "warning"
        result["message"] = "Conectado, pero el chain_id no coincide con el esperado"
    else:
        result["status"] = "ok"
        result["message"] = "Conexion blockchain correcta"

    return result


def get_owner() -> str:
    contract = get_contract()
    return contract.functions.owner().call()


def normalize_token_hash(token_hash: str) -> str:
    clean = token_hash.strip()

    if not clean.startswith("0x"):
        clean = "0x" + clean

    if len(clean) != 66:
        raise ValueError("token_hash debe tener formato 0x + 64 caracteres hexadecimales")

    if not re.fullmatch(r"0x[0-9a-fA-F]{64}", clean):
        raise ValueError("token_hash no es hexadecimal valido")

    return clean


def get_token_status_number(token_hash: str) -> int:
    token_hash = normalize_token_hash(token_hash)

    contract = get_contract()

    status = contract.functions.getTokenStatusNumber(token_hash).call()

    return int(status)


def get_token_status(token_hash: str) -> dict:
    token_hash = normalize_token_hash(token_hash)

    status_number = get_token_status_number(token_hash)
    status_name = STATUS_NAMES.get(status_number, f"UNKNOWN_{status_number}")

    return {
        "token_hash": token_hash,
        "status_number": status_number,
        "status_name": status_name,
    }


def _get_wallet(web3: Web3):
    private_key = _get_env("DEPLOYER_PRIVATE_KEY")
    return web3.eth.account.from_key(private_key)


def _send_transaction(function_call) -> dict:
    web3 = get_web3()
    wallet = _get_wallet(web3)

    nonce = web3.eth.get_transaction_count(wallet.address, "pending")

    latest_block = web3.eth.get_block("latest")
    base_fee = latest_block.get("baseFeePerGas", web3.eth.gas_price)

    # Polygon Amoy exige tip alto. Antes 2 gwei fallaba.
    priority_fee = web3.to_wei(30, "gwei")
    max_fee_per_gas = int((base_fee * 2) + priority_fee)

    # Gas fijo alto para evitar error:
    # gas required exceeds allowance (19548)
    tx = function_call.build_transaction({
        "from": wallet.address,
        "nonce": nonce,
        "chainId": web3.eth.chain_id,
        "gas": 90000,
        "maxFeePerGas": max_fee_per_gas,
        "maxPriorityFeePerGas": priority_fee,
    })

    signed_tx = web3.eth.account.sign_transaction(
        tx,
        private_key=os.getenv("DEPLOYER_PRIVATE_KEY")
    )

    tx_hash = web3.eth.send_raw_transaction(signed_tx.raw_transaction)
    receipt = web3.eth.wait_for_transaction_receipt(tx_hash)

    return {
        "tx_hash": web3.to_hex(tx_hash),
        "block_number": receipt.blockNumber,
        "status": receipt.status,
        "from": wallet.address,
    }

def register_token(token_hash: str) -> dict:
    token_hash = normalize_token_hash(token_hash)
    contract = get_contract()

    return _send_transaction(
        contract.functions.registerToken(token_hash)
    )


def mark_token_used(token_hash: str) -> dict:
    token_hash = normalize_token_hash(token_hash)
    contract = get_contract()

    return _send_transaction(
        contract.functions.markTokenUsed(token_hash)
    )


def mark_token_returned(token_hash: str) -> dict:
    token_hash = normalize_token_hash(token_hash)
    contract = get_contract()

    return _send_transaction(
        contract.functions.markTokenReturned(token_hash)
    )