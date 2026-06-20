// Networth hesabi SkyHelper-Networth ile yapilir (SkyCrypt'in kullandigi kutuphane).
// Lambda paketine dahil edilmeli:  npm install skyhelper-networth
// (node_modules ile birlikte zip'leyip yukle, ya da Lambda Layer kullan.)

import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, PutCommand, QueryCommand } from "@aws-sdk/lib-dynamodb";

export const handler = async (event) => {
    try {
        // /users komutu: tum kullanici aktivitesini DynamoDB ozetinden getir.
        if ((event.queryStringParameters?.command || "") === "users") {
            const summary = await getUsersSummary(2000).catch(e => ({ error: String(e) }));
            return json(200, { success: true, ...summary });
        }

        const username = (event.queryStringParameters?.username || "").trim();

        if (!/^[A-Za-z0-9_]{1,16}$/.test(username)) {
            return json(400, {
                success: false,
                error: "Invalid username"
            });
        }

        const hypixelKey = process.env.HYPIXEL_API_KEY;

        if (!hypixelKey) {
            return json(500, {
                success: false,
                error: "HYPIXEL_API_KEY missing"
            });
        }

        // Istek yapan oyuncu (mod gonderir) + Discord webhook log (spam takibi).
        const requester = (event.queryStringParameters?.requester || "?").toString().trim().slice(0, 32) || "?";
        await withTimeout(Promise.all([
            logToDiscord(requester, username).catch(() => {}),
            logToDynamo(requester, username).catch(() => {})
        ]), 4000, "log").catch(() => {});

        const mojangRes = await fetch(
            `https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(username)}`
        );

        if (!mojangRes.ok) {
            return json(404, {
                success: false,
                error: "Minecraft player not found"
            });
        }

        const mojang = await mojangRes.json();
        const uuid = mojang.id;

        const [playerRes, profilesRes, statusRes, skinRes, colResRes] = await Promise.all([
            fetch(`https://api.hypixel.net/v2/player?uuid=${uuid}`, {
                headers: { "API-Key": hypixelKey }
            }),
            fetch(`https://api.hypixel.net/v2/skyblock/profiles?uuid=${uuid}`, {
                headers: { "API-Key": hypixelKey }
            }),
            fetch(`https://api.hypixel.net/v2/status?uuid=${uuid}`, {
                headers: { "API-Key": hypixelKey }
            }),
            fetch(`https://sessionserver.mojang.com/session/minecraft/profile/${uuid}?unsigned=false`),
            fetch(`https://api.hypixel.net/v2/resources/skyblock/collections`)
        ]);

        const playerJson = await playerRes.json();
        const profilesJson = await profilesRes.json();
        const statusJson = await statusRes.json().catch(() => ({ success: false }));

        // --- Collection tier eşikleri (resources, key gerektirmez) ---
        const colResJson = await colResRes.json().catch(() => ({}));
        const collectionsResource = colResJson.collections || {};

        // --- Skin textures (Mojang session server) ---
        const skinJson = await skinRes.json().catch(() => ({}));
        const texProp = (skinJson.properties || []).find(p => p.name === "textures") || {};
        const skin_value = texProp.value || "";
        const skin_signature = texProp.signature || "";

        if (!playerJson.success) {
            return json(502, {
                success: false,
                error: "Hypixel player API failed",
                cause: playerJson.cause || null
            });
        }

        if (!profilesJson.success) {
            return json(502, {
                success: false,
                error: "Hypixel profiles API failed",
                cause: profilesJson.cause || null
            });
        }

        const player = playerJson.player || {};
        const profiles = profilesJson.profiles || [];

        // --- Online durumu ---
        // 1) /status endpoint'i en güveniliridir (online + bulunduğu oyun/mod).
        // 2) Olmazsa lastLogin > lastLogout ise online kabul edilir.
        let online = false;
        let gameType = null;
        let mode = null;
        if (statusJson.success && statusJson.session) {
            online = !!statusJson.session.online;
            gameType = statusJson.session.gameType || null;
            mode = statusJson.session.mode || null;
        } else {
            online = (player.lastLogin || 0) > (player.lastLogout || 0);
        }

        const summaries = profiles.map(profile => {
            const member = profile.members?.[uuid] || {};
            const inv = member.inventory || member;
            const dungeons = member.dungeons || {};

            // Collections: resource eşikleriyle her collection için tier hesabı.
            // Collections coop geneli: tum uyelerin collection sayilarini topla
            // (in-game/SkyCrypt davranisi; coop uyesi az toplamis olsa bile dogru gozuksun).
            const memberCol = {};
            for (const cm of Object.values(profile.members || {})) {
                for (const [cid, camt] of Object.entries(cm.collection || {})) {
                    memberCol[cid] = (memberCol[cid] || 0) + (camt || 0);
                }
            }
            const collections = {};
            for (const [cat, catData] of Object.entries(collectionsResource)) {
                const items = catData.items || {};
                collections[cat] = Object.entries(items).map(([id, info]) => {
                    const amount = memberCol[id] || 0;
                    let tier = 0;
                    for (const t of (info.tiers || [])) {
                        if (amount >= t.amountRequired) tier = t.tier;
                    }
                    return { id, name: info.name, amount, tier, maxTier: info.maxTiers || (info.tiers || []).length };
                });
            }
            const cata = dungeons.dungeon_types?.catacombs || {};
            const master = dungeons.dungeon_types?.master_catacombs || {};
            const playerClasses = dungeons.player_classes || {};

            // Floor completion sayıları + en iyi süreler (normal + master).
            const floorData = (dt) => {
                const tiers = dt.tier_completions || {};
                const fastest = dt.fastest_time || {};
                const fastestS = dt.fastest_time_s || {};
                const fastestSPlus = dt.fastest_time_s_plus || {};
                const bestScore = dt.best_score || {};
                const out = {};
                for (let f = 0; f <= 7; f++) {
                    out[f] = {
                        completions: tiers[f] || 0,
                        fastest: fastest[f] || 0,
                        fastest_s: fastestS[f] || 0,
                        fastest_s_plus: fastestSPlus[f] || 0,
                        best_score: bestScore[f] || 0
                    };
                }
                return out;
            };

            return {
                profile_id: profile.profile_id,
                cute_name: profile.cute_name || "Unknown",
                game_mode: profile.game_mode || "normal",
                selected: profile.selected || false,
                last_save: member.last_save || 0,
                purse: member.currencies?.coin_purse || 0,
                bank: profile.banking?.balance || 0,
                skyblock_level_xp: member.leveling?.experience || 0,
                fairy_souls: member.fairy_soul?.total_collected || 0,
                // Ham base64+gzip NBT - client tarafında decode edilecek.
                inventory: {
                    inv_contents: inv.inv_contents?.data || null,
                    ender_chest: inv.ender_chest_contents?.data || null,
                    talisman_bag: inv.bag_contents?.talisman_bag?.data || null,
                    backpacks: Object.values(inv.backpack_contents || {})
                        .map(b => b?.data).filter(Boolean),
                    armor: inv.inv_armor?.data || null,
                    equipment: inv.equipment_contents?.data || null,
                    wardrobe: inv.wardrobe_contents?.data || null,
                    personal_vault: (inv.personal_vault_contents?.data) || (member.personal_vault_contents?.data) || null,
                    fishing_bag: inv.bag_contents?.fishing_bag?.data || null,
                    potion_bag: inv.bag_contents?.potion_bag?.data || null,
                    quiver: inv.bag_contents?.quiver?.data || null
                },
                accessories: {
                    selected_power: member.accessory_bag_storage?.selected_power || null,
                    highest_magical_power: member.accessory_bag_storage?.highest_magical_power || 0
                },
                collections: collections,
                // Minionlar coop genelinde paylasimli: profildeki TUM uyelerin
                // crafted_generators listelerini birlestir (SkyCrypt / in-game davranisi).
                crafted_generators: (() => {
                    const set = new Set();
                    for (const m of Object.values(profile.members || {})) {
                        for (const g of (m.player_data?.crafted_generators || m.crafted_generators || [])) set.add(g);
                    }
                    return Array.from(set);
                })(),
                bestiary: {
                    kills: member.bestiary?.kills || {},
                    deaths: member.bestiary?.deaths || {},
                    milestone: member.bestiary?.milestone?.last_claimed_milestone || 0
                },
                trophy_fish: member.trophy_fish || {},
                rift: (() => {
                    const r = member.rift || {};
                    const ri = r.inventory || {};
                    const ps = member.player_stats?.rift || {};
                    return {
                        motes_purse: Math.floor(member.currencies?.motes_purse || 0),
                        lifetime_motes: Math.floor(ps.lifetime_motes_earned || 0),
                        enigma_found: (r.enigma?.found_souls || []).length,
                        burgers: r.castle?.grubber_stacks || 0,
                        timecharms: (r.gallery?.secured_trophies || []).length,
                        timecharms_obtained: (r.gallery?.secured_trophies || []).map(t => t.type).filter(Boolean),
                        vampire_xp: member.slayer?.slayer_bosses?.vampire?.xp || 0,
                        inventory: {
                            inv_contents: ri.inv_contents?.data || null,
                            armor: ri.inv_armor?.data || null,
                            equipment: ri.equipment_contents?.data || null,
                            ender_chest: ri.ender_chest_contents?.data || null
                        }
                    };
                })(),
                crimson: (() => {
                    const ni = member.nether_island_player_data || {};
                    const k = ni.kuudra_completed_tiers || {};
                    const d = ni.dojo || {};
                    return {
                        kuudra: {
                            none: k.none || 0, hot: k.hot || 0, burning: k.burning || 0, fiery: k.fiery || 0, infernal: k.infernal || 0,
                            hw_none: k.highest_wave_none || 0, hw_hot: k.highest_wave_hot || 0, hw_burning: k.highest_wave_burning || 0,
                            hw_fiery: k.highest_wave_fiery || 0, hw_infernal: k.highest_wave_infernal || 0
                        },
                        dojo: {
                            mob_kb: d.dojo_points_mob_kb || 0, wall_jump: d.dojo_points_wall_jump || 0, archer: d.dojo_points_archer || 0,
                            sword_swap: d.dojo_points_sword_swap || 0, snake: d.dojo_points_snake || 0, lock_head: d.dojo_points_lock_head || 0,
                            fireball: d.dojo_points_fireball || 0
                        },
                        faction: ni.selected_faction || "N/A",
                        mages_rep: ni.mages_reputation || 0,
                        barbarians_rep: ni.barbarians_reputation || 0,
                        matriarch_pearls: ni.matriarch?.pearls_collected || 0,
                        matriarch_last: ni.matriarch?.last_attempt || 0
                    };
                })(),
                mining: (() => {
                    const mc = member.mining_core || {};
                    const st = member.skill_tree || {};
                    const crystals = {};
                    for (const [k, v] of Object.entries(mc.crystals || {})) {
                        crystals[k.replace("_crystal", "")] = { state: v.state || "NOT_FOUND", total_placed: v.total_placed || 0 };
                    }
                    // Nucleus runs = 5 nucleus kristalinin min total_placed'i
                    let nucleusRuns = Infinity;
                    for (const nk of ["jade", "amber", "topaz", "sapphire", "amethyst"]) {
                        nucleusRuns = Math.min(nucleusRuns, mc.crystals?.[nk + "_crystal"]?.total_placed || 0);
                    }
                    if (!isFinite(nucleusRuns)) nucleusRuns = 0;
                    const stExp = (typeof st.experience === "number") ? st.experience
                        : (typeof st.experience?.mining === "number" ? st.experience.mining
                        : (typeof mc.experience === "number" ? mc.experience : 0));
                    const rawNodes = st.nodes?.mining || mc.nodes || {};
                    const nodes = {};
                    for (const [k, v] of Object.entries(rawNodes)) {
                        nodes[k] = (typeof v === "number") ? v : (typeof v?.level === "number" ? v.level : 0);
                    }
                    return {
                        experience: stExp,
                        tokens: mc.tokens || 0,
                        powder_mithril: (mc.powder_mithril || 0),
                        powder_mithril_total: (mc.powder_mithril || 0) + (mc.powder_spent_mithril || 0),
                        powder_gemstone: (mc.powder_gemstone || 0),
                        powder_gemstone_total: (mc.powder_gemstone || 0) + (mc.powder_spent_gemstone || 0),
                        powder_glacite: (mc.powder_glacite || 0),
                        powder_glacite_total: (mc.powder_glacite || 0) + (mc.powder_spent_glacite || 0),
                        nodes: nodes,
                        crystals: crystals,
                        nucleus_runs: nucleusRuns
                    };
                })(),
                pets: ((member.pets_data?.pets) || member.pets || []).map(p => ({
                    type: p.type,
                    tier: p.tier,
                    exp: p.exp || 0,
                    active: !!p.active,
                    heldItem: p.heldItem || null,
                    candyUsed: p.candyUsed || 0,
                    skin: p.skin || null
                })),
                skills: {
                    combat: member.player_data?.experience?.SKILL_COMBAT || 0,
                    mining: member.player_data?.experience?.SKILL_MINING || 0,
                    farming: member.player_data?.experience?.SKILL_FARMING || 0,
                    foraging: member.player_data?.experience?.SKILL_FORAGING || 0,
                    fishing: member.player_data?.experience?.SKILL_FISHING || 0,
                    enchanting: member.player_data?.experience?.SKILL_ENCHANTING || 0,
                    alchemy: member.player_data?.experience?.SKILL_ALCHEMY || 0,
                    taming: member.player_data?.experience?.SKILL_TAMING || 0,
                    hunting: member.player_data?.experience?.SKILL_HUNTING || 0,
                    dungeoneering: cata.experience || 0,
                    carpentry: member.player_data?.experience?.SKILL_CARPENTRY || 0,
                    runecrafting: member.player_data?.experience?.SKILL_RUNECRAFTING || 0,
                    social: member.player_data?.experience?.SKILL_SOCIAL || 0
                },
                slayers: {
                    zombie: member.slayer?.slayer_bosses?.zombie?.xp || 0,
                    spider: member.slayer?.slayer_bosses?.spider?.xp || 0,
                    wolf: member.slayer?.slayer_bosses?.wolf?.xp || 0,
                    enderman: member.slayer?.slayer_bosses?.enderman?.xp || 0,
                    blaze: member.slayer?.slayer_bosses?.blaze?.xp || 0,
                    vampire: member.slayer?.slayer_bosses?.vampire?.xp || 0
                },
                dungeons: {
                    catacombs_xp: cata.experience || 0,
                    master_catacombs_xp: master.experience || 0,
                    // SkyCrypt ile ayni: profil bazli secrets (member.dungeons.secrets)
                    secrets_found: dungeons.secrets || 0,
                    // SkyCrypt S/R paydasi: TUM tier_completions (giris kati 0 dahil,
                    // normal + master, "total" anahtari haric, master 2'yle CARPILMADAN).
                    secrets_per_run: (() => {
                        const sumTiers = (dt) => Object.entries(dt.tier_completions || {})
                            .filter(([k, v]) => k !== "total" && v > 0)
                            .reduce((a, [, v]) => a + v, 0);
                        const runs = sumTiers(cata) + sumTiers(master);
                        return runs > 0 ? Math.round(((dungeons.secrets || 0) / runs) * 100) / 100 : 0;
                    })(),
                    classes: {
                        healer: playerClasses.healer?.experience || 0,
                        mage: playerClasses.mage?.experience || 0,
                        berserk: playerClasses.berserk?.experience || 0,
                        archer: playerClasses.archer?.experience || 0,
                        tank: playerClasses.tank?.experience || 0
                    },
                    selected_class: dungeons.selected_dungeon_class || null,
                    floors: floorData(cata),
                    master_floors: floorData(master),
                    // Boss collections = floor tier completions (M1-M7 / F1-F7)
                    boss_collections: {
                        f0: cata.tier_completions?.[0] || 0,
                        f1: cata.tier_completions?.[1] || 0,
                        f2: cata.tier_completions?.[2] || 0,
                        f3: cata.tier_completions?.[3] || 0,
                        f4: cata.tier_completions?.[4] || 0,
                        f5: cata.tier_completions?.[5] || 0,
                        f6: cata.tier_completions?.[6] || 0,
                        f7: cata.tier_completions?.[7] || 0,
                        m1: master.tier_completions?.[1] || 0,
                        m2: master.tier_completions?.[2] || 0,
                        m3: master.tier_completions?.[3] || 0,
                        m4: master.tier_completions?.[4] || 0,
                        m5: master.tier_completions?.[5] || 0,
                        m6: master.tier_completions?.[6] || 0,
                        m7: master.tier_completions?.[7] || 0
                    },
                    total_runs: [1,2,3,4,5,6,7].reduce((a,i) => a + (cata.tier_completions?.[i] || 0), 0),
                    total_runs_master: [1,2,3,4,5,6,7].reduce((a,i) => a + (master.tier_completions?.[i] || 0), 0)
                }
            };
        });

        summaries.sort((a, b) => b.last_save - a.last_save);

        // Networth: SkyHelper-Networth (SkyCrypt ile birebir ayni hesap).
        // Sadece ana (en son oynanan) profil icin hesaplanir.
        let nwError = null;
        if (summaries.length > 0) {
            const mp = profiles.find(p => p.profile_id === summaries[0].profile_id);
            const mm = mp?.members?.[uuid] || {};
            try {
                const { ProfileNetworthCalculator } = await import("skyhelper-networth");

                // Museum verisi (Museum networth icin; opsiyonel ama SkyCrypt ile esleme icin onemli).
                let museumData = null;
                try {
                    const museumRes = await fetch(
                        `https://api.hypixel.net/v2/skyblock/museum?profile=${mp.profile_id}`,
                        { headers: { "API-Key": hypixelKey } }
                    );
                    const museumJson = await museumRes.json();
                    museumData = museumJson?.members?.[uuid] || null;
                } catch (e) { /* museum yoksa atla */ }

                const calc = new ProfileNetworthCalculator(mm, museumData, mp?.banking?.balance || 0);
                const nw = await Promise.race([
                    calc.getNetworth(),
                    new Promise((_, rej) => setTimeout(() => rej(new Error("networth timeout (8s)")), 8000))
                ]);

                const petsTotal = nw.types?.pets?.total || 0;
                summaries[0].networth = {
                    total: Math.floor(nw.networth || 0),
                    unsoulbound: Math.floor(nw.unsoulboundNetworth || 0),
                    purse: Math.floor(nw.purse || 0),
                    bank: Math.floor(nw.bank || 0),
                    pets: Math.floor(petsTotal),
                    // "Items" = pets/purse/bank disindaki her sey (accessories, storage,
                    // museum, sacks, essence, inventory, armor, ... hepsi dahil).
                    items: Math.floor((nw.networth || 0) - (nw.purse || 0) - (nw.bank || 0) - petsTotal),
                    // Tum kategori kirilimi (client isterse tooltip'te gosterebilir).
                    types: Object.fromEntries(
                        Object.entries(nw.types || {}).map(([k, v]) => [k, Math.floor(v?.total || 0)])
                    )
                };
            } catch (e) {
                summaries[0].networth = null;
                // Hata ayiklama: networth neden hesaplanamadi (gecici).
                nwError = String(e?.stack || e?.message || e);
                summaries[0].networth_error = nwError;
            }
        }

        // Yanıt boyutunu küçült: ağır verileri sadece ana (en son) profilde tut.
        for (let i = 1; i < summaries.length; i++) {
            delete summaries[i].inventory;
            delete summaries[i].collections;
            delete summaries[i].bestiary;
            delete summaries[i].trophy_fish;
            delete summaries[i].pets;
            delete summaries[i].mining;
            delete summaries[i].crafted_generators;
            delete summaries[i].crimson;
            delete summaries[i].rift;
        }

        return json(200, {
            success: true,
            _v: "nw4",
            nw_error: nwError,
            username: mojang.name,
            uuid: uuid,
            skin_value: skin_value,
            skin_signature: skin_signature,
            displayname: player.displayname || mojang.name,
            rank: getRank(player),
            online: online,
            game_type: gameType,
            mode: mode,
            last_login: player.lastLogin || 0,
            last_logout: player.lastLogout || 0,
            first_login: player.firstLogin || 0,
            profiles_count: summaries.length,
            selected_profile: summaries.length > 0 ? summaries[0] : null,
            profiles: summaries
        });

    } catch (e) {
        return json(500, {
            success: false,
            error: String(e),
            message: e?.message || null
        });
    }
};

