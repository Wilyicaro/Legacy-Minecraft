package wily.legacy.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.datafixers.util.Pair;
import com.sun.net.httpserver.HttpServer;
import io.netty.channel.ConnectTimeoutException;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.screen.ChooseUserScreen;
import wily.legacy.client.screen.LegacyLoadingScreen;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static wily.legacy.Legacy4J.LOGGER;


/**
 * Code adapted from <a href="https://github.com/axieum/authme/blob/main/src/main/java/me/axieum/mcmod/authme/api/util/MicrosoftUtils.java">...</a>
 * <p>
 * An interface used as storage for user profile and login information, containing Microsoft Account authentication methods
 *
 * <p>For more information refer to:
 * <a href="https://wiki.vg/Microsoft_Authentication_Scheme">https://wiki.vg/Microsoft_Authentication_Scheme</a>
 */
public interface MCAccount {
    String MC_ACCESS_TOKEN = "mcAccessToken";
    String MSA_REFRESH_TOKEN = "msaRefreshToken";
    String ENCRYPTED = "encrypted";
    String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    Component LOGIN_IN = Component.translatable("legacy.menu.choose_user.login_in");
    Component ACQUIRING_MSAUTH_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringMSAuthCode");
    Component ACQUIRING_MSACCESS_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringMSAccessToken");
    Component ACQUIRING_XBOX_ACCESS_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringXboxAccessToken");
    Component ACQUIRING_XBOX_XSTS_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringXboxXstsToken");
    Component ACQUIRING_MC_ACCESS_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringMCAccessToken");
    Component FINALIZING = Component.translatable("legacy.menu.choose_user.stage.finalizing");

    Path ACCOUNTS_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve(".accounts.json");

    List<MCAccount> list = new ArrayList<>();

    GameProfile getProfile();

    default boolean isEncrypted(){
        return false;
    }

    default Optional<String> getToken(@Nullable String password, String tokenEntry){
        return Optional.empty();
    }
    default Optional<String> getMCAccessToken(@Nullable String password){
        return getToken(password, MC_ACCESS_TOKEN);
    }
    default Optional<String> getMSARefreshToken(@Nullable String password){
        return getToken(password, MSA_REFRESH_TOKEN);
    }

    default void serialize(JsonObject object) {
        serializeProfile(getProfile(),object);
    }
    default void login(ChooseUserScreen screen, @Nullable String password){
        login(()->{
            screen.reloadAccountButtons();
            Minecraft.getInstance().setScreen(screen);
        },password);
    }
    default void login(Runnable onClose, @Nullable String password){
        MCAccount.setUser(new User(getProfile().getName(),getProfile().getId()/*? if <=1.20.2 {*//*.toString()*//*?}*/,"invalidtoken", Optional.empty(),Optional.empty(),User.Type.LEGACY));
        onClose.run();
    }

    static void loadAll(){
        list.clear();
        if (!Files.exists(ACCOUNTS_PATH)) return;
        try (BufferedReader r = Files.newBufferedReader(ACCOUNTS_PATH, Charsets.UTF_8)) {
            GsonHelper.parseArray(r).forEach(e-> list.add(deserialize(e.getAsJsonObject())));
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Failed to load the saved accounts",e);
        }
    }
    static void saveAll(){
        if (list.isEmpty()) return;
        try (JsonWriter w = new JsonWriter(Files.newBufferedWriter(ACCOUNTS_PATH, Charsets.UTF_8))){
            w.setSerializeNulls(false);
            w.setIndent("  ");
            JsonArray array = new JsonArray();
            list.forEach(a->{
                JsonObject obj = new JsonObject();
                a.serialize(obj);
                array.add(obj);
            });
            GsonHelper.writeValue(w,array,null);
            if (Util.getPlatform().equals(Util.OS.WINDOWS)) Files.setAttribute(ACCOUNTS_PATH, "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Failed to write the saved accounts",e);
        }
    }

