from db_client import fetch_all


def main() -> None:
    print("=== TOKENS PENDIENTES DE REGISTRO EN BLOCKCHAIN ===")

    rows = fetch_all(
        """
        SELECT
            id,
            token_hash,
            status,
            blockchain_status,
            register_tx_hash,
            used_tx_hash,
            returned_tx_hash,
            chain_id,
            contract_address
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
        print("No se encontraron tokens pendientes de registrar.")
        return

    for i, row in enumerate(rows, start=1):
        print(f"\n--- Pendiente {i} ---")
        print(f"id: {row.get('id')}")
        print(f"token_hash: {row.get('token_hash')}")
        print(f"status: {row.get('status')}")
        print(f"blockchain_status: {row.get('blockchain_status')}")
        print(f"register_tx_hash: {row.get('register_tx_hash')}")
        print(f"used_tx_hash: {row.get('used_tx_hash')}")
        print(f"returned_tx_hash: {row.get('returned_tx_hash')}")
        print(f"chain_id: {row.get('chain_id')}")
        print(f"contract_address: {row.get('contract_address')}")


if __name__ == "__main__":
    main()