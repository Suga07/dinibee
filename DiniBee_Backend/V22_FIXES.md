# v22 fixes
- Menu image storage now uses a persistent user-home location by default: `${user.home}/.dinibee/menu-images`.
- Override with JVM property `-Ddinibee.menu.media.root=<absolute-path>` when needed.
- Upload and public media controller resolve the same storage root.
