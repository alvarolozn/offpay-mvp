from db_client import fetch_all, fetch_one


def main() -> None:
    print("=== PRUEBA DE LECTURA DB OFFPAY ===")

    version_row = fetch_one("SELECT version() AS version;")
    print("Conexión OK")
    print(f"Versión DB: {version_row['version']}")

    print("\n=== TOKENS (máximo 5) ===")
    rows = fetch_all(
        """
        SELECT
            id,
            token_hash,
            status,
            blockchain_status,
            register_tx_hash,
            used_tx_hash,
            returned_tx_hash
        FROM tokens
        ORDER BY id DESC
        LIMIT 5;
        """
    )

    print(f"Filas encontradas: {len(rows)}")

    for i, row in enumerate(rows, start=1):
        print(f"\n--- Token {i} ---")
        print(f"id: {row.get('id')}")
        print(f"token_hash: {row.get('token_hash')}")
        print(f"status: {row.get('status')}")
        print(f"blockchain_status: {row.get('blockchain_status')}")
        print(f"register_tx_hash: {row.get('register_tx_hash')}")
        print(f"used_tx_hash: {row.get('used_tx_hash')}")
        print(f"returned_tx_hash: {row.get('returned_tx_hash')}")


if __name__ == "__main__":
    main()