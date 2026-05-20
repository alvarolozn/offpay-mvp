from contract_client import OffPayContractClient
from db_client import fetch_all


def to_bytes32(token_hash: str) -> bytes:
    stripped = token_hash[2:] if token_hash.startswith("0x") else token_hash
    return bytes.fromhex(stripped)


def main() -> None:
    print("=== DEBUG DE LLAMADAS getTokenState CON TOKENS REALES ===")

    rows = fetch_all(
        """
        SELECT
            id,
            token_hash,
            status
        FROM tokens
        WHERE token_hash IS NOT NULL
          AND token_hash <> ''
          AND register_tx_hash IS NULL
        ORDER BY id ASC
        LIMIT 20;
        """
    )

    print(f"Cantidad encontrada: {len(rows)}")

    if not rows:
        print("No hay tokens pendientes.")
        return

    client = OffPayContractClient()

    for i, row in enumerate(rows, start=1):
        token_id = row["id"]
        token_hash = row["token_hash"]
        status = row["status"]

        print(f"\n--- Token {i} ---")
        print(f"id: {token_id}")
        print(f"status: {status}")
        print(f"token_hash: {token_hash}")
        print(f"repr(token_hash): {repr(token_hash)}")
        print(f"tipo: {type(token_hash)}")
        print(f"longitud: {len(token_hash)}")

        try:
            state = client.get_token_state(token_hash)
            print(f"CALL COMO STRING OK -> state={state}")
        except Exception as e:
            print(f"CALL COMO STRING FALLÓ -> {repr(e)}")

            try:
                state_bytes = client.contract.functions.getTokenState(
                    to_bytes32(token_hash)
                ).call()
                print(f"CALL COMO BYTES OK -> state={state_bytes}")
            except Exception as e2:
                print(f"CALL COMO BYTES FALLÓ -> {repr(e2)}")

            print("\nSe detiene en el primer token que falla.")
            return

    print("\nTodos los tokens consultados respondieron bien.")
    print("En ese caso, el error anterior fue transitorio o vino de otra parte del flujo.")


if __name__ == "__main__":
    main()