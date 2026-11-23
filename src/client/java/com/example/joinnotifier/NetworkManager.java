package com.example.joinnotifier;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManager {
    // ВСТАВЬ СЮДА СВОЮ НАСТОЯЩУЮ ССЫЛКУ ИЗ CLOUDFLARE (без слэша на конце)
    public static final String API_URL = "There was the url";

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) JoinNotifier/1.2.0";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private static final Path CACHE_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("joinnotifier_servers_cache.json");

    private static final Set<String> blockedServers = ConcurrentHashMap.newKeySet();
    private static volatile String adminToken = null;
    private static long lastSyncTime = 0;

    public static class SyncResult {
        public final boolean online;
        public final boolean banned;
        public final String banReason;
        public final Component errorComponent;

        public SyncResult(boolean online, boolean banned, String banReason, Component errorComponent) {
            this.online = online;
            this.banned = banned;
            this.banReason = banReason;
            this.errorComponent = errorComponent;
        }
    }

    public static class IdeaSubmitResult {
        public final String status;
        public final Component detail;

        public IdeaSubmitResult(String status, Component detail) {
            this.status = status;
            this.detail = detail;
        }
    }

    public static class IdeaItem {
        public String id;
        public String author;
        public String text;
        public long timestamp;
    }

    public static class BanItem {
        public String name;
        public String reason;
    }

    public static class AdminData {
        public final boolean ok;
        public final List<IdeaItem> ideas;
        public final List<BanItem> bannedUsers;
        public final List<String> blockedServers;
        public final List<String> adminWhitelist;

        public AdminData(boolean ok, List<IdeaItem> ideas, List<BanItem> bannedUsers,
                         List<String> blockedServers, List<String> adminWhitelist) {
            this.ok = ok;
            this.ideas = ideas;
            this.bannedUsers = bannedUsers;
            this.blockedServers = blockedServers;
            this.adminWhitelist = adminWhitelist;
        }
    }

    public static class LoginResult {
        public final boolean ok;
        public final Component errorComponent;

        public LoginResult(boolean ok, Component errorComponent) {
            this.ok = ok;
            this.errorComponent = errorComponent;
        }
    }

    public static boolean isApiUrlPlaceholder() {
        return API_URL.contains("твой-ник") || !API_URL.startsWith("http");
    }

    public static void loadCachedBlockedServers() {
        try {
            if (Files.exists(CACHE_PATH)) {
                String json = Files.readString(CACHE_PATH);
                String[] arr = GSON.fromJson(json, String[].class);
                if (arr != null) {
                    blockedServers.clear();
                    for (String s : arr) blockedServers.add(normalizeIp(s));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveCachedBlockedServers() {
        try {
            Files.createDirectories(CACHE_PATH.getParent());
            Files.writeString(CACHE_PATH, GSON.toJson(blockedServers));
        } catch (Exception ignored) {
        }
    }

    public static void refreshSyncIfNeeded(String username, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastSyncTime < 120_000L) return;
        lastSyncTime = now;
        syncAsync(username);
    }

    public static CompletableFuture<SyncResult> syncAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            if (isApiUrlPlaceholder()) {
                return new SyncResult(false, false, "",
                        Component.translatable("gui.joinnotifier.net.placeholder"));
            }
            try {
                String encoded = URLEncoder.encode(username == null ? "" : username, StandardCharsets.UTF_8);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "/sync?user=" + encoded))
                        .timeout(Duration.ofSeconds(6))
                        .header("User-Agent", USER_AGENT)
                        .GET()
                        .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 500) {
                    return new SyncResult(false, false, "",
                            Component.translatable("gui.joinnotifier.net.no_db"));
                }
                if (resp.statusCode() != 200) {
                    return new SyncResult(false, false, "",
                            Component.translatable("gui.joinnotifier.net.http_error", resp.statusCode()));
                }

                JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
                boolean online = obj.has("online") && obj.get("online").getAsBoolean();
                boolean banned = obj.has("banned") && obj.get("banned").getAsBoolean();
                String banReason = obj.has("banReason") ? obj.get("banReason").getAsString() : "";

                if (obj.has("blockedServers") && obj.get("blockedServers").isJsonArray()) {
                    blockedServers.clear();
                    for (JsonElement el : obj.getAsJsonArray("blockedServers")) {
                        blockedServers.add(normalizeIp(el.getAsString()));
                    }
                    saveCachedBlockedServers();
                }

                return new SyncResult(online, banned, banReason, Component.empty());
            } catch (Exception e) {
                return new SyncResult(false, false, "",
                        Component.translatable("gui.joinnotifier.net.offline"));
            }
        });
    }

    public static boolean isCurrentServerBlocked(Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server == null || server.ip == null) return false;

        String raw = normalizeIp(server.ip);
        String hostOnly = raw.contains(":") ? raw.substring(0, raw.indexOf(':')) : raw;

        return blockedServers.contains(raw) || blockedServers.contains(hostOnly);
    }

    private static String normalizeIp(String ip) {
        String s = ip.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith(":25565")) {
            s = s.substring(0, s.length() - 6);
        }
        return s;
    }

    public static CompletableFuture<IdeaSubmitResult> submitIdeaAsync(String author, String text) {
        return CompletableFuture.supplyAsync(() -> {
            if (isApiUrlPlaceholder()) {
                return new IdeaSubmitResult("ERROR",
                        Component.translatable("gui.joinnotifier.net.placeholder"));
            }
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("author", author);
                payload.addProperty("text", text);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "/ideas"))
                        .timeout(Duration.ofSeconds(6))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                        .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    return new IdeaSubmitResult("OK", Component.empty());
                }
                if (resp.statusCode() == 403) {
                    JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
                    String reason = obj.has("reason") ? obj.get("reason").getAsString() : "";
                    Component reasonComp = isDefaultReason(reason)
                            ? Component.translatable("gui.joinnotifier.net.default_reason")
                            : Component.literal(reason);
                    return new IdeaSubmitResult("BANNED", reasonComp);
                }
                return new IdeaSubmitResult("ERROR",
                        Component.translatable("gui.joinnotifier.net.http_error", resp.statusCode()));
            } catch (Exception e) {
                return new IdeaSubmitResult("OFFLINE",
                        Component.translatable("gui.joinnotifier.net.offline"));
            }
        });
    }

    public static boolean isDefaultReason(String reason) {
        return reason == null || reason.trim().isEmpty() || reason.equals("Причина не указана");
    }

    public static CompletableFuture<Boolean> tryLicenseAdminLoginAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (isApiUrlPlaceholder()) return false;
            try {
                User user = Minecraft.getInstance().getUser();
                if (user == null) return false;

                String username = user.getName();
                String token = user.getAccessToken();
                String uuid = user.getProfileId().toString().replace("-", "");

                if (token == null || token.isEmpty() || token.equals("0")) {
                    return false;
                }

                String serverId = UUID.randomUUID().toString().replace("-", "");

                JsonObject joinBody = new JsonObject();
                joinBody.addProperty("accessToken", token);
                joinBody.addProperty("selectedProfile", uuid);
                joinBody.addProperty("serverId", serverId);

                HttpRequest joinReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/join"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(joinBody)))
                        .build();

                HttpResponse<String> joinResp = HTTP.send(joinReq, HttpResponse.BodyHandlers.ofString());
                if (joinResp.statusCode() != 204) {
                    return false;
                }

                JsonObject loginBody = new JsonObject();
                loginBody.addProperty("mode", "license");
                loginBody.addProperty("username", username);
                loginBody.addProperty("serverId", serverId);

                HttpRequest loginReq = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "/admin/login"))
                        .timeout(Duration.ofSeconds(6))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(loginBody)))
                        .build();

                HttpResponse<String> loginResp = HTTP.send(loginReq, HttpResponse.BodyHandlers.ofString());
                if (loginResp.statusCode() == 200) {
                    JsonObject obj = JsonParser.parseString(loginResp.body()).getAsJsonObject();
                    if (obj.has("ok") && obj.get("ok").getAsBoolean()) {
                        adminToken = obj.get("token").getAsString();
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public static CompletableFuture<LoginResult> loginWithPasswordAsync(String password) {
        return CompletableFuture.supplyAsync(() -> {
            if (isApiUrlPlaceholder()) {
                return new LoginResult(false, Component.translatable("gui.joinnotifier.net.placeholder"));
            }
            try {
                JsonObject loginBody = new JsonObject();
                loginBody.addProperty("mode", "password");
                loginBody.addProperty("password", password);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "/admin/login"))
                        .timeout(Duration.ofSeconds(6))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(loginBody)))
                        .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
                    if (obj.has("ok") && obj.get("ok").getAsBoolean()) {
                        adminToken = obj.get("token").getAsString();
                        return new LoginResult(true, Component.empty());
                    }
                }
                if (resp.statusCode() == 403) {
                    return new LoginResult(false, Component.translatable("gui.joinnotifier.net.wrong_password"));
                }
                if (resp.statusCode() == 500) {
                    return new LoginResult(false, Component.translatable("gui.joinnotifier.net.no_db"));
                }
                return new LoginResult(false,
                        Component.translatable("gui.joinnotifier.net.http_error", resp.statusCode()));
            } catch (Exception e) {
                return new LoginResult(false, Component.translatable("gui.joinnotifier.net.offline"));
            }
        });
    }

    public static boolean hasAdminSession() {
        return adminToken != null && !adminToken.isEmpty();
    }

    public static CompletableFuture<AdminData> fetchAdminDataAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!hasAdminSession()) {
                    return new AdminData(false, List.of(), List.of(), List.of(), List.of());
                }
                JsonObject body = new JsonObject();
                body.addProperty("action", "get_all");

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "/admin/action"))
                        .timeout(Duration.ofSeconds(6))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .header("Authorization", "Bearer " + adminToken)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                        .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    return new AdminData(false, List.of(), List.of(), List.of(), List.of());
                }

                JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
                List<IdeaItem> ideas = new ArrayList<>();
                for (JsonElement el : obj.getAsJsonArray("ideas")) {
                    ideas.add(GSON.fromJson(el, IdeaItem.class));
                }

                List<BanItem> banned = new ArrayList<>();
                for (JsonElement el : obj.getAsJsonArray("bannedUsers")) {
                    if (el.isJsonObject()) {
                        banned.add(GSON.fromJson(el, BanItem.class));
                    } else {
                        BanItem b = new BanItem();
                        b.name = el.getAsString();
                        b.reason = "";
                        banned.add(b);
                    }
                }

                List<String> servers = toStringList(obj.getAsJsonArray("blockedServers"));
                List<String> admins = toStringList(obj.getAsJsonArray("adminWhitelist"));

                blockedServers.clear();
                for (String s : servers) blockedServers.add(normalizeIp(s));
                saveCachedBlockedServers();

                return new AdminData(true, ideas, banned, servers, admins);
            } catch (Exception e) {
                return new AdminData(false, List.of(), List.of(), List.of(), List.of());
            }
        });
    }

    public static CompletableFuture<Boolean> sendAdminActionAsync(String action, String id, String target, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!hasAdminSession()) return false;
                JsonObject body = new JsonObject();
                body.addProperty("action", action);
                if (id != null) body.addProperty("id", id);
                if (target != null) body.addProperty("target", target);
                if (reason != null) body.addProperty("reason", reason);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "/admin/action"))
                        .timeout(Duration.ofSeconds(6))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .header("Authorization", "Bearer " + adminToken)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                        .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                return resp.statusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }

    private static List<String> toStringList(JsonArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (JsonElement el : array) list.add(el.getAsString());
        }
        return list;
    }
}
