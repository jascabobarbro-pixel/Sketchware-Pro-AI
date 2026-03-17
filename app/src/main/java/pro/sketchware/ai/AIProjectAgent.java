package pro.sketchware.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Agent with full access to Sketchware project files.
 * Can read, modify, generate code, assets (icons, sounds, images), and project structures.
 */
public class AIProjectAgent {
    private static final String TAG = "AIProjectAgent";
    private final Context context;
    private final AIAgentManager aiManager;

    public AIProjectAgent(Context context, AIAgentManager aiManager) {
        this.context = context;
        this.aiManager = aiManager;
    }

    // ==================== FILE OPERATIONS ====================

    public String readFile(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) return null;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    public void writeFile(String path, String content) throws IOException {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    public boolean deleteFile(String path) {
        return new File(path).delete();
    }

    public List<String> listFiles(String dirPath) {
        List<String> result = new ArrayList<>();
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    result.add(f.getAbsolutePath());
                }
            }
        }
        return result;
    }

    // ==================== PROJECT OPERATIONS ====================

    public String getProjectPath(String scId) {
        return context.getFilesDir().getAbsolutePath() + "/.sketchware/data/" + scId;
    }

    public String getProjectLogicPath(String scId) {
        return getProjectPath(scId) + "/logic";
    }

    public String getProjectViewPath(String scId) {
        return getProjectPath(scId) + "/view";
    }

    public String getProjectFilePath(String scId) {
        return getProjectPath(scId) + "/file";
    }

    public String getProjectResPath(String scId) {
        return getProjectPath(scId) + "/resource";
    }

    public String getProjectLibPath(String scId) {
        return getProjectPath(scId) + "/library";
    }

    public Map<String, String> readAllProjectFiles(String scId) throws IOException {
        Map<String, String> files = new HashMap<>();
        String basePath = getProjectPath(scId);
        readDirRecursive(new File(basePath), files);
        return files;
    }

    private void readDirRecursive(File dir, Map<String, String> files) throws IOException {
        if (!dir.exists()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                readDirRecursive(f, files);
            } else {
                try {
                    String content = readFile(f.getAbsolutePath());
                    if (content != null) {
                        files.put(f.getAbsolutePath(), content);
                    }
                } catch (Exception e) {
                    // Skip binary files
                }
            }
        }
    }

    // ==================== ASSET GENERATION ====================

    public File generateIcon(String scId, String text, int size, int bgColor, int textColor) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(textColor);
        paint.setTextSize(size * 0.4f);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);

        float x = size / 2f;
        float y = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(text, x, y, paint);

        String iconDir = getProjectResPath(scId) + "/icons";
        new File(iconDir).mkdirs();
        File iconFile = new File(iconDir, "ic_" + text.toLowerCase().replace(" ", "_") + ".png");
        try (FileOutputStream fos = new FileOutputStream(iconFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        bitmap.recycle();
        return iconFile;
    }

    public File generateAppIcon(String scId, String appName, int primaryColor) throws IOException {
        int size = 512;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Background with rounded look
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(primaryColor);
        canvas.drawRoundRect(0, 0, size, size, size * 0.2f, size * 0.2f, bgPaint);

        // Text
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size * 0.35f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        String initial = appName.length() > 0 ? appName.substring(0, Math.min(2, appName.length())).toUpperCase() : "AI";
        float x = size / 2f;
        float y = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(initial, x, y, textPaint);

        String resDir = getProjectResPath(scId);
        new File(resDir).mkdirs();
        File iconFile = new File(resDir, "app_icon.png");
        try (FileOutputStream fos = new FileOutputStream(iconFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        bitmap.recycle();
        return iconFile;
    }

    // ==================== AI-POWERED CODE OPERATIONS ====================

    public void generateCodeFromDescription(String scId, String description, AIAgentManager.AIResponseCallback callback) {
        String systemPrompt = "You are an expert Android developer integrated into Sketchware Pro. " +
                "You generate Java code for Android apps. The user describes what they want and you provide " +
                "clean, working Java code. Only output code, no explanations unless asked. " +
                "Use Android SDK APIs compatible with minSdk 26 and targetSdk 35.";

        aiManager.sendMessage(
                "Generate Java code for the following: " + description +
                "\nProject ID: " + scId,
                systemPrompt, callback
        );
    }

    public void fixCode(String code, String error, AIAgentManager.AIResponseCallback callback) {
        String systemPrompt = "You are a code debugging expert for Android/Java. " +
                "Analyze the error, fix the code, and return ONLY the corrected code.";

        aiManager.sendMessage(
                "Fix this code:\n```java\n" + code + "\n```\n\nError: " + error,
                systemPrompt, callback
        );
    }

    public void explainBlocks(String blockData, AIAgentManager.AIResponseCallback callback) {
        String systemPrompt = "You are a Sketchware Pro expert. Explain what the given block logic does " +
                "in simple terms. If asked, convert it to Java code.";

        aiManager.sendMessage(
                "Explain these Sketchware blocks:\n" + blockData,
                systemPrompt, callback
        );
    }

    public void suggestImprovements(String scId, String code, AIAgentManager.AIResponseCallback callback) {
        String systemPrompt = "You are a senior Android developer reviewing code from a Sketchware Pro project. " +
                "Suggest improvements for performance, security, and best practices. " +
                "Be specific and provide corrected code snippets.";

        aiManager.sendMessage(
                "Review and suggest improvements for this code:\n```java\n" + code + "\n```",
                systemPrompt, callback
        );
    }

    public void generateLayout(String description, AIAgentManager.AIResponseCallback callback) {
        String systemPrompt = "You are an Android UI expert. Generate Android XML layouts based on descriptions. " +
                "Use Material Design components when possible. Output only valid Android XML.";

        aiManager.sendMessage(
                "Generate an Android XML layout for: " + description,
                systemPrompt, callback
        );
    }

    public void blocksToJava(String blockJson, AIAgentManager.AIResponseCallback callback) {
        String systemPrompt = "You are a Sketchware block-to-Java converter. " +
                "Given Sketchware block JSON data, convert it to equivalent clean Java code. " +
                "Output only the Java code.";

        aiManager.sendMessage(
                "Convert these Sketchware blocks to Java:\n" + blockJson,
                systemPrompt, callback
        );
    }

    public void generateAssetDescription(String assetType, String description, AIAgentManager.AIResponseCallback callback) {
        String systemPrompt = "You are a creative assistant helping generate descriptions for app assets. " +
                "Provide detailed specifications for generating " + assetType + " assets.";

        aiManager.sendMessage(
                "I need a " + assetType + " for: " + description + "\nProvide specifications and SVG/code if possible.",
                systemPrompt, callback
        );
    }
}
