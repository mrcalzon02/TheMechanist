package mechanist.input;

import java.util.Objects;

/**
 * Immutable remappable binding row for a single abstract game command.
 */
public record KeyBind(String commandId, String displayName, InputToken token) {
    public KeyBind {
        commandId = Objects.requireNonNull(commandId, "commandId").trim();
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        Objects.requireNonNull(token, "token");
        if (commandId.isEmpty()) {
            throw new IllegalArgumentException("Command id cannot be blank.");
        }
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be blank.");
        }
    }

    public KeyBind withToken(InputToken newToken) {
        return new KeyBind(commandId, displayName, Objects.requireNonNull(newToken, "newToken"));
    }
}
