from offpay_chain_client import (
    check_blockchain_health,
    get_owner,
    get_token_status,
)

TEST_TOKEN_HASH = "0x3cb59f04d16756fc147bfb7f5aec7c720280ef1cf008a54291afc90c9ec512e6"

print("=== HEALTH ===")
print(check_blockchain_health())

print("")
print("=== OWNER ===")
print(get_owner())

print("")
print("=== TOKEN STATUS ===")
print(get_token_status(TEST_TOKEN_HASH))