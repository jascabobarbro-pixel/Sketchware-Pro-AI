package pro.sketchware.fragments.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import pro.sketchware.R;
import pro.sketchware.ai.AIChatMessage;
import pro.sketchware.ai.AIAgentManager;
import pro.sketchware.ai.AIProjectAgent;

import java.util.ArrayList;
import java.util.List;

public class AIChatFragment extends Fragment {
    private RecyclerView recyclerChat;
    private EditText editMessage;
    private FloatingActionButton btnSend;
    private Chip chipProvider, chipModel;
    private ImageButton btnSettings, btnAttach;
    private View quickActions;

    private AIChatAdapter adapter;
    private List<AIChatMessage> messages = new ArrayList<>();
    private AIAgentManager aiManager;
    private AIProjectAgent projectAgent;
    private String currentProjectId;

    public static AIChatFragment newInstance(String projectId) {
        AIChatFragment fragment = new AIChatFragment();
        Bundle args = new Bundle();
        args.putString("project_id", projectId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentProjectId = getArguments().getString("project_id", "");
        }
        aiManager = new AIAgentManager(requireContext());
        projectAgent = new AIProjectAgent(requireContext(), aiManager);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerChat = view.findViewById(R.id.recyclerChat);
        editMessage = view.findViewById(R.id.editMessage);
        btnSend = view.findViewById(R.id.btnSend);
        chipProvider = view.findViewById(R.id.chipProvider);
        chipModel = view.findViewById(R.id.chipModel);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnAttach = view.findViewById(R.id.btnAttach);
        quickActions = view.findViewById(R.id.quickActions);

        setupRecyclerView();
        setupClickListeners();
        updateProviderChips();

        if (!aiManager.isConfigured()) {
            addMessage(new AIChatMessage(AIChatMessage.Role.SYSTEM,
                    "Welcome! Please configure your AI API key in Settings to get started.\n\n" +
                    "I can help you with:\n" +
                    "• Generate code from descriptions\n" +
                    "• Fix errors in your project\n" +
                    "• Explain Sketchware blocks\n" +
                    "• Generate app icons and assets\n" +
                    "• Convert blocks to Java/Kotlin\n" +
                    "• Suggest improvements"));
        } else {
            addMessage(new AIChatMessage(AIChatMessage.Role.ASSISTANT,
                    "Hi! I'm your AI assistant for Sketchware Pro. I have full access to your project files. How can I help?"));
            quickActions.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        adapter = new AIChatAdapter(messages);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        recyclerChat.setLayoutManager(lm);
        recyclerChat.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        chipProvider.setOnClickListener(v -> showProviderSelector());
        chipModel.setOnClickListener(v -> showModelSelector());

        btnSettings.setOnClickListener(v -> {
            // Navigate to AI settings
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new AISettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnAttach.setOnClickListener(v -> showAttachOptions());

        View chipFixCode = getView().findViewById(R.id.chipFixCode);
        View chipGenerate = getView().findViewById(R.id.chipGenerate);
        View chipExplain = getView().findViewById(R.id.chipExplain);

        if (chipFixCode != null) chipFixCode.setOnClickListener(v ->
                editMessage.setText("Fix the errors in my current project"));
        if (chipGenerate != null) chipGenerate.setOnClickListener(v ->
                editMessage.setText("Generate code for "));
        if (chipExplain != null) chipExplain.setOnClickListener(v ->
                editMessage.setText("Explain how my project works"));
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        addMessage(new AIChatMessage(AIChatMessage.Role.USER, text));
        editMessage.setText("");

        String systemPrompt = buildSystemPrompt();

        aiManager.sendMessage(text, systemPrompt, new AIAgentManager.AIResponseCallback() {
            @Override
            public void onResponse(String response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        addMessage(new AIChatMessage(AIChatMessage.Role.ASSISTANT, response));
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        addMessage(new AIChatMessage(AIChatMessage.Role.ERROR, error));
                    });
                }
            }
        });

        // Add thinking indicator
        addMessage(new AIChatMessage(AIChatMessage.Role.ASSISTANT, "Thinking..."));
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI assistant embedded in Sketchware Pro, an Android app development IDE. ");
        sb.append("You have full access to the user's project files. ");
        sb.append("You can read, modify, and generate code, layouts, and assets. ");
        sb.append("The user's current project ID is: ").append(currentProjectId).append(". ");
        sb.append("Respond concisely and provide working code when asked. ");
        sb.append("For Android development, target SDK 35 (Android 15) with minSdk 26. ");
        sb.append("When generating code, use Java unless Kotlin is specifically requested. ");
        sb.append("You understand Sketchware block structures and can convert between blocks and code.");
        return sb.toString();
    }

    private void addMessage(AIChatMessage message) {
        // Remove "Thinking..." placeholder if it exists
        if (message.getRole() != AIChatMessage.Role.USER && messages.size() > 0) {
            AIChatMessage last = messages.get(messages.size() - 1);
            if (last.getContent().equals("Thinking...")) {
                messages.remove(messages.size() - 1);
                adapter.notifyItemRemoved(messages.size());
            }
        }

        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerChat.smoothScrollToPosition(messages.size() - 1);
    }

    private void showProviderSelector() {
        String[] providers = new String[AIAgentManager.Provider.values().length];
        for (int i = 0; i < providers.length; i++) {
            providers[i] = AIAgentManager.Provider.values()[i].displayName;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select AI Provider")
                .setItems(providers, (dialog, which) -> {
                    aiManager.setProvider(AIAgentManager.Provider.values()[which]);
                    aiManager.saveSettings();
                    updateProviderChips();
                })
                .show();
    }

    private void showModelSelector() {
        String[] models = aiManager.getModelsForProvider(aiManager.getProvider());
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Model")
                .setItems(models, (dialog, which) -> {
                    aiManager.setModel(models[which]);
                    aiManager.saveSettings();
                    updateProviderChips();
                })
                .show();
    }

    private void showAttachOptions() {
        String[] options = {"Attach project file", "Attach code block", "Attach screenshot", "Generate icon"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Attach")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: attachProjectFile(); break;
                        case 1: attachCodeBlock(); break;
                        case 2: Toast.makeText(requireContext(), "Screenshot capture coming soon", Toast.LENGTH_SHORT).show(); break;
                        case 3: generateIcon(); break;
                    }
                })
                .show();
    }

    private void attachProjectFile() {
        if (currentProjectId == null || currentProjectId.isEmpty()) {
            Toast.makeText(requireContext(), "No project selected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            java.util.Map<String, String> files = projectAgent.readAllProjectFiles(currentProjectId);
            StringBuilder sb = new StringBuilder("Project files:\n");
            for (java.util.Map.Entry<String, String> entry : files.entrySet()) {
                sb.append("\n--- ").append(entry.getKey()).append(" ---\n");
                String content = entry.getValue();
                if (content.length() > 500) content = content.substring(0, 500) + "...(truncated)";
                sb.append(content).append("\n");
            }
            editMessage.setText(sb.toString());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error reading project: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void attachCodeBlock() {
        editMessage.setText("Here are my current blocks:\n[Paste or select blocks here]");
    }

    private void generateIcon() {
        editMessage.setText("Generate an app icon for my project with a modern design");
    }

    private void updateProviderChips() {
        chipProvider.setText(aiManager.getProvider().displayName);
        chipModel.setText(aiManager.getModel());
    }
}
