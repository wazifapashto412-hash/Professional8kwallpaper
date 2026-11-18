# Upload to GitHub
1. Create an empty repository (private is fine) on github.com.
2. Unzip the project. In the folder:
```
git init
git add .
git commit -m "Infinity 8K Wallpapers"
git branch -M main
git remote add origin https://github.com/YOUR-USER/YOUR-REPO.git
git push -u origin main
```
(or use GitHub Desktop / upload via the web UI.)
3. Check that `local.properties`, `google-services.json`, `*.jks` are **not** in the repo (`.gitignore` handles it).
4. Add the secrets listed in `docs/BUILD.md`.
5. Actions tab → *Android CI* → Run workflow → download **apk-and-aab**.
