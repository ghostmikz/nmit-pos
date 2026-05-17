import controller.LoginController;
import view.LoginFrame;

import javax.swing.*;

public class App {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext",                "true");
        System.setProperty("sun.java2d.xrender",          "true");

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}

            LoginFrame frame = new LoginFrame();
            new LoginController(frame);
            frame.setVisible(true);
        });
    }
}
