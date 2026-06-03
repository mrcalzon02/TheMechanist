package mechanist.ui;

import mechanist.input.GenericControllerInput;
import mechanist.input.InputDevice;
import mechanist.input.InputToken;
import mechanist.input.KeyBind;
import mechanist.input.KeyBindingManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Interactive, scrollable keybinding remapping menu for keyboard, mouse, and generic controllers.
 *
 * The panel owns only UI state. Binding authority lives in KeyBindingManager. Controller libraries
 * should call acceptControllerInput(...) when their polling bridge detects the next valid button,
 * hat, or axis event while the panel is in listening mode.
 */
public final class KeybindingRemappingPanel extends JPanel implements PropertyChangeListener {
    private static final Color NORMAL_FOREGROUND = UIManager.getColor("Button.foreground") == null ? Color.WHITE : UIManager.getColor("Button.foreground");
    private static final Color LISTENING_FOREGROUND = new Color(255, 210, 90);
    private static final Color WARNING_FOREGROUND = new Color(220, 70, 70);
    private static final int WARNING_DISPLAY_MS = 1800;

    private final KeyBindingManager manager;
    private final JTabbedPane tabs = new JTabbedPane();
    private final EnumMap<InputDevice, Map<String, JButton>> buttonsByDevice = new EnumMap<>(InputDevice.class);
    private final JLabel statusLabel = new JLabel("Select a binding to remap it.");
    private final Timer warningTimer;
    private ListeningState listeningState;
    private AWTEventListener mouseCaptureListener;
    private KeyEventDispatcher keyboardDispatcher;

    public KeybindingRemappingPanel() {
        this(KeyBindingManager.getInstance());
    }

    public KeybindingRemappingPanel(KeyBindingManager manager) {
        super(new BorderLayout(10, 10));
        this.manager = Objects.requireNonNull(manager, "manager");
        this.warningTimer = new Timer(WARNING_DISPLAY_MS, e -> clearWarning());
        this.warningTimer.setRepeats(false);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUi();
        manager.addPropertyChangeListener(this);
        refreshAllRows();
    }

    public void dispose() {
        stopListening();
        manager.removePropertyChangeListener(this);
    }

