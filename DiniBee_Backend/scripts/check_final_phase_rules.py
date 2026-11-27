#!/usr/bin/env python3
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
files=['dining/enums/DiningStatus.java','dining/service/DiningRules.java','payment/service/PaymentRules.java','wallet/service/WalletRules.java','report/service/ReportExporter.java']
with tempfile.TemporaryDirectory() as tmp:
 sources=[str(root/'src/main/java/com/bookeat'/f) for f in files]+[str(root/'src/test/java/com/bookeat/FinalPhaseRuleChecks.java')]
 subprocess.run(['java','com.sun.tools.javac.Main','--release','17','-d',tmp,*sources],check=True)
 subprocess.run(['java','-cp',tmp,'com.bookeat.FinalPhaseRuleChecks'],check=True)
