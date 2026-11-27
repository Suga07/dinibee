"""Add the Phase 3 bank encryption key without changing any existing secrets."""
from pathlib import Path
import base64
import secrets
import os
root=Path(__file__).resolve().parent.parent
target=root/".env"
with target.open("r+",encoding="utf-8") as output:
    existing=output.read()
    if any(line.strip().startswith("SETTLEMENT_ENCRYPTION_KEY=") for line in existing.splitlines()):
        print("Settlement key already configured; existing values were preserved.")
    else:
        key=base64.b64encode(secrets.token_bytes(32)).decode("ascii")
        output.write(("" if existing.endswith("\n") else "\n")+"SETTLEMENT_ENCRYPTION_KEY="+key+"\n")
        output.flush();os.fsync(output.fileno())
        print("Added the Phase 3 key. Existing authentication and mail keys were preserved.")
target.chmod(0o600)
