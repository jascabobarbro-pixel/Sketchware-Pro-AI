package pro.sketchware.fragments.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import pro.sketchware.R;
import pro.sketchware.ai.AIAgentManager;
import pro.sketchware.ai.AISettingsManager;

public class AISettingsFragment extends Fragment {
    private AIAgentManager aiManager;
    private AISettingsManager settingsManager;

    private AutoCompleteTextView actvProvider, actvModel;
    private TextInputEditText etApiKey, etCustomEndpoint;
    private TextInputLayout tilCustomEndpoint;
    private MaterialSwitch switchAutoSuggest, switchAutoFix, switchCodeGen;
    private MaterialButton btnSave, btnTest;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aiManager = new AIAgentManager(requireContext());
        settingsManager = new AISettingsManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        actvProvider = view.findViewById(R.id.actvProvider);
        actvModel = view.findViewById(R.id.actvModel);
        etApiKey = view.findViewById(R.id.etApiKey);
        etCustomEndpoint = view.findViewById(R.id.etCustomEndpoint);
        tilCustomEndpoint = view.findViewById(R.id.tilCustomEndpoint);
        switchAutoSuggest = view.findViewById(R.id.switchAutoSuggest);
        switchAutoFix = view.findViewById(R.id.switchAutoFix);
        switchCodeGen = view.findViewById(R.id.switchCodeGen);
        btnSave = view.findViewById(R.id.btnSave);
        btnTest = view.findViewById(R.id.btnTest);

        setupProviderDropdown();
        loadCurrentSettings();

        btnSave.setOnClickListener(v -> saveSettings());
        btnTest.setOnClickListener(v -> testConnection());
    }

    private void setupProviderDropdown() {
        String[] providerNames = new String[AIAgentManager.Provider.values().length];
        for (int i = 0; i < providerNames.length; i++) {
            providerNames[i] = AIAgentManager.Provider.values()[i].displayName;
        }
        ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, providerNames);
        actvProvider.setAdapter(providerAdapter);

        actvProvider.setOnItemClickListener((parent, v, position, id) -> {
            AIAgentManager.Provider selected = AIAgentManager.Provider.values()[position];
            aiManager.setProvider(selected);
            updateModelDropdown(selected);
            tilCustomEndpoint.setVisibility(
                    selected == AIAgentManager.Provider.CUSTOM ? View.VISIBLE : View.GONE);
        });
    }

    private void updateModelDropdown(AIAgentManager.Provider provider) {
        String[] models = aiManager.getModelsForProvider(provider);
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, models);
        actvModel.setAdapter(modelAdapter);
        if (models.length > 0) {
            actvModel.setText(models[0], false);
        }
    }

    private void loadCurrentSettings() {
        AIAgentManager.Provider provider = settingsManager.getDefaultProvider();
        actvProvider.setText(provider.displayName, false);
        updateModelDropdown(provider);
        actvModel.setText(settingsManager.getDefaultModel(), false);
        etApiKey.setText(settingsManager.getApiKey(provider));
        etCustomEndpoint.setText(settingsManager.getCustomEndpoint());
        switchAutoSuggest.setChecked(settingsManager.isAutoSuggestEnabled());
        tilCustomEndpoint.setVisibility(
                provider == AIAgentManager.Provider.CUSTOM ? View.VISIBLE : View.GONE);
    }

    private void saveSettings() {
        AIAgentManager.Provider provider = aiManager.getProvider();
        String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString() : "";
        String model = actvModel.getText() != null ? actvModel.getText().toString() : "";

        settingsManager.setDefaultProvider(provider);
        settingsManager.setApiKey(provider, apiKey);
        settingsManager.setDefaultModel(model);
        settingsManager.setAutoSuggest(switchAutoSuggest.isChecked());

        if (provider == AIAgentManager.Provider.CUSTOM) {
            String endpoint = etCustomEndpoint.getText() != null ? etCustomEndpoint.getText().toString() : "";
            settingsManager.setCustomEndpoint(endpoint);
            aiManager.setCustomEndpoint(endpoint);
        }

        aiManager.setApiKey(apiKey);
        aiManager.setModel(model);
        aiManager.saveSettings();

        Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void testConnection() {
        String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString() : "";
        if (apiKey.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an API key first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnTest.setEnabled(false);
        btnTest.setText("Testing...");

        aiManager.setApiKey(apiKey);
        aiManager.sendMessage("Say 'Connection successful!' in one short sentence.",
                "You are a test bot. Respond with a very short confirmation.",
                new AIAgentManager.AIResponseCallback() {
                    @Override
                    public void onResponse(String response) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                btnTest.setEnabled(true);
                                btnTest.setText("Test Connection");
                                Toast.makeText(requireContext(), "✅ " + response, Toast.LENGTH_LONG).show();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                btnTest.setEnabled(true);
                                btnTest.setText("Test Connection");
                                Toast.makeText(requireContext(), "❌ " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
    }
}
