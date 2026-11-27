"""Generate a private local .env without printing or overwriting any secrets."""
from pathlib import Path
import base64
import secrets

root = Path(__file__).resolve().parent.parent
target = root / ".env"
values = {name: base64.b64encode(secrets.token_bytes(32)).decode("ascii")
          for name in ("JWT_SECRET", "AUTH_TOKEN_PEPPER", "MAIL_ENCRYPTION_KEY", "SETTLEMENT_ENCRYPTION_KEY")}
values["DB_PASSWORD"] = secrets.token_urlsafe(32)
lines = []
for line in (root / ".env.example").read_text().splitlines():
    name = line.split("=", 1)[0]
    lines.append(name + "=" + values[name] if name in values else line)
with target.open("x", encoding="utf-8") as output:
    output.write("\n".join(lines) + "\n")
target.chmod(0o600)
print("Created .env with unique secrets. Existing files are never overwritten.")