    /**
     * Hook for controller polling bridges.
     *
     * Call this from the controller polling layer when a generic-controller event is detected.
     * The method safely marshals to the EDT if called from a background gamepad thread.
     */
    public void acceptControllerInput(GenericControllerInput input) {
        Objects.requireNonNull(input, "input");
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> acceptControllerInput(input));
            return;
        }
        if (listeningState == null || listeningState.device != InputDevice.GENERIC_CONTROLLER) {
            return;
        }
        completeListening(input.toToken());
    }

    /**
     * Convenience overload for raw controller button capture.
     */
    public void acceptControllerButton(int buttonIndex) {
        acceptControllerInput(GenericControllerInput.button(buttonIndex));
    }

    /**
     * Convenience overload for raw controller axis capture. Direction should be -1 or +1.
     */
    public void acceptControllerAxis(String axisName, int direction) {
        acceptControllerInput(GenericControllerInput.axis(axisName, direction));
    }

    /**
     * Convenience overload for D-pad/hat capture.
     */
    public void acceptControllerHat(String direction) {
        acceptControllerInput(GenericControllerInput.hat(direction));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("bindings".equals(evt.getPropertyName())) {
            refreshAllRows();
        }
    }

    private void buildUi() {
        var header = new JPanel(new BorderLayout(8, 4));
        var title = new JLabel("Controls / Keybindings");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 3f));
        header.add(title, BorderLayout.NORTH);
        var detail = new JLabel("Click a binding, then press the next keyboard key, mouse button, or generic controller input.");
        header.add(detail, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        for (var device : InputDevice.values()) {
            buttonsByDevice.put(device, new LinkedHashMap<>());
            tabs.addTab(device.displayName(), new JScrollPane(buildDevicePanel(device), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        }
        add(tabs, BorderLayout.CENTER);

        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(8, 4, 2, 4)
        ));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel buildDevicePanel(InputDevice device) {
        var panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (var bind : manager.getBindings(device)) {
            addBindingRow(panel, gbc, device, bind);
            gbc.gridy++;
        }

        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);
        return panel;
    }

    private void addBindingRow(JPanel panel, GridBagConstraints gbc, InputDevice device, KeyBind bind) {
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        var label = new JLabel(bind.displayName());
        label.setLabelFor(null);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        var button = new JButton(bind.token().displayName());
        button.setPreferredSize(new Dimension(170, 30));
        button.setMinimumSize(new Dimension(170, 30));
        button.addActionListener(e -> beginListening(device, bind.commandId(), button));
        buttonsByDevice.get(device).put(bind.commandId(), button);
        panel.add(button, gbc);
    }

    private void beginListening(InputDevice device, String commandId, JButton button) {
        stopListening();
        listeningState = new ListeningState(device, commandId, button, button.getText());
        button.setText(device == InputDevice.GENERIC_CONTROLLER ? "Press controller..." : device == InputDevice.MOUSE ? "Click mouse..." : "Press a key...");
        button.setForeground(LISTENING_FOREGROUND);
        button.requestFocusInWindow();
        statusLabel.setForeground(LISTENING_FOREGROUND);
        statusLabel.setText("Listening for " + device.displayName() + " input for " + commandDisplayName(device, commandId) + ". Press Escape to cancel.");

        installKeyboardDispatcher();
        if (device == InputDevice.MOUSE) {
            installMouseCaptureListener();
        }
    }

    private void completeListening(InputToken token) {
        if (listeningState == null) {
            return;
        }
        var state = listeningState;
        if (token.device() != state.device) {
            showWarning("Wrong input family. Expected " + state.device.displayName() + ", received " + token.device().displayName() + ".");
            return;
        }

        var result = manager.rebind(state.device, state.commandId, token, KeyBindingManager.DuplicatePolicy.SWAP);
        stopListening();
        if (result.accepted()) {
            refreshAllRows();
            statusLabel.setForeground(new Color(90, 180, 90));
            statusLabel.setText(result.message());
        } else {
            showWarning(result.message());
        }
    }

    private void stopListening() {
        if (listeningState != null) {
            listeningState.button.setText(listeningState.previousText);
            listeningState.button.setForeground(NORMAL_FOREGROUND);
            listeningState = null;
        }
        if (keyboardDispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyboardDispatcher);
            keyboardDispatcher = null;
        }
        if (mouseCaptureListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(mouseCaptureListener);
            mouseCaptureListener = null;
        }
        statusLabel.setForeground(NORMAL_FOREGROUND);
        statusLabel.setText("Select a binding to remap it.");
    }

    private void installKeyboardDispatcher() {
        keyboardDispatcher = event -> {
            if (listeningState == null || event.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                stopListening();
                return true;
            }
            if (listeningState.device != InputDevice.KEYBOARD) {
                return false;
            }
            var modifiers = event.getModifiersEx() & allowedKeyboardModifierMask();
            completeListening(InputToken.keyboard(event.getKeyCode(), modifiers));
            return true;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyboardDispatcher);
    }

    private void installMouseCaptureListener() {
        mouseCaptureListener = event -> {
            if (!(event instanceof MouseEvent mouseEvent)) {
                return;
            }
            if (listeningState == null || listeningState.device != InputDevice.MOUSE) {
                return;
            }
            if (mouseEvent.getID() != MouseEvent.MOUSE_PRESSED) {
                return;
            }
            if (!SwingUtilities.isDescendingFrom(mouseEvent.getComponent(), this) && mouseEvent.getComponent() != this) {
                return;
            }
            mouseEvent.consume();
            completeListening(InputToken.mouseButton(mouseEvent.getButton()));
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(mouseCaptureListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private static int allowedKeyboardModifierMask() {
        return InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK;
    }

    private void refreshAllRows() {
        for (var device : InputDevice.values()) {
            var buttons = buttonsByDevice.get(device);
            if (buttons == null) {
                continue;
            }
            for (var bind : manager.getBindings(device)) {
                var button = buttons.get(bind.commandId());
                if (button != null && (listeningState == null || listeningState.button != button)) {
                    button.setText(bind.token().displayName());
                    button.setForeground(NORMAL_FOREGROUND);
                }
            }
        }
    }

    private String commandDisplayName(InputDevice device, String commandId) {
        return manager.getBinding(device, commandId).map(KeyBind::displayName).orElse(commandId);
    }

    private void showWarning(String text) {
        statusLabel.setForeground(WARNING_FOREGROUND);
        statusLabel.setText(text);
        if (listeningState != null) {
            listeningState.button.setForeground(WARNING_FOREGROUND);
        }
        warningTimer.restart();
    }

    private void clearWarning() {
        if (listeningState == null) {
            statusLabel.setForeground(NORMAL_FOREGROUND);
            statusLabel.setText("Select a binding to remap it.");
        } else {
            statusLabel.setForeground(LISTENING_FOREGROUND);
            listeningState.button.setForeground(LISTENING_FOREGROUND);
        }
    }

    private record ListeningState(InputDevice device, String commandId, JButton button, String previousText) {}
}
