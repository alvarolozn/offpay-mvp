import os
from dotenv import load_dotenv

load_dotenv()

print("=== DEBUG ENV ===")
print("POLYGON_AMOY_RPC_URL =", repr(os.getenv("POLYGON_AMOY_RPC_URL")))
print("POLYGON_AMOY_CHAIN_ID =", repr(os.getenv("POLYGON_AMOY_CHAIN_ID")))
print("WALLET_ADDRESS =", repr(os.getenv("WALLET_ADDRESS")))
print("CONTRACT_ADDRESS =", repr(os.getenv("CONTRACT_ADDRESS")))
print("DATABASE_URL existe =", os.getenv("DATABASE_URL") is not None)