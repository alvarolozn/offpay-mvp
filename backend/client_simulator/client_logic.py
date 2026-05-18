import json
from pathlib import Path

import qrcode

TOKEN_VALUE_COP = 10000
BASE_DIR = Path(__file__).parent
LOCAL_FILE = BASE_DIR / "local_tokens.json"
QR_IMAGE_FILE = BASE_DIR / "payment_qr.png"
QR_TEXT_FILE = BASE_DIR / "payment_payload.json"


def load_local_tokens():
    if not LOCAL_FILE.exists():
        raise FileNotFoundError(f"No existe el archivo: {LOCAL_FILE}")

    with open(LOCAL_FILE, "r", encoding="utf-8") as f:
        return json.load(f)


def save_local_tokens(data):
    with open(LOCAL_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


def get_available_local_tokens(data):
    tokens = data.get("tokens", [])
    available = []

    for token in tokens:
        backend_status = token.get("status")
        local_status = token.get("local_status", "AVAILABLE")

        if backend_status == "AVAILABLE" and local_status == "AVAILABLE":
            available.append(token)

    available.sort(key=lambda t: t["counter"])
    return available


def build_payment_package(data, amount_cop: int):
    if amount_cop <= 0:
        raise ValueError("El monto debe ser mayor a cero")

    if amount_cop % TOKEN_VALUE_COP != 0:
        raise ValueError("El monto debe ser múltiplo de 10000 COP")

    token_count = amount_cop // TOKEN_VALUE_COP
    available_tokens = get_available_local_tokens(data)

    if len(available_tokens) < token_count:
        raise ValueError(
            f"No hay suficientes tokens disponibles. "
            f"Necesitas {token_count} y solo tienes {len(available_tokens)}."
        )

    selected_tokens = available_tokens[:token_count]
    payment_codes = [token["payment_code"] for token in selected_tokens]

    qr_payload = {
        "type": "OFFPAY_PACKAGE",
        "client_id": data["client_id"],
        "amount_cop": amount_cop,
        "token_count": token_count,
        "payment_codes": payment_codes
    }

    for token in data["tokens"]:
        if token["payment_code"] in payment_codes:
            token["local_status"] = "EXPOSED_LOCAL"

    return qr_payload, selected_tokens


def save_qr_payload(qr_payload):
    qr_payload_text = json.dumps(qr_payload, ensure_ascii=False)

    with open(QR_TEXT_FILE, "w", encoding="utf-8") as f:
        f.write(qr_payload_text)

    qr = qrcode.QRCode(
        version=None,
        box_size=10,
        border=4
    )
    qr.add_data(qr_payload_text)
    qr.make(fit=True)

    img = qr.make_image(fill_color="black", back_color="white")
    img.save(QR_IMAGE_FILE)

    return qr_payload_text


def main():
    try:
        data = load_local_tokens()

        print("\n=== OFFPAY CLIENT SIMULATOR ===")
        print(f"Cliente: {data['client_id']}")

        available_tokens = get_available_local_tokens(data)
        print(f"Tokens locales disponibles: {len(available_tokens)}")
        print(f"Saldo pagable offline: {len(available_tokens) * TOKEN_VALUE_COP} COP")

        amount_input = input("Ingresa el monto a pagar (múltiplo de 10000): ").strip()
        amount_cop = int(amount_input)

        qr_payload, selected_tokens = build_payment_package(data, amount_cop)
        qr_payload_text = save_qr_payload(qr_payload)
        save_local_tokens(data)

        print("\n=== PAGO PREPARADO ===")
        print("Tokens seleccionados:")
        for token in selected_tokens:
            print(f"- counter={token['counter']} | payment_code={token['payment_code']}")

        print("\n=== PAYLOAD QR ===")
        print(json.dumps(qr_payload, indent=2, ensure_ascii=False))

        print("\n=== QR_PAYLOAD_TEXT ===")
        print(qr_payload_text)

        print(f"\nArchivo QR creado en: {QR_IMAGE_FILE}")
        print(f"Archivo payload creado en: {QR_TEXT_FILE}")

        print("\nLos tokens usados para este QR quedaron marcados como EXPOSED_LOCAL.")
        print("No deberían volver a usarse localmente.")

    except Exception as e:
        print(f"\nError: {e}")


if __name__ == "__main__":
    main()