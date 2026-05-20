from db_client import fetch_all


def main() -> None:
    print("=== INSPECCIÓN DE TOKEN_HASH PENDIENTES ===")

    rows = fetch_all(
        """
        SELECT
            id,
            token_hash,
            status,
            register_tx_hash
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

    for i, row in enumerate(rows, start=1):
        token_hash = row["token_hash"]

        print(f"\n--- Token {i} ---")
        print(f"id: {row['id']}")
        print(f"status: {row['status']}")
        print(f"token_hash: {token_hash}")
        print(f"longitud: {len(token_hash) if token_hash else 0}")
        print(f"empieza con 0x: {str(token_hash).startswith('0x')}")

        if token_hash:
            stripped = token_hash[2:] if token_hash.startswith("0x") else token_hash
            print(f"longitud hex sin 0x: {len(stripped)}")

            is_hex = all(c in "0123456789abcdefABCDEF" for c in stripped)
            print(f"solo hex válido: {is_hex}")

            is_bytes32_ok = token_hash.startswith("0x") and len(token_hash) == 66 and is_hex
            print(f"formato bytes32 OK: {is_bytes32_ok}")


if __name__ == "__main__":
    main()