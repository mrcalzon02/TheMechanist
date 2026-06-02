package mechanist;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Shared command runner for editor tables, forms, and packaging controls. */
final class EditorUndoRedoController {
    private final Deque<EditorCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditorCommand> redoStack = new ArrayDeque<>();
    private final EditorEventBus bus;

    EditorUndoRedoController(EditorEventBus bus) { this.bus = bus; }

    void execute(EditorCommand command) {
        if (command == null || command.isNoop()) return;
        command.apply();
        undoStack.push(command);
        redoStack.clear();
        publishStatus("Applied: " + command.description());
    }

    boolean canUndo() { return !undoStack.isEmpty(); }
    boolean canRedo() { return !redoStack.isEmpty(); }

    void undo() {
        if (undoStack.isEmpty()) { publishStatus("Undo stack is empty."); return; }
        EditorCommand command = undoStack.pop();
        command.revert();
        redoStack.push(command);
        publishStatus("Undid: " + command.description());
    }

    void redo() {
        if (redoStack.isEmpty()) { publishStatus("Redo stack is empty."); return; }
        EditorCommand command = redoStack.pop();
        command.apply();
        undoStack.push(command);
        publishStatus("Redid: " + command.description());
    }

    String compactState() { return "undo=" + undoStack.size() + " redo=" + redoStack.size(); }

    private void publishStatus(String message) {
        if (bus != null) bus.publish(new EditorEvent.StatusChanged(message + " | " + compactState()));
    }
}

sealed interface EditorCommand permits EditorCommand.PropertyChange, EditorCommand.ToggleProjectSelection, EditorCommand.CreateEntity {
    void apply();
    void revert();
    String description();
    default boolean isNoop() { return false; }

    record PropertyChange(SimulationEditorRepository repository,
                          SimulationEditorRepository.EntityRef ref,
                          String propertyName,
                          Object oldValue,
                          Object newValue,
                          EditorEventBus bus) implements EditorCommand {
        public PropertyChange {
            Objects.requireNonNull(repository, "repository");
            Objects.requireNonNull(ref, "ref");
            Objects.requireNonNull(propertyName, "propertyName");
        }
        @Override public void apply() { set(newValue); }
        @Override public void revert() { set(oldValue); }
        @Override public String description() { return ref.editorName() + " / " + ref.entityId() + " / " + propertyName; }
        @Override public boolean isNoop() { return Objects.equals(oldValue, newValue); }
        private void set(Object value) {
            repository.setProperty(ref, propertyName, value);
            if (bus != null) bus.publish(new EditorEvent.ModelChanged(ref.editorName(), ref.entityId(), propertyName, value));
        }
    }



    record CreateEntity(SimulationEditorRepository repository,
                        String editorName,
                        SimulationEditorRepository.EditableEntity entity,
                        EditorEventBus bus) implements EditorCommand {
        public CreateEntity {
            Objects.requireNonNull(repository, "repository");
            Objects.requireNonNull(editorName, "editorName");
            Objects.requireNonNull(entity, "entity");
        }
        @Override public void apply() {
            repository.addEntity(editorName, entity);
            if (bus != null) bus.publish(new EditorEvent.ModelChanged(editorName, entity.id(), "create", entity.name()));
        }
        @Override public void revert() {
            repository.removeEntity(new SimulationEditorRepository.EntityRef(editorName, entity.id()));
            if (bus != null) bus.publish(new EditorEvent.ModelChanged(editorName, entity.id(), "remove", entity.name()));
        }
        @Override public String description() { return "Create " + editorName + " / " + entity.id(); }
    }


    record ToggleProjectSelection(SimulationEditorRepository repository,
                                  SimulationEditorRepository.EntityRef ref,
                                  boolean oldSelected,
                                  boolean newSelected,
                                  EditorEventBus bus) implements EditorCommand {
        public ToggleProjectSelection {
            Objects.requireNonNull(repository, "repository");
            Objects.requireNonNull(ref, "ref");
        }
        @Override public void apply() { set(newSelected); }
        @Override public void revert() { set(oldSelected); }
        @Override public String description() { return (newSelected ? "Bind " : "Unbind ") + ref.editorName() + " / " + ref.entityId() + " to mod scope"; }
        @Override public boolean isNoop() { return oldSelected == newSelected; }
        private void set(boolean value) {
            repository.setSelected(ref, value);
            if (bus != null) bus.publish(new EditorEvent.ModelChanged("Mod Packaging", ref.entityId(), "selected", value));
        }
    }
}
