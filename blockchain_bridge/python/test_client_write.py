import secrets

from offpay_chain_client import (
    get_token_status,
    register_token,
    mark_token_returned,
)

token_hash = "0x" + secrets.token_hex(32)

print("=== TOKEN HASH NUEVO ===")
print(token_hash)

print("")
print("=== ESTADO INICIAL ===")
print(get_token_status(token_hash))

print("")
print("=== REGISTRANDO TOKEN ===")
register_result = register_token(token_hash)
print(register_result)

print("")
print("=== ESTADO DESPUES DE REGISTRAR ===")
print(get_token_status(token_hash))

print("")
print("=== MARCANDO TOKEN COMO RETURNED ===")
returned_result = mark_token_returned(token_hash)
print(returned_result)

print("")
print("=== ESTADO FINAL ===")
print(get_token_status(token_hash))

print("")
print("PRUEBA PYTHON DE ESCRITURA COMPLETADA CORRECTAMENTE")