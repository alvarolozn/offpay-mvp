import os
from dotenv import load_dotenv


load_dotenv()


POLYGON_AMOY_RPC_URL = os.getenv("POLYGON_AMOY_RPC_URL")
POLYGON_AMOY_CHAIN_ID = os.getenv("POLYGON_AMOY_CHAIN_ID")
WALLET_ADDRESS = os.getenv("WALLET_ADDRESS")
WALLET_PRIVATE_KEY = os.getenv("WALLET_PRIVATE_KEY")
CONTRACT_ADDRESS = os.getenv("CONTRACT_ADDRESS")


def validate_config() -> None:
    missing = []

    if not POLYGON_AMOY_RPC_URL:
        missing.append("POLYGON_AMOY_RPC_URL")

    if not POLYGON_AMOY_CHAIN_ID:
        missing.append("POLYGON_AMOY_CHAIN_ID")

    if not WALLET_ADDRESS:
        missing.append("WALLET_ADDRESS")

    if not WALLET_PRIVATE_KEY:
        missing.append("WALLET_PRIVATE_KEY")

    if not CONTRACT_ADDRESS:
        missing.append("CONTRACT_ADDRESS")

    if missing:
        raise ValueError(
            "Faltan variables en el .env: " + ", ".join(missing)
        )