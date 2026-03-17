package pro.sketchware.ai;

public interface AIResponseCallback {
    void onResponse(String response);
    void onError(String error);
}
