import os
from dotenv import load_dotenv

from manual_rpc import get_block_number, get_chain_id, get_transaction_count, validate_chain


def main() -> None:
    load_dotenv()

    wallet_address = os.getenv("WALLET_ADDRESS")
    if not wallet_address:
        raise ValueError("Falta WALLET_ADDRESS en el .env")

    print("=== TEST MANUAL RPC ===")

    validate_chain()

    chain_id = get_chain_id()
    block_number = get_block_number()
    nonce = get_transaction_count(wallet_address)

    print(f"Chain ID: {chain_id}")
    print(f"Último bloque: {block_number}")
    print(f"Nonce wallet: {nonce}")


if __name__ == "__main__":
    main()