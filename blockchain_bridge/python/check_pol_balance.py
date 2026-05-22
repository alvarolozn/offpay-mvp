from web3 import Web3
from dotenv import load_dotenv
import os

load_dotenv()

RPC_URL = os.getenv("BLOCKCHAIN_RPC_URL", "https://rpc-amoy.polygon.technology")

WALLET_ADDRESS = "0x97c90b56C5d485996aD4C9870E38470aa644fec8"

web3 = Web3(Web3.HTTPProvider(RPC_URL))

balance_wei = web3.eth.get_balance(WALLET_ADDRESS)
balance_pol = web3.from_wei(balance_wei, "ether")

print("Wallet:", WALLET_ADDRESS)
print("Balance POL:", balance_pol)
