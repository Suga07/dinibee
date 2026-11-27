#!/usr/bin/env python3
"""Compile and execute actual dependency-free Zoho protocol rules; no remote services."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(['java','com.sun.tools.javac.Main','--release','17','-d',tmp,*[str(root/x) for x in ['src/main/java/com/bookeat/payment/service/PaymentRules.java','src/main/java/com/bookeat/payment/gateway/ZohoSignatures.java','src/test/java/com/bookeat/ZohoProtocolChecks.java']]],check=True)
    subprocess.run(['java','-cp',tmp,'com.bookeat.ZohoProtocolChecks'],check=True)
