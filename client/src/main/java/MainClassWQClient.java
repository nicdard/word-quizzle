import javax.swing.*;
import java.awt.*;

public class MainClassWQClient {
    public static void main(String[] args) {
        System.out.println("Starting Client");
        JFrame frame = new JFrame("My First GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300,300);
        JButton button = new JButton("Press");
        button.setPreferredSize(new Dimension(100, 100));
        frame.getContentPane().add(button); // Adds Button to content pane of frame
        frame.setVisible(true);
        System.out.println("CIAONE");
    }
}
