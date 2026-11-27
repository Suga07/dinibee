#!/usr/bin/env python3
"""Run actual dependency-free workflow, time and encryption checks with Java 17."""
from pathlib import Path
import subprocess, tempfile
root = Path(__file__).resolve().parents[1]
paths = list((root / 'src/main/java/com/bookeat/restaurant/enums').glob('*.java'))
paths += [root / 'src/main/java/com/bookeat/restaurant' / p for p in [
    'service/WorkflowRules.java', 'security/SettlementCipher.java', 'storage/DocumentContentRules.java']]
paths += [root / 'scripts/qa/OnboardingWorkflowChecks.java']
with tempfile.TemporaryDirectory() as dest:
    subprocess.run(['java', 'com.sun.tools.javac.Main', '--release', '17', '-d', dest, *map(str, paths)], check=True)
    subprocess.run(['java', '-cp', dest, 'OnboardingWorkflowChecks'], check=True)
