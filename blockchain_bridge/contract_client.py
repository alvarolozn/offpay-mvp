import json
from pathlib import Path
from typing import List

import requests
from web3 import Web3

from config import (
    CONTRACT_ADDRESS,
    POLYGON_AMOY_CHAIN_ID,
    POLYGON_AMOY_RPC_URL,
    WALLET_ADDRESS,
    WALLET_PRIVATE_KEY,
    validate_config,
)


class OffPayContractClient:
    def __init__(self) -> None:
        validate_config()

        # Sesión limpia: evita heredar proxies del sistema
        session = requests.Session()
        session.trust_env = False

        self.w3 = Web3(
            Web3.HTTPProvider(
                POLYGON_AMOY_RPC_URL,
                session=session,
                request_kwargs={"timeout": 20},
            )
        )

        try:
            real_chain_id = self.w3.eth.chain_id
        except Exception as e:
            raise ConnectionError(
                f"No se pudo consultar chain_id en el RPC de Polygon Amoy: {repr(e)}"
            )

        if str(real_chain_id) != str(POLYGON_AMOY_CHAIN_ID):
            raise ValueError(
                f"Chain ID incorrecto. Esperado: {POLYGON_AMOY_CHAIN_ID}, real: {real_chain_id}"
            )

        abi_path = Path("abi") / "OffPayTokenRegistry.json"
        if not abi_path.exists():
            raise FileNotFoundError("No existe abi/OffPayTokenRegistry.json")

        with abi_path.open("r", encoding="utf-8") as f:
            abi = json.load(f)

        self.wallet_address = Web3.to_checksum_address(WALLET_ADDRESS)
        self.contract_address = Web3.to_checksum_address(CONTRACT_ADDRESS)
        self.private_key = WALLET_PRIVATE_KEY

        self.contract = self.w3.eth.contract(
            address=self.contract_address,
            abi=abi,
        )

    def _to_bytes32(self, token_hash: str) -> bytes:
        if not token_hash:
            raise ValueError("token_hash vacío")

        token_hash = token_hash.strip()

        if token_hash.startswith("0x"):
            stripped = token_hash[2:]
        else:
            stripped = token_hash

        if len(stripped) != 64:
            raise ValueError(
                f"token_hash inválido para bytes32. Longitud hex recibida: {len(stripped)}"
            )

        try:
            return bytes.fromhex(stripped)
        except ValueError as e:
            raise ValueError(f"token_hash no es hex válido: {token_hash}") from e

    def _to_bytes32_list(self, token_hashes: List[str]) -> List[bytes]:
        return [self._to_bytes32(token_hash) for token_hash in token_hashes]

    def is_connected(self) -> bool:
        try:
            _ = self.w3.eth.chain_id
            return True
        except Exception:
            return False

    def get_chain_id(self) -> int:
        return self.w3.eth.chain_id

    def get_token_state(self, token_hash: str) -> int:
        token_hash_bytes = self._to_bytes32(token_hash)
        return self.contract.functions.getTokenState(token_hash_bytes).call()

    def _send_transaction(self, tx_function) -> dict:
        nonce = self.w3.eth.get_transaction_count(self.wallet_address)
        gas_price = self.w3.eth.gas_price

        tx = tx_function.build_transaction(
            {
                "from": self.wallet_address,
                "chainId": int(POLYGON_AMOY_CHAIN_ID),
                "nonce": nonce,
                "gas": 300000,
                "gasPrice": gas_price,
            }
        )

        signed_tx = self.w3.eth.account.sign_transaction(
            tx,
            private_key=self.private_key,
        )

        tx_hash = self.w3.eth.send_raw_transaction(signed_tx.raw_transaction)
        receipt = self.w3.eth.wait_for_transaction_receipt(tx_hash)

        return {
            "tx_hash": tx_hash.hex(),
            "status": receipt.status,
            "block_number": receipt.blockNumber,
            "gas_used": receipt.gasUsed,
        }

    def register_tokens(self, token_hashes: List[str]) -> dict:
        if not token_hashes:
            raise ValueError("La lista de token_hashes no puede estar vacía")

        token_hashes_bytes = self._to_bytes32_list(token_hashes)
        tx_function = self.contract.functions.registerTokens(token_hashes_bytes)
        return self._send_transaction(tx_function)

    def use_tokens(self, token_hashes: List[str]) -> dict:
        if not token_hashes:
            raise ValueError("La lista de token_hashes no puede estar vacía")

        token_hashes_bytes = self._to_bytes32_list(token_hashes)
        tx_function = self.contract.functions.useTokens(token_hashes_bytes)
        return self._send_transaction(tx_function)

    def return_tokens(self, token_hashes: List[str]) -> dict:
        if not token_hashes:
            raise ValueError("La lista de token_hashes no puede estar vacía")

        token_hashes_bytes = self._to_bytes32_list(token_hashes)
        tx_function = self.contract.functions.returnTokens(token_hashes_bytes)
        return self._send_transaction(tx_function)