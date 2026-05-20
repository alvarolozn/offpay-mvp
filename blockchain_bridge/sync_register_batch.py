from contract_client import OffPayContractClient
from db_client import fetch_all, execute_many


def main() -> None:
    print("=== SYNC DE REGISTRO EN BLOCKCHAIN ===")

    rows = fetch_all(
        """
        SELECT
            id,
            token_hash,
            status,
            blockchain_status,
            register_tx_hash
        FROM tokens
        WHERE token_hash IS NOT NULL
          AND token_hash <> ''
          AND register_tx_hash IS NULL
        ORDER BY id ASC
        LIMIT 20;
        """
    )

    print(f"Candidatos encontrados: {len(rows)}")

    if not rows:
        print("No hay tokens pendientes de registrar.")
        return

    client = OffPayContractClient()

    # Prevalidación en blockchain:
    # si alguno ya existe en cadena, abortamos para no romper el lote.
    already_on_chain = []
    ready_to_send = []

    for row in rows:
        token_hash = row["token_hash"]
        state = client.get_token_state(token_hash)

        if state == 0:
            ready_to_send.append(row)
        else:
            already_on_chain.append(
                {
                    "id": row["id"],
                    "token_hash": token_hash,
                    "chain_state": state,
                }
            )

    print(f"Listos para enviar: {len(ready_to_send)}")
    print(f"Ya existentes en blockchain: {len(already_on_chain)}")

    if already_on_chain:
        print("\nSe aborta el proceso porque hay tokens que ya existen en blockchain.")
        print("Esto evita que falle la transacción batch completa.\n")

        for item in already_on_chain:
            print(
                f"id={item['id']} | token_hash={item['token_hash']} | chain_state={item['chain_state']}"
            )

        return

    if not ready_to_send:
        print("No quedó ningún token listo para enviar.")
        return

    token_hashes = [row["token_hash"] for row in ready_to_send]

    print("\nEnviando lote a blockchain...")
    result = client.register_tokens(token_hashes)

    print("\n=== RESULTADO BLOCKCHAIN ===")
    print(f"tx_hash: {result['tx_hash']}")
    print(f"status: {result['status']}")
    print(f"block_number: {result['block_number']}")
    print(f"gas_used: {result['gas_used']}")

    if result["status"] != 1:
        raise RuntimeError("La transacción fue minada pero no quedó exitosa (status != 1)")

    update_params = [
        (
            result["tx_hash"],
            client.get_chain_id(),
            client.contract_address,
            row["id"],
        )
        for row in ready_to_send
    ]

    execute_many(
        """
        UPDATE tokens
        SET
            register_tx_hash = %s,
            chain_id = %s,
            contract_address = %s
        WHERE id = %s;
        """,
        update_params,
    )

    print("\n=== ACTUALIZACIÓN DB OK ===")
    print(f"Tokens actualizados en DB: {len(update_params)}")

    print("\n=== IDS PROCESADOS ===")
    for row in ready_to_send:
        print(f"id={row['id']} | token_hash={row['token_hash']}")


if __name__ == "__main__":
    main()