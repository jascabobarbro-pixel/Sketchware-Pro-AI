package pro.sketchware.ai;

public class AIChatMessage {
    public enum Role {
        USER, ASSISTANT, SYSTEM, ERROR
    }

    private final Role role;
    private final String content;
    private final long timestamp;

    public AIChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public Role getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}