// ===================== DYNAMODB LOG / USERS =====================
// Her istegi DynamoDB tablosuna yazar; /users bunu ozetler.
// Lambda env: LOG_TABLE = tablo adi. Tablo: pk (String) + ts (Number) anahtar.
// Bir promise'i ms icinde bitmezse reddeder (DynamoDB takilmasini onler).
function withTimeout(promise, ms, label) {
    return Promise.race([
        promise,
        new Promise((_, rej) => setTimeout(() => rej(new Error((label || 'op') + ' timeout ' + ms + 'ms')), ms))
    ]);
}

let _ddb = null;
function getDdb() {
    if (!process.env.LOG_TABLE) return null;
    if (!_ddb) _ddb = DynamoDBDocumentClient.from(new DynamoDBClient({ maxAttempts: 2 }));
    return _ddb;
}

async function logToDynamo(requester, target) {
    const db = getDdb();
    if (!db) return;
    const now = Date.now();
    await withTimeout(db.send(new PutCommand({
        TableName: process.env.LOG_TABLE,
        Item: {
            pk: "log",
            ts: now,
            requester: requester || "?",
            target: target || "?",
            // TTL: 30 gun (tabloda TTL attribute = expireAt olarak ayarla, istege bagli)
            expireAt: Math.floor(now / 1000) + 30 * 24 * 3600
        }
    })), 3000, "ddb-put");
}

