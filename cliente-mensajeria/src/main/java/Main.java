import ui.VentanaConexion;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // Establecer el Look & Feel del sistema para que se vea moderno
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Iniciar la aplicación en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            VentanaConexion login = new VentanaConexion();
            login.setLocationRelativeTo(null); // Centrar en pantalla
            login.setVisible(true);
        });
    }

}
