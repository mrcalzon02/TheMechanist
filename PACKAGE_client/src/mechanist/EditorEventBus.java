package mechanist;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Lightweight Swing-safe event bus for editor views that must not directly own simulation models. */
final class EditorEventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<? super EditorEvent>>> listeners = new ConcurrentHashMap<>();

    <T extends EditorEvent> AutoCloseable subscribe(Class<T> type, Consumer<T> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        Consumer<? super EditorEvent> bridge = event -> {
            if (type.isInstance(event)) listener.accept(type.cast(event));
        };
        listeners.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>()).add(bridge);
        return () -> {
            List<Consumer<? super EditorEvent>> bucket = listeners.get(type);
            if (bucket != null) bucket.remove(bridge);
        };
    }

    void publish(EditorEvent event) {
        if (event == null) return;
        Runnable delivery = () -> {
            for (Map.Entry<Class<?>, CopyOnWriteArrayList<Consumer<? super EditorEvent>>> entry : listeners.entrySet()) {
                if (!entry.getKey().isInstance(event)) continue;
                for (Consumer<? super EditorEvent> listener : entry.getValue()) listener.accept(event);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) delivery.run();
        else SwingUtilities.invokeLater(delivery);
    }
}

sealed interface EditorEvent permits EditorEvent.StatusChanged, EditorEvent.ModelChanged, EditorEvent.SelectionChanged, EditorEvent.DeploymentProgress, EditorEvent.DeploymentFinished, EditorEvent.DeploymentFailed {
    record StatusChanged(String message) implements EditorEvent { }
    record ModelChanged(String editorName, String entityId, String propertyName, Object newValue) implements EditorEvent { }
    record SelectionChanged(String editorName, String entityId) implements EditorEvent { }
    record DeploymentProgress(String stage, int percent, String detail) implements EditorEvent { }
    record DeploymentFinished(String summary, java.nio.file.Path outputPath) implements EditorEvent { }
    record DeploymentFailed(String summary, String detail) implements EditorEvent { }
}
