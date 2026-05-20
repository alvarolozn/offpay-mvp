from contract_client import OffPayContractClient


def main() -> None:
    client = OffPayContractClient()

    hash_1 = "0x1111111111111111111111111111111111111111111111111111111111111111"
    hash_2 = "0x2222222222222222222222222222222222222222222222222222222222222222"
    hash_3 = "0x3333333333333333333333333333333333333333333333333333333333333333"

    print("=== TEST CLIENT READ ===")
    print(f"Conectado: {client.is_connected()}")
    print(f"Chain ID: {client.get_chain_id()}")

    print(f"Estado hash 1: {client.get_token_state(hash_1)}")
    print(f"Estado hash 2: {client.get_token_state(hash_2)}")
    print(f"Estado hash 3: {client.get_token_state(hash_3)}")

    print("\nInterpretación:")
    print("0 = NONE")
    print("1 = AVAILABLE")
    print("2 = USED")
    print("3 = RETURNED")


if __name__ == "__main__":
    main()