#!/usr/bin/env python3
"""Compile/run only dependency-free Phase 4 rules with JDK 17; not a Maven/JUnit substitute."""
from pathlib import Path
import subprocess
import tempfile
root = Path(__file__).resolve().parents[1]
sources = [
    'src/main/java/com/bookeat/availability/enums/AvailabilityReason.java',
    'src/main/java/com/bookeat/table/enums/TableStatus.java',
    'src/main/java/com/bookeat/table/service/TableRules.java',
    'src/main/java/com/bookeat/menu/service/MenuPricing.java',
    'src/main/java/com/bookeat/availability/service/IntervalIndex.java',
    'src/main/java/com/bookeat/availability/service/AvailabilityEngine.java',
    'src/test/java/com/bookeat/Phase4RuleChecks.java',
]
with tempfile.TemporaryDirectory(prefix='bookeat-phase4-check-') as output:
    subprocess.run(['java', 'com.sun.tools.javac.Main', '--release', '17', '-d', output,
                    *[str(root / p) for p in sources]], check=True)
    subprocess.run(['java', '-cp', output, 'com.bookeat.Phase4RuleChecks'], check=True)
