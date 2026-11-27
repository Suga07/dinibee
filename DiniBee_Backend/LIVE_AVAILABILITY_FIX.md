# Live availability fix

`AvailabilityServiceImpl` now returns owner-created tables that are genuinely available in at least one calculated slot when availability is queried without an exact time. Existing allocation, operating-hours, reservation-settings, approval/service, capacity and booking-window rules remain unchanged.
