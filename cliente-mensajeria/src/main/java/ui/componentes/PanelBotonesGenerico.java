package ui.componentes;
import javax.swing.*;
import java.awt.*;


public class PanelBotonesGenerico extends JPanel {
    public JButton btnOriginal = new JButton("Original");
    public JButton btnHash = new JButton("Hash");
    public JButton btnEnc = new JButton("Encriptado");

    public PanelBotonesGenerico(boolean mostrarOriginal) {
        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
        setOpaque(true);
        Font f = new Font("Arial", Font.PLAIN, 11);
        btnOriginal.setFont(f); btnHash.setFont(f); btnEnc.setFont(f);

        if (mostrarOriginal) add(btnOriginal);
        add(btnHash);
        add(btnEnc);
    }
}