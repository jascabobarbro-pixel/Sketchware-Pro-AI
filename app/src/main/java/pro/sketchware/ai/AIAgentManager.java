package pro.sketchware.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIAgentManager {
    private static final String TAG = "AIAgentManager";
    private static final String PREF_NAME = "ai_agent_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_PROVIDER = "ai_provider";
    private static final String KEY_MODEL = "ai_model";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public enum Provider {
        OPENAI("OpenAI", "https://api.openai.com/v1/chat/completions"),
        GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"),
        CLAUDE("Anthropic Claude", "https://api.anthropic.com/v1/messages"),
        CUSTOM("Custom API", "");

        public final String displayName;
        public final String baseUrl;

        Provider(String displayName, String baseUrl) {
            this.displayName = displayName;
            this.baseUrl = baseUrl;
        }
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient client;
    private final Gson gson;
    private Provider currentProvider = Provider.OPENAI;
    private String currentModel = "gpt-4o-mini";
    private String apiKey = "";
    private String customEndpoint = "";
    private final List<JsonObject> conversationHistory = new ArrayList<>();

    public AIAgentManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        loadSettings();
    }

    private void loadSettings() {
        apiKey = prefs.getString(KEY_API_KEY, "");
        String providerName = prefs.getString(KEY_PROVIDER, Provider.OPENAI.name());
        try {
            currentProvider = Provider.valueOf(providerName);
        } catch (IllegalArgumentException e) {
            currentProvider = Provider.OPENAI;
        }
        currentModel = prefs.getString(KEY_MODEL, "gpt-4o-mini");
        customEndpoint = prefs.getString("custom_endpoint", "");
    }

    public void saveSettings() {
        prefs.edit()
                .putString(KEY_API_KEY, apiKey)
                .putString(KEY_PROVIDER, currentProvider.name())
                .putString(KEY_MODEL, currentModel)
                .putString("custom_endpoint", customEndpoint)
                .apply();
    }

    public void setProvider(Provider provider) { this.currentProvider = provider; }
    public Provider getProvider() { return currentProvider; }
    public void setModel(String model) { this.currentModel = model; }
    public String getModel() { return currentModel; }
    public void setApiKey(String key) { this.apiKey = key; }
    public String getApiKey() { return apiKey; }
    public void setCustomEndpoint(String url) { this.customEndpoint = url; }
    public String getCustomEndpoint() { return customEndpoint; }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    public void clearHistory() {
        conversationHistory.clear();
    }

    public void sendMessage(String userMessage, String systemPrompt, AIResponseCallback callback) {
        if (!isConfigured()) {
            callback.onError("API key not configured. Go to Settings > AI Agent to set it up.");
            return;
        }

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        conversationHistory.add(userMsg);

        switch (currentProvider) {
            case OPENAI:
            case CUSTOM:
                sendOpenAIFormat(systemPrompt, callback);
                break;
            case GEMINI:
                sendGeminiFormat(userMessage, systemPrompt, callback);
                break;
            case CLAUDE:
                sendClaudeFormat(systemPrompt, callback);
                break;
        }
    }

    private void sendOpenAIFormat(String systemPrompt, AIResponseCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("model", currentModel);
        body.addProperty("max_tokens", 4096);
        body.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);
        }
        for (JsonObject msg : conversationHistory) {
            messages.add(msg);
        }
        body.add("messages", messages);

        String url = currentProvider == Provider.CUSTOM ? customEndpoint : currentProvider.baseUrl;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        callback.onError("API error (" + response.code() + "): " + responseBody);
                        return;
                    }
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    String content = json.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();

                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.addProperty("content", content);
                    conversationHistory.add(assistantMsg);

                    callback.onResponse(content);
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    private void sendGeminiFormat(String userMessage, String systemPrompt, AIResponseCallback callback) {
        JsonObject body = new JsonObject();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject sysInstruction = new JsonObject();
            JsonObject parts = new JsonObject();
            parts.addProperty("text", systemPrompt);
            JsonArray partsArr = new JsonArray();
            partsArr.add(parts);
            sysInstruction.add("parts", partsArr);
            body.add("system_instruction", sysInstruction);
        }

        JsonArray contents = new JsonArray();
        for (JsonObject msg : conversationHistory) {
            JsonObject content = new JsonObject();
            String role = msg.get("role").getAsString();
            content.addProperty("role", role.equals("assistant") ? "model" : "user");
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", msg.get("content").getAsString());
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
        }
        body.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", 4096);
        generationConfig.addProperty("temperature", 0.7);
        body.add("generationConfig", generationConfig);

        String url = currentProvider.baseUrl.replace("{model}", currentModel) + "?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        callback.onError("API error (" + response.code() + "): " + responseBody);
                        return;
                    }
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    String content = json.getAsJsonArray("candidates")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("content")
                            .getAsJsonArray("parts")
                            .get(0).getAsJsonObject()
                            .get("text").getAsString();

                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.addProperty("content", content);
                    conversationHistory.add(assistantMsg);

                    callback.onResponse(content);
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    private void sendClaudeFormat(String systemPrompt, AIResponseCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("model", currentModel);
        body.addProperty("max_tokens", 4096);

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.addProperty("system", systemPrompt);
        }

        JsonArray messages = new JsonArray();
        for (JsonObject msg : conversationHistory) {
            messages.add(msg);
        }
        body.add("messages", messages);

        Request request = new Request.Builder()
                .url(currentProvider.baseUrl)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        callback.onError("API error (" + response.code() + "): " + responseBody);
                        return;
                    }
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    String content = json.getAsJsonArray("content")
                            .get(0).getAsJsonObject()
                            .get("text").getAsString();

                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.addProperty("content", content);
                    conversationHistory.add(assistantMsg);

                    callback.onResponse(content);
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    public interface AIResponseCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public String[] getModelsForProvider(Provider provider) {
        switch (provider) {
            case OPENAI:
                return new String[]{"gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo", "o1-mini", "o3-mini"};
            case GEMINI:
                return new String[]{"gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash", "gemini-2.5-pro-preview-05-06"};
            case CLAUDE:
                return new String[]{"claude-sonnet-4-20250514", "claude-3-5-haiku-20241022", "claude-3-opus-20240229"};
            case CUSTOM:
                return new String[]{"custom"};
            default:
                return new String[]{};
        }
    }
}
