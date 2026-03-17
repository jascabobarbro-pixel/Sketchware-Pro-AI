package pro.sketchware.ai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages AI agent settings persistence
 */
public class AISettingsManager {
    private static final String PREF_NAME = "ai_settings";
    private final SharedPreferences prefs;

    public AISettingsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setApiKey(AIAgentManager.Provider provider, String key) {
        prefs.edit().putString("key_" + provider.name(), key).apply();
    }

    public String getApiKey(AIAgentManager.Provider provider) {
        return prefs.getString("key_" + provider.name(), "");
    }

    public void setDefaultProvider(AIAgentManager.Provider provider) {
        prefs.edit().putString("default_provider", provider.name()).apply();
    }

    public AIAgentManager.Provider getDefaultProvider() {
        String name = prefs.getString("default_provider", AIAgentManager.Provider.OPENAI.name());
        try {
            return AIAgentManager.Provider.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AIAgentManager.Provider.OPENAI;
        }
    }

    public void setDefaultModel(String model) {
        prefs.edit().putString("default_model", model).apply();
    }

    public String getDefaultModel() {
        return prefs.getString("default_model", "gpt-4o-mini");
    }

    public void setAutoSuggest(boolean enabled) {
        prefs.edit().putBoolean("auto_suggest", enabled).apply();
    }

    public boolean isAutoSuggestEnabled() {
        return prefs.getBoolean("auto_suggest", true);
    }

    public void setCustomEndpoint(String url) {
        prefs.edit().putString("custom_endpoint", url).apply();
    }

    public String getCustomEndpoint() {
        return prefs.getString("custom_endpoint", "");
    }
}