async function getUsersSummary(limit) {
    const db = getDdb();
    if (!db) return { error: "LOG_TABLE env not set" };
    const res = await withTimeout(db.send(new QueryCommand({
        TableName: process.env.LOG_TABLE,
        KeyConditionExpression: "pk = :p",
        ExpressionAttributeValues: { ":p": "log" },
        ScanIndexForward: false,
        Limit: limit || 1000
    })), 4000, "ddb-query");
    const items = res.Items || [];
    const byUser = {};
    for (const it of items) {
        const u = it.requester || "?";
        if (!byUser[u]) byUser[u] = { requester: u, count: 0, lastSeen: 0, lastTarget: null };
        byUser[u].count++;
        if (it.ts > byUser[u].lastSeen) { byUser[u].lastSeen = it.ts; byUser[u].lastTarget = it.target; }
    }
    const users = Object.values(byUser).sort((a, b) => b.count - a.count);
    const recent = items.slice(0, 30).map(it => ({ requester: it.requester, target: it.target, ts: it.ts }));
    return { total_requests: items.length, unique_users: users.length, users, recent };
}

// ===================== DISCORD WEBHOOK LOG =====================
// Her /pve istegini bir Discord kanalina yazar: kim, kimi, ne zaman.
// Webhook URL'si Lambda env: DISCORD_WEBHOOK_URL
async function logToDiscord(requester, target) {
    const url = process.env.DISCORD_WEBHOOK_URL;
    if (!url) return;
    const ts = Math.floor(Date.now() / 1000);
    const body = {
        username: "Skyblock PV Logs",
        embeds: [{
            title: "\ud83d\udd0d /pve",
            color: 0x5865F2,
            fields: [
                { name: "Kullanici", value: "`" + (requester || "?") + "`", inline: true },
                { name: "Bakilan", value: "`" + target + "`", inline: true },
                { name: "Zaman", value: `<t:${ts}:F> \u00b7 <t:${ts}:R>`, inline: false }
            ]
        }]
    };
    const ctrl = new AbortController();
    const tm = setTimeout(() => ctrl.abort(), 2500);
    try {
        await fetch(url, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(body),
            signal: ctrl.signal
        });
    } catch (e) {
        // webhook hatasi profili etkilemesin
    } finally {
        clearTimeout(tm);
    }
}

function getRank(player) {
    if (!player) return "DEFAULT";
    if (player.prefix) return player.prefix;
    if (player.monthlyPackageRank && player.monthlyPackageRank !== "NONE") {
        return player.monthlyPackageRank;
    }
    if (player.newPackageRank) return player.newPackageRank;
    if (player.packageRank) return player.packageRank;
    return "DEFAULT";
}

function json(statusCode, body) {
    return {
        statusCode: statusCode,
        headers: {
            "content-type": "application/json",
            "access-control-allow-origin": "*"
        },
        body: JSON.stringify(body)
    };
}
