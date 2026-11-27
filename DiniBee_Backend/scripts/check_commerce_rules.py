#!/usr/bin/env python3
"""Compile/run dependency-free Phase 5–8 rules. Requires JDK 17 and Python 3; not Maven/JUnit."""
from pathlib import Path
import subprocess,tempfile
r=Path(__file__).resolve().parents[1]
paths=['commerce/service/GeoRules','reservation/service/BookingRules','reservation/enums/ReservationStatus','reservation/enums/RecurrenceType','order/service/OrderRules','order/service/OrderPricing','order/enums/FoodOrderStatus','diniticket/service/TicketRules','diniticket/enums/TicketStatus']
files=[str(r/'src/main/java/com/bookeat'/(p+'.java')) for p in paths]+[str(r/'src/test/java/com/bookeat/CommerceRuleChecks.java')]
with tempfile.TemporaryDirectory(prefix='bookeat-commerce-check-') as out:
    subprocess.run(['java','com.sun.tools.javac.Main','--release','17','-d',out,*files],check=True)
    subprocess.run(['java','-cp',out,'com.bookeat.CommerceRuleChecks'],check=True)
