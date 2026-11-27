# v36 DiniTicket session sync fix
- Added persisted DiniTicket sessions.
- Token labels restart at DT-1 for each new archived-menu session while queue_sequence remains globally unique.
- Legacy active tickets are expired when the first new session starts so old DT-3 cannot leak into a fresh session.
- Owner live queue no longer expires tickets merely because the dashboard refreshes.
- Current customer tickets are limited to the active DiniTicket session.
- Session menu is archived when the configured session token limit is reached.