    static String encryptToken(String password, String token){
        if (password == null) return token;
        try {
            byte[] salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            byte[] iv = new byte[16];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getKeyFromPassword(password, salt), new IvParameterSpec(iv));

            byte[] encrypted = cipher.doFinal(token.getBytes());

            byte[] encryptedWithSaltAndIv = new byte[salt.length + iv.length + encrypted.length];
            System.arraycopy(salt, 0, encryptedWithSaltAndIv, 0, salt.length);
            System.arraycopy(iv, 0, encryptedWithSaltAndIv, salt.length, iv.length);
            System.arraycopy(encrypted, 0, encryptedWithSaltAndIv, salt.length + iv.length, encrypted.length);


            return Base64.getEncoder().encodeToString(encryptedWithSaltAndIv);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | BadPaddingException | InvalidKeyException e) {
            return null;
        }
    }

    static MCAccount deserialize(JsonObject object){
        return create(deserializeProfile(object),GsonHelper.getAsBoolean(object, ENCRYPTED,false),GsonHelper.getAsString(object, MC_ACCESS_TOKEN,null), GsonHelper.getAsString(object, MSA_REFRESH_TOKEN,null));
    }

    static void serializeProfile(GameProfile profile, JsonObject object){
        object.addProperty("id",profile.getId().toString());
        object.addProperty("name",profile.getName());
    }
    static GameProfile deserializeProfile(JsonObject object){
        return new GameProfile(UUID.fromString(object.get("id").getAsString()),object.get("name").getAsString());
    }

    static SecretKeySpec getKeyFromPassword(String password, byte[] salt) {
        try {
            return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(password.toCharArray(), salt, 65536, 256)).getEncoded(), "AES");
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    static MCAccount create(GameProfile profile, boolean encrypted,  String mcAccessToken, String msaRefreshToken){
        if (mcAccessToken == null || msaRefreshToken == null) return ()-> profile;
        CompletableFuture<GameProfile> result = CompletableFuture.supplyAsync(() -> /*? if >1.20.2 {*/Minecraft.getInstance().getMinecraftSessionService().fetchProfile(profile.getId(), true).profile(), Util.nonCriticalIoPool()/*?} else {*//*Minecraft.getInstance().getMinecraftSessionService().fillProfileProperties(profile, false)*//*?}*/);
        return new MCAccount() {
            @Override
            public GameProfile getProfile() {
                return result.getNow(profile);
            }

            @Override
            public boolean isEncrypted() {
                return encrypted;
            }

            @Override
            public void login(Runnable onClose, @Nullable String password) {
                if (password == null && isEncrypted()) return;
                getMCAccessToken(password).ifPresent(s-> {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    LegacyLoadingScreen screen = MCAccount.prepareLoginInScreen(onClose, executor);
                    Minecraft.getInstance().setScreen(screen);
                    MCAccount.login(s, executor).thenAccept(u -> {
                        Minecraft.getInstance().execute(onClose);
                        setUser(u);
                    }).exceptionally(t -> {
                        getMSARefreshToken(password).ifPresent(re -> {
                            Stocker<String> refresh = Stocker.of(re);
                            MCAccount.login(screen, re, password, refresh, executor).thenAccept(user -> {
                                setUser(user);
                                list.set(list.indexOf(this), create(getProfile(), isEncrypted(), encryptToken(password, user.getAccessToken()), encryptToken(password, refresh.get())));
                                Minecraft.getInstance().execute(onClose);
                            });
                        });
                        return null;
                    });
                });
            }

            @Override
            public void serialize(JsonObject object) {
                MCAccount.super.serialize(object);
                object.addProperty("encrypted",isEncrypted());
                getMCAccessToken(null).ifPresent(t-> object.addProperty(MC_ACCESS_TOKEN,t));
                getMSARefreshToken(null).ifPresent(t-> object.addProperty(MSA_REFRESH_TOKEN,t));
            }

            @Override
            public Optional<String> getToken(@Nullable String password, String tokenEntry) {
                Optional<String> token = Optional.ofNullable(tokenEntry.equals(MC_ACCESS_TOKEN) ? mcAccessToken : tokenEntry.equals(MSA_REFRESH_TOKEN) ? msaRefreshToken : null);
                if (token.isEmpty() || password == null) return token;
                try {
                    byte[] decodedData = Base64.getDecoder().decode(token.get());

                    byte[] salt = new byte[16];
                    byte[] iv = new byte[16];
                    byte[] encrypted = new byte[decodedData.length - salt.length - iv.length];
                    System.arraycopy(decodedData, 0, salt, 0, salt.length);
                    System.arraycopy(decodedData, salt.length, iv, 0, iv.length);
                    System.arraycopy(decodedData, salt.length + iv.length, encrypted, 0, encrypted.length);

                    Cipher c = Cipher.getInstance(TRANSFORMATION);
                    c.init(Cipher.DECRYPT_MODE,getKeyFromPassword(password, salt),new IvParameterSpec(iv));
                    return Optional.of(new String(c.doFinal(encrypted)));
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
                    FactoryAPIClient.getToasts().addToast(new LegacyTip(Component.translatable("legacy.menu.choose_user.failed", Component.translatable("legacy.menu.choose_user.failed.incorrect_password").withStyle(ChatFormatting.RED)), 140, 46).centered());
                    return Optional.empty();
                }
            }
        };
    }

    // A reusable Apache HTTP request config
    // NB: We use Apache's HTTP implementation as the native HTTP client does
    //     not appear to free its resources after use!
    RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectionRequestTimeout(30_000).setConnectTimeout(30_000).setSocketTimeout(30_000).build();


    // Default URLs used in the configuration.
    String CLIENT_ID = "2f63b52c-2aeb-4f21-a753-12adfd4ef9fc";
    String AUTHORIZE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    String XBOX_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    String MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    Stocker<Long> lastSessionCheckTime = Stocker.of(0L);
    Stocker<Boolean> lastSessionCheck = Stocker.of(null);


    /**
     * Navigates to the Microsoft login, and listens for a successful login
     * callback.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param browserMessage function that takes true if success, and returns
     *                       a message to be shown in the browser after
     *                       logging in
     * @param executor       executor to run the login task on
     * @return completable future for the Microsoft auth token
     * @see #acquireMSAuthCode(Consumer, Function, Executor)
     */
    static CompletableFuture<String> acquireMSAuthCode(final Function<Boolean, @NotNull String> browserMessage, final Executor executor) {
        return acquireMSAuthCode(Util.getPlatform()::openUri, browserMessage, executor);
    }

    /**
     * Navigates to the Microsoft login with user interaction, and listens for
     * a successful login callback.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param browserMessage function that takes true if success, and returns
     *                       a message to be shown in the browser after
     *                       logging in
     * @param executor       executor to run the login task on
     * @param prompt         optional Microsoft interaction prompt override
     * @return completable future for the Microsoft auth token
     * @see #acquireMSAuthCode(Consumer, Function, Executor)
     */
    static CompletableFuture<String> acquireMSAuthCode(final Function<Boolean, @NotNull String> browserMessage, final Executor executor, final @Nullable MicrosoftPrompt prompt) {
        return acquireMSAuthCode(Util.getPlatform()::openUri, browserMessage, executor, prompt);
    }

    /**
     * Generates a Microsoft login link, triggers the given browser action, and
     * listens for a successful login callback.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param browserAction  consumer that opens the generated login url
     * @param browserMessage function that takes true if success, and returns
     *                       a message to be shown in the browser after
     *                       logging in
     * @param executor       executor to run the login task on
     * @return completable future for the Microsoft auth token
     */
    static CompletableFuture<String> acquireMSAuthCode(final Consumer<URI> browserAction, final Function<Boolean, @NotNull String> browserMessage, final Executor executor) {
        return acquireMSAuthCode(browserAction, browserMessage, executor, null);
    }

    /**
     * Generates a Microsoft login link with user interaction, triggers the
     * given browser action, and listens for a successful login callback.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param browserAction  consumer that opens the generated login url
     * @param browserMessage function that takes true if success, and returns
     *                       a message to be shown in the browser after
     *                       logging in
     * @param executor       executor to run the login task on
     * @param prompt         optional Microsoft interaction prompt override
     * @return completable future for the Microsoft auth token
     */
    static CompletableFuture<String> acquireMSAuthCode(final Consumer<URI> browserAction, final Function<Boolean, @NotNull String> browserMessage, final Executor executor, final @Nullable MicrosoftPrompt prompt) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Acquiring Microsoft auth code...");
            try {
                // Generate a random "state" to be included in the request that will in turn be returned with the token
                final String state = RandomStringUtils.randomAlphanumeric(8);

                // Prepare a temporary HTTP server we can listen for the OAuth2 callback on
                final HttpServer server = HttpServer.create(new InetSocketAddress(25585), 0);
                final CountDownLatch latch = new CountDownLatch(1); // track when a request has been handled
                final AtomicReference<@Nullable String> authCode = new AtomicReference<>(null), errorMsg = new AtomicReference<>(null);

                server.createContext("/callback", exchange -> {
                    // Parse the query parameters
                    final Map<String, String> query = URLEncodedUtils.parse(exchange.getRequestURI(), StandardCharsets.UTF_8).stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

                    // Check the returned parameter values
                    if (!state.equals(query.get("state"))) {
                        // The "state" does not match what we sent
                        errorMsg.set(String.format("State mismatch! Expected '%s' but got '%s'.", state, query.get("state")));
                    } else if (query.containsKey("code")) {
                        // Successfully matched the auth code
                        authCode.set(query.get("code"));
                    } else if (query.containsKey("error")) {
                        // Otherwise, try to find an error description
                        errorMsg.set(String.format("%s: %s", query.get("error"), query.get("error_description")));
                    }

                    // Send a response informing that the browser may now be closed
                    final byte[] message = browserMessage.apply(errorMsg.get() == null).getBytes();
                    exchange.sendResponseHeaders(200, message.length);
                    final OutputStream res = exchange.getResponseBody();
                    res.write(message);
                    res.close();

                    // Let the caller thread know that the request has been handled
                    latch.countDown();
                });

                // Build a Microsoft login url
                final URIBuilder uriBuilder = new URIBuilder(AUTHORIZE_URL)
                    .addParameter("client_id", CLIENT_ID)
                    .addParameter("response_type", "code")
                    .addParameter(
                        "redirect_uri", String.format("http://localhost:%d/callback", server.getAddress().getPort())
                    )
                    .addParameter("scope", "XboxLive.signin offline_access")
                    .addParameter("state", state);
                if (prompt != null) uriBuilder.addParameter("prompt", prompt.toString());

                final URI uri = uriBuilder.build();

                // Navigate to the Microsoft login in browser
                LOGGER.info("Launching Microsoft login in browser: {}", uri.toString());
                browserAction.accept(uri);

                try {
                    // Start the HTTP server
                    LOGGER.info("Begin listening on http://localhost:{}/callback for a successful Microsoft login...", server.getAddress().getPort());
                    server.start();

                    // Wait for the server to stop and return the auth code, if any captured
                    latch.await();

                    return Optional.ofNullable(authCode.get())
                                   .filter(code -> !code.isBlank())
                                   // If present, log success and return
                                   .map(code -> {
                                       LOGGER.info("Acquired Microsoft auth code! ({})",
                                           StringUtils.abbreviateMiddle(code, "...", 32));
                                       return code;
                                   })
                                   // Otherwise, throw an exception with the error description if present
                                   .orElseThrow(() -> new Exception(
                                       Optional.ofNullable(errorMsg.get())
                                               .orElse("There was no auth code or error description present.")
                                   ));
                } finally {
                    // Always release the server!
                    server.stop(2);
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Microsoft auth code acquisition was cancelled!");
                throw new CancellationException("Interrupted");
            } catch (Exception e) {
                LOGGER.error("Unable to acquire Microsoft auth code!", e);
                throw new CompletionException(e);
            }
        }, executor);
    }
    /**
     * Exchanges a Microsoft auth code for an access token.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param code Microsoft auth code
     * @param executor executor to run the login task on
     * @return completable future for the Microsoft access token
     */
    static CompletableFuture<Pair<String,String>> acquireMSAccessToken(final String code, final Executor executor) {
        return acquireMSAccessToken(code,false,executor);
    }
    /**
     * Exchanges a Microsoft auth code or a refresh token for an access token.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param code Microsoft auth code or a refresh token
     * @param executor executor to run the login task on
     * @return completable future for the Microsoft access token
     */
    static CompletableFuture<Pair<String,String>> acquireMSAccessToken(final String code, boolean isRefreshToken, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Exchanging Microsoft auth code for an access token...");
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                // Build a new HTTP request
                final HttpPost request = new HttpPost(URI.create(TOKEN_URL));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setEntity(new UrlEncodedFormEntity(List.of(
                        new BasicNameValuePair("client_id", CLIENT_ID),
                        new BasicNameValuePair("grant_type", isRefreshToken ? "refresh_token" : "authorization_code"),
                        new BasicNameValuePair(isRefreshToken ? "refresh_token" : "code", code),
                        // We must provide the exact redirect URI that was used to obtain the auth code
                        new BasicNameValuePair("redirect_uri", String.format("http://localhost:%d/callback", 25585))
                    ),
                    "UTF-8"
                ));

                // Send the request on the HTTP client
                LOGGER.info("[{}] {} (timeout={}s)", request.getMethod(), request.getURI().toString(), request.getConfig().getConnectTimeout() / 1000);
                final org.apache.http.HttpResponse res = client.execute(request);

                // Attempt to parse the response body as JSON and extract the access token
                final JsonObject json = GsonHelper.parse(EntityUtils.toString(res.getEntity()));
                return Pair.of(Optional.ofNullable(json.get("access_token"))
                               .map(JsonElement::getAsString)
                               .filter(token -> !token.isBlank())
                               // If present, log success and return
                               .map(token -> {
                                   LOGGER.info("Acquired Microsoft access token! ({})",
                                       StringUtils.abbreviateMiddle(token, "...", 32));
                                   return token;
                               })
                               // Otherwise, throw an exception with the error description if present
                               .orElseThrow(() -> new Exception(
                                   json.has("error") ? String.format(
                                       "%s: %s",
                                       json.get("error").getAsString(),
                                       json.get("error_description").getAsString()
                                   ) : "There was no access token or error description present."
                               )),json.get("refresh_token").getAsString());
            } catch (InterruptedException e) {
                LOGGER.warn("Microsoft access token acquisition was cancelled!");
                throw new CancellationException("Interrupted");
            } catch (Exception e) {
                LOGGER.error("Unable to acquire Microsoft access token!", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Exchanges a Microsoft access token for an Xbox Live access token.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param accessToken Microsoft access token
     * @param executor    executor to run the login task on
     * @return completable future for the Xbox Live access token
     */
    static CompletableFuture<String> acquireXboxAccessToken(final String accessToken, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Exchanging Microsoft access token for an Xbox Live access token...");
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                // Build a new HTTP request
                final HttpPost request = new HttpPost(URI.create(XBOX_AUTH_URL));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(
                    String.format("""
                        {
                          "Properties": {
                            "AuthMethod": "RPS",
                            "SiteName": "user.auth.xboxlive.com",
                            "RpsTicket": "d=%s"
                          },
                          "RelyingParty": "http://auth.xboxlive.com",
                          "TokenType": "JWT"
                        }""", accessToken)
                ));

                // Send the request on the HTTP client
                LOGGER.info("[{}] {} (timeout={}s)",
                    request.getMethod(), request.getURI().toString(), request.getConfig().getConnectTimeout() / 1000);
                final org.apache.http.HttpResponse res = client.execute(request);

                // Attempt to parse the response body as JSON and extract the access token
                // NB: No response body is sent if the response is not ok
                final JsonObject json = res.getStatusLine().getStatusCode() == 200
                                        ? GsonHelper.parse(EntityUtils.toString(res.getEntity()))
                                        : new JsonObject();
                return Optional.ofNullable(json.get("Token"))
                               .map(JsonElement::getAsString)
                               .filter(token -> !token.isBlank())
                               // If present, log success and return
                               .map(token -> {
                                   LOGGER.info("Acquired Xbox Live access token! ({})",
                                       StringUtils.abbreviateMiddle(token, "...", 32));
                                   return token;
                               })
                               // Otherwise, throw an exception with the error description if present
                               .orElseThrow(() -> new Exception(
                                   json.has("XErr") ? String.format(
                                       "%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()
                                   ) : "There was no access token or error description present."
                               ));
            } catch (InterruptedException e) {
                LOGGER.warn("Xbox Live access token acquisition was cancelled!");
                throw new CancellationException("Interrupted");
            } catch (Exception e) {
                LOGGER.error("Unable to acquire Xbox Live access token!", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Exchanges an Xbox Live access token for an Xbox Live XSTS (security
     * token service) token.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param accessToken Xbox Live access token
     * @param executor    executor to run the login task on
     * @return completable future for a mapping of Xbox Live XSTS token ("Token") and user hash ("uhs")
     */
    static CompletableFuture<Map<String, String>> acquireXboxXstsToken(final String accessToken, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Exchanging Xbox Live token for an Xbox Live XSTS token...");
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                // Build a new HTTP request
                final HttpPost request = new HttpPost(URI.create(XBOX_XSTS_URL));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(
                    String.format("""
                        {
                          "Properties": {
                            "SandboxId": "RETAIL",
                            "UserTokens": ["%s"]
                          },
                          "RelyingParty": "rp://api.minecraftservices.com/",
                          "TokenType": "JWT"
                        }""", accessToken)
                ));

                // Send the request on the HTTP client
                LOGGER.info("[{}] {} (timeout={}s)",
                    request.getMethod(), request.getURI().toString(), request.getConfig().getConnectTimeout() / 1000);
                final org.apache.http.HttpResponse res = client.execute(request);

                // Attempt to parse the response body as JSON and extract the access token and user hash
                // NB: No response body is sent if the response is not ok
                final JsonObject json = res.getStatusLine().getStatusCode() == 200
                                        ? GsonHelper.parse(EntityUtils.toString(res.getEntity()))
                                        : new JsonObject();
                return Optional.ofNullable(json.get("Token"))
                               .map(JsonElement::getAsString)
                               .filter(token -> !token.isBlank())
                               // If present, extract the user hash, log success and return
                               .map(token -> {
                                   // Extract the user hash
                                   final String uhs = json.get("DisplayClaims").getAsJsonObject()
                                                          .get("xui").getAsJsonArray()
                                                          .get(0).getAsJsonObject()
                                                          .get("uhs").getAsString();
                                   // Return an immutable mapping of the token and user hash
                                   LOGGER.info("Acquired Xbox Live XSTS token! (token={}, uhs={})",
                                       StringUtils.abbreviateMiddle(token, "...", 32), uhs);
                                   return Map.of("Token", token, "uhs", uhs);
                               })
                               // Otherwise, throw an exception with the error description if present
                               .orElseThrow(() -> new Exception(
                                   json.has("XErr") ? String.format(
                                       "%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()
                                   ) : "There was no access token or error description present."
                               ));
            } catch (InterruptedException e) {
                LOGGER.warn("Xbox Live XSTS token acquisition was cancelled!");
                throw new CancellationException("Interrupted");
            } catch (Exception e) {
                LOGGER.error("Unable to acquire Xbox Live XSTS token!", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Exchanges an Xbox Live XSTS token for a Minecraft access token.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param xstsToken Xbox Live XSTS token
     * @param userHash  Xbox Live user hash
     * @param executor  executor to run the login task on
     * @return completable future for the Minecraft access token
     */
    static CompletableFuture<String> acquireMCAccessToken(final String xstsToken, final String userHash, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Exchanging Xbox Live XSTS token for a Minecraft access token...");
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                // Build a new HTTP request
                final HttpPost request = new HttpPost(URI.create(MC_AUTH_URL));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(
                    String.format("{\"identityToken\": \"XBL3.0 x=%s;%s\"}", userHash, xstsToken)
                ));

                // Send the request on the HTTP client
                LOGGER.info("[{}] {} (timeout={}s)",
                    request.getMethod(), request.getURI().toString(), request.getConfig().getConnectTimeout() / 1000);
                final org.apache.http.HttpResponse res = client.execute(request);

                // Attempt to parse the response body as JSON and extract the access token
                final JsonObject json = GsonHelper.parse(EntityUtils.toString(res.getEntity()));
                return Optional.ofNullable(json.get("access_token"))
                               .map(JsonElement::getAsString)
                               .filter(token -> !token.isBlank())
                               // If present, log success and return
                               .map(token -> {
                                   LOGGER.info("Acquired Minecraft access token! ({})",
                                       StringUtils.abbreviateMiddle(token, "...", 32));
                                   return token;
                               })
                               // Otherwise, throw an exception with the error description if present
                               .orElseThrow(() -> new Exception(
                                   json.has("error") ? String.format(
                                       "%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()
                                   ) : "There was no access token or error description present."
                               ));
            } catch (InterruptedException e) {
                LOGGER.warn("Minecraft access token acquisition was cancelled!");
                throw new CancellationException("Interrupted");
            } catch (Exception e) {
                LOGGER.error("Unable to acquire Minecraft access token!", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    static LegacyLoadingScreen prepareLoginInScreen(Runnable onClose, ExecutorService executor){
        return new LegacyLoadingScreen(LOGIN_IN,Component.empty()){
            @Override
            public void onClose() {
                onClose.run();
                executor.shutdown();
                boolean bl;
                try {
                    bl = executor.awaitTermination(3L, TimeUnit.SECONDS);
                } catch (InterruptedException var3) {
                    bl = false;
                }

                if (!bl) {
                    executor.shutdownNow();
                }
            }

            @Override
            public boolean shouldCloseOnEsc() {
                return true;
            }
        };
    }
    static CompletableFuture<User> login(LegacyLoadingScreen screen, String code, String password, Stocker<String> refresh, Executor executor){
        CompletableFuture<User> login = updateStage(MCAccount.acquireMSAccessToken(code,refresh.get() != null,executor), screen,ACQUIRING_MSACCESS_TOKEN,20).
                thenComposeAsync(s-> {
                    refresh.set(encryptToken(password,s.getSecond()));
                    return updateStage(MCAccount.acquireXboxAccessToken(s.getFirst(), executor), screen, ACQUIRING_XBOX_ACCESS_TOKEN, 40);
                }).
                thenComposeAsync(s-> updateStage(MCAccount.acquireXboxXstsToken(s,executor),screen,ACQUIRING_XBOX_XSTS_TOKEN,60)).
                thenComposeAsync(s-> updateStage(MCAccount.acquireMCAccessToken(s.get("Token"),s.get("uhs"),executor),screen,ACQUIRING_MC_ACCESS_TOKEN,80)).
                thenComposeAsync(s-> updateStage(MCAccount.login(s,executor),screen,FINALIZING,100));

        login.exceptionally(throwable -> {
            FactoryAPIClient.getToasts().addToast(new LegacyTip(Component.translatable("legacy.menu.choose_user.failed",Component.translatable("legacy.menu.choose_user.failed." + (throwable instanceof ConnectTimeoutException ? "timeout" : (throwable != null && throwable.getCause().getMessage().equals("NOT_FOUND: Not Found") ? "notPurchased" : "unauthorized"))).withStyle(ChatFormatting.RED)),140,46).centered());
            if (throwable != null) Legacy4J.LOGGER.error(throwable.getMessage());
            Minecraft.getInstance().executeBlocking(screen::onClose);
            return null;
        });
        return login;
    }
    static User loginFail(LegacyLoadingScreen screen, Throwable throwable) {
        FactoryAPIClient.getToasts().addToast(new LegacyTip(Component.translatable("legacy.menu.choose_user.failed", Component.translatable("legacy.menu.choose_user.failed." + (throwable instanceof ConnectTimeoutException ? "timeout" : (throwable != null && throwable.getCause().getMessage().equals("NOT_FOUND: Not Found") ? "notPurchased" : "unauthorized"))).withStyle(ChatFormatting.RED)), 140, 46).centered());
        if (throwable != null) Legacy4J.LOGGER.error(throwable.getMessage());
        Minecraft.getInstance().executeBlocking(screen::onClose);
        return null;
    }
    static CompletableFuture<MCAccount> create(Runnable onClose, String password){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        LegacyLoadingScreen screen = MCAccount.prepareLoginInScreen(onClose,executor);
        Minecraft.getInstance().setScreen(screen);
        Stocker<String> refresh = Stocker.of(null);
        return updateStage(MCAccount.acquireMSAuthCode(bol-> I18n.get(bol ? "legacy.menu.choose_user.login_successful" : "legacy.menu.choose_user.failed_login"),executor),screen,ACQUIRING_MSAUTH_TOKEN,0).thenComposeAsync(s-> login(screen,s,password,refresh,executor)).thenApplyAsync(user -> create(new GameProfile(user.getProfileId(),user.getName()),password != null,encryptToken(password,user.getAccessToken()),refresh.get()),executor);
    }

    /**
     * Fetches the Minecraft profile for the given access token, and returns a
     * new Minecraft session.
     *
     * <p>NB: You must manually interrupt the executor thread if the
     * completable future is cancelled!
     *
     * @param mcToken  Minecraft access token
     * @param executor executor to run the login task on
     * @return completable future for the new Minecraft session
     * @see MCAccount#setUser(User) to apply the new session
     */
    static CompletableFuture<User> login(final String mcToken, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Fetching Minecraft profile...");
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                // Build a new HTTP request
                final HttpGet request = new HttpGet(URI.create(MC_PROFILE_URL));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Authorization", "Bearer " + mcToken);

                // Send the request on the HTTP client
                LOGGER.info("[{}] {} (timeout={}s)",
                    request.getMethod(), request.getURI().toString(), request.getConfig().getConnectTimeout() / 1000);
                final org.apache.http.HttpResponse res = client.execute(request);

                // Attempt to parse the response body as JSON and extract the profile
                final JsonObject json = GsonHelper.parse(EntityUtils.toString(res.getEntity()));
                return Optional.ofNullable(json.get("id"))
                               .map(JsonElement::getAsString)
                               .filter(uuid -> !uuid.isBlank())
                               // Parse the UUID (without hyphens)
                               .map(uuid -> UUID.fromString(
                                   uuid.replaceFirst(
                                       "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
                                       "$1-$2-$3-$4-$5"
                                   )
                               ))
                               // If present, log success, build a new session and return
                               .map(uuid -> {
                                   LOGGER.info("Fetched Minecraft profile! (name={}, uuid={})",
                                       json.get("name").getAsString(), uuid);
                                   return new User(
                                       json.get("name").getAsString(),
                                       uuid/*? if <=1.20.2 {*//*.toString()*//*?}*/,
                                       mcToken,
                                       Optional.empty(),
                                       Optional.empty(),
                                       User.Type.MSA
                                   );
                               })
                               // Otherwise, throw an exception with the error description if present
                               .orElseThrow(() -> new Exception(
                                   json.has("error") ? String.format(
                                       "%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()
                                   ) : "There was no profile or error description present."
                               ));
            } catch (InterruptedException e) {
                LOGGER.warn("Minecraft profile fetching was cancelled!");
                throw new CancellationException("Interrupted");
            } catch (Exception e) {
                LOGGER.error("Unable to fetch Minecraft profile!", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Changes the actual Minecraft User, and replace its value assigned during game startup in different fields
     *
     * @param user New Minecraft User
     */
    static void setUser(User user){
        if (MinecraftAccessor.getInstance().setUser(user)) {
            Component success = Component.translatable("legacy.menu.choose_user.success", user.getName());
            FactoryAPIClient.getToasts().addToast(new LegacyTip(success, Minecraft.getInstance().font.width(success) + 110, 46) {
                @Override
                public void renderTip(GuiGraphics guiGraphics, int i, int j, float f, float l) {
                    super.renderTip(guiGraphics, i, j, f, l);
                    GameProfile profile = /*? if >1.20.2 {*/Minecraft.getInstance().getGameProfile()/*?} else {*//*user.getGameProfile()*//*?}*/;
                    //? if <=1.20.2
                    /*if (profile.getProperties().isEmpty()) profile.getProperties().putAll(Minecraft.getInstance().getProfileProperties());*/
                    PlayerFaceRenderer.draw(guiGraphics, Minecraft.getInstance().getSkinManager()./*? if >1.20.1 {*/getInsecureSkin/*?} else {*//*getInsecureSkinLocation*//*?}*/(profile), 7, (height() - 32) / 2, 32);
                }
            }.centered().disappearTime(2400).canRemove(()->user != Minecraft.getInstance().getUser()));
            lastSessionCheck.set(null);
        }
    }

    /**
     * Checks if the actual User have an offline account, within a 3-min interval so there are no excessive checks
     *
     * <p>Note: Because of this interval, each User change requires setting the lastSessionCheckTime value to null so that there is no erroneous check
     */
    static boolean isOfflineUser(){
        if (lastSessionCheck.get() != null && Util.getMillis() - lastSessionCheckTime.get() <= 180000) return lastSessionCheck.get();
        lastSessionCheckTime.set(Util.getMillis());
        lastSessionCheck.set(true);
        CompletableFuture.runAsync(()->{
            try {
                String server = UUID.randomUUID().toString();
                //? if <=1.20.2 {
                /*GameProfile profile = Minecraft.getInstance().getUser().getGameProfile();
                if (profile.getProperties().isEmpty()) profile.getProperties().putAll(Minecraft.getInstance().getProfileProperties());
                *///?}
                Minecraft.getInstance().getMinecraftSessionService().joinServer(/*? if >1.20.2 {*/Minecraft.getInstance().getUser().getProfileId()/*?} else {*/ /*profile*//*?}*/,Minecraft.getInstance().getUser().getAccessToken(),server);
                lastSessionCheck.set(Minecraft.getInstance().getMinecraftSessionService().hasJoinedServer(/*? if >1.20.2 {*/Minecraft.getInstance().getUser().getName()/*?} else {*/ /*profile*//*?}*/,server,null) == null);
            } catch (AuthenticationException e) {
                lastSessionCheck.set(true);
            }
        });
        return false;
    }


    static <T> CompletableFuture<T> updateStage(CompletableFuture<T> future, LegacyLoadingScreen screen, Component stage, int percentage){
        screen.setLoadingStage(stage);
        screen.setProgress(percentage);
        return future;
    }


    /**
     * Indicates the sync of user interaction that is required when requesting
     * Microsoft authorization codes.
     */
    enum MicrosoftPrompt
    {
        /**
         * Will use the default prompt, equivalent of not sending a prompt.
         */
        DEFAULT(""),

        /**
         * Will interrupt single sign-on providing account selection experience
         * listing all the accounts either in session or any remembered account
         * or an option to choose to use a different account altogether.
         */
        SELECT_ACCOUNT("select_account"),

        /**
         * Will force the user to enter their credentials on that request,
         * negating single-sign on.
         */
        LOGIN("login"),

        /**
         * Will ensure that the user isn't presented with any interactive
         * prompt whatsoever. If the request can't be completed silently via
         * single-sign on, the Microsoft identity platform will return an
         * {@code interaction_required} error.
         */
        NONE("none"),

        /**
         * Will trigger the OAuth consent dialog after the user signs in,
         * asking the user to grant permissions to the app.
         */
        CONSENT("consent");

        private final String prompt;

        /**
         * Constructs a new Microsoft Prompt enum.
         *
         * @param prompt prompt query value
         */
        MicrosoftPrompt(final String prompt)
        {
            this.prompt = prompt;
        }

        @Override
        public String toString()
        {
            return prompt;
        }
    }
}