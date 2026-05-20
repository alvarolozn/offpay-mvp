import json
import os
import urllib.request
from typing import Any

from dotenv import load_dotenv


load_dotenv()


RPC_URL = os.getenv("POLYGON_AMOY_RPC_URL")
CHAIN_ID_EXPECTED = os.getenv("POLYGON_AMOY_CHAIN_ID")


class ManualRPCError(Exception):
    pass


def rpc_call(method: str, params: list[Any]) -> Any:
    if not RPC_URL:
        raise ValueError("Falta POLYGON_AMOY_RPC_URL en el .env")

    payload = {
        "jsonrpc": "2.0",
        "method": method,
        "params": params,
        "id": 1,
    }

    data = json.dumps(payload).encode("utf-8")

    req = urllib.request.Request(
        RPC_URL,
        data=data,
        headers={"Content-Type": "application/json"},
    )

    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            raw = response.read().decode("utf-8")
    except Exception as e:
        raise ManualRPCError(f"Fallo RPC en {method}: {repr(e)}") from e

    try:
        parsed = json.loads(raw)
    except json.JSONDecodeError as e:
        raise ManualRPCError(f"Respuesta no JSON en {method}: {raw}") from e

    if "error" in parsed:
        raise ManualRPCError(f"RPC devolvió error en {method}: {parsed['error']}")

    if "result" not in parsed:
        raise ManualRPCError(f"RPC sin result en {method}: {parsed}")

    return parsed["result"]


def get_chain_id() -> int:
    result = rpc_call("eth_chainId", [])
    return int(result, 16)


def get_block_number() -> int:
    result = rpc_call("eth_blockNumber", [])
    return int(result, 16)


def get_transaction_count(address: str) -> int:
    result = rpc_call("eth_getTransactionCount", [address, "latest"])
    return int(result, 16)


def validate_chain() -> None:
    real_chain_id = get_chain_id()

    if not CHAIN_ID_EXPECTED:
        raise ValueError("Falta POLYGON_AMOY_CHAIN_ID en el .env")

    if str(real_chain_id) != str(CHAIN_ID_EXPECTED):
        raise ValueError(
            f"Chain ID incorrecto. Esperado: {CHAIN_ID_EXPECTED}, real: {real_chain_id}"
        )