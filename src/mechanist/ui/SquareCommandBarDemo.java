package mechanist.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.List;

public final class SquareCommandBarDemo {
    private SquareCommandBarDemo() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Square Command Bar Demo");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(new SquareCommandBar(List.of(
                    new SquareCommandAction("move", "Move", "Move or path the selected actor.", null, () -> System.out.println("move")),
                    new SquareCommandAction("look", "Look", "Inspect the selected tile, entity, or object.", null, () -> System.out.println("look")),
                    new SquareCommandAction("inventory", "Items", "Open carried equipment and inventory state.", null, () -> System.out.println("inventory")),
                    new SquareCommandAction("wait", "Wait", "Pass time while remaining alert.", null, () -> System.out.println("wait"))
            )), BorderLayout.EAST);
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }
}
