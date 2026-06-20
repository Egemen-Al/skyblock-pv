# Skyblock ProfileViewer (Fabric 1.21.10)

A **Fabric 1.21.10** port of NotEnoughUpdates' ProfileViewer GUI, backed by an **AWS Lambda** service.
Use `/pve <name>` in-game to view any Hypixel SkyBlock player's profile.

## Features

- `/pve <name>` — profile screen with NEU-style tabs
- Pages: **Skills, Dungeons, Storage, Collections, Pets, Bestiary, Mining, Trophy Fishing, Crimson Isle, Rift**
- **Networth** — matches SkyCrypt (uses SkyHelper's `skyhelper-networth`), with a per-category breakdown tooltip
- **Pets** — level, XP to next/max level, glinted pet preview
- **Rift** — motes, armor/equipment, inventory + ender chest, Vampire slayer, burgers, enigma souls, timecharm panel
- **Dungeons** — catacombs level (50+ overflow), boss collections, class levels, secrets/run
- Clickable player names in chat (→ `/pve`)
- **Discord webhook logging** + **`/users`** command (who looked up whom and when, via DynamoDB)
- Client-side 3-minute profile cache (instant reload)

## Architecture

```
Minecraft mod (Fabric)  -->  AWS Lambda (API Gateway)  -->  Hypixel API
                                     |
                                     +- skyhelper-networth (networth)
                                     +- Discord webhook (logging)
                                     +- DynamoDB (/users stats)
```

The Hypixel API key, Discord webhook and table name are **not in the code** — they live in Lambda environment variables.

## Setup

### 1) Mod (client)
- JDK 21 + IntelliJ IDEA
- Set `BASE_URL` in `src/client/java/com/example/pv/api/BackendApi.java` to **your own backend endpoint**
- `./gradlew build` -> jar in `build/libs/`
- Run: `./gradlew runClient`

### 2) Backend (AWS Lambda, Node.js 20+)
1. `npm install` (in the root — installs `skyhelper-networth` + `@aws-sdk/*`)
2. Zip `index.mjs` + `node_modules` + `package.json`, upload to Lambda (handler: `index.handler`)
3. Add an **API Gateway** trigger and put its URL into the mod's `BASE_URL`
4. **Environment variables:**
   - `HYPIXEL_API_KEY` — key from [developer.hypixel.net](https://developer.hypixel.net) (required)
   - `DISCORD_WEBHOOK_URL` — log channel webhook (optional)
   - `LOG_TABLE` — DynamoDB table name, for `/users` (optional)
5. **DynamoDB** (optional, for `/users`): table with `pk` (String) + `ts` (Number); grant the Lambda role `dynamodb:PutItem` + `dynamodb:Query`
6. Recommended: Lambda timeout **30s**, memory **512 MB** (skyhelper fetches prices/items on first call)

> Note: `skyhelper-networth` wants to write an items backup into its package folder; since Lambda is read-only, `constants/itemsMap.js` is patched to write the backup to `/tmp` instead.

## Commands
- `/pve [name]` — view a profile (no name = yourself)
- `/users` — top users by `/pve` count (requires backend `LOG_TABLE`)

## License
LGPL-3.0-or-later (derivative of NEU). Source must remain open.

## Credits
- [NotEnoughUpdates](https://github.com/NotEnoughUpdates/NotEnoughUpdates) — ProfileViewer GUI and repo data
- [SkyHelper-Networth](https://github.com/Altpapier/SkyHelper-Networth) — networth calculation
- [SkyCrypt](https://github.com/SkyCryptWebsite) — field-mapping reference
