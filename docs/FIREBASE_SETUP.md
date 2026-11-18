# Firebase setup (live wallpaper updates without a new APK)

1. Firebase Console → create a project → add Android app, package `com.infinity.wallpapers` (add your SHA-1 later if needed).
2. Download `google-services.json` → put in `app/` (ignored by git; for CI use the `GOOGLE_SERVICES_JSON_BASE64` secret).
3. Build > Firestore Database → create database. Paste `firestore.rules` (public read, no client writes) → Publish.
4. Images: host properly licensed files in Firebase Storage (make them publicly readable via download URLs) or any HTTPS CDN. Create three versions per wallpaper: `thumbUrl` (~400×700), `previewUrl` (~1080–1900 px), `fullUrl` (original; 7680 px on its long side for real 8K).
5. Add documents in collection `wallpapers`:

| field | type | notes |
|---|---|---|
| title | string | |
| category | string | slug: amoled, nature, mountains, cars, supercars, anime, abstract, space, animals, architecture, technology, minimalist, flowers, luxury, dark, art (or your own category id) |
| thumbUrl / previewUrl / fullUrl | string | HTTPS |
| width, height | number | **real pixels of fullUrl** |
| maxSide | number | max(width,height) – drives 8K (≥7680) / 4K (≥3840) categories |
| trending, featured | boolean | |
| downloads | number | popularity |
| createdAt | number (millis) | latest ordering |
| credit | string | photographer / licence |

Optional collection `categories` (doc id = slug, fields `name`, `order`) adds categories remotely.

6. Admin: use the Firebase Console, or `tools/wallpaper-admin.js` (bulk import / edit / remove; needs a service-account key kept private; untested in the build sandbox). Sample: `tools/wallpapers.sample.json`.
7. Indexes: the first time a category/trending/8K screen loads, Firestore may log "requires an index" with a link; open it and click Create. Typical ones: `category + createdAt`, `trending + createdAt`, `featured + createdAt`, `category + downloads`, `maxSide`.
8. Production hardening: enable App Check; keep rules read-only; if you use paid Storage egress, put a CDN in front.
