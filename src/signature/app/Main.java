package signature.app;
/**
*@author 223017726
*@author 222008253
*@author 224029403
*@author 222209456
*/
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import signature.ui.SignatureVerifierFrame;
 
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {
            }

            SignatureVerifierFrame frame = new SignatureVerifierFrame();
            frame.setVisible(true);
        });
    }
}
