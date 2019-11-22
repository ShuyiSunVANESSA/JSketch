import javax.swing.JFrame;

class Main {
    public static void main(String[] args) {
        JFrame mainFrame = new JFrame();
        MainPanel mainPanel = new MainPanel();
        StartScreen startScreen = new StartScreen(mainPanel);
        mainPanel.switchScreen(startScreen);
        mainFrame.add(mainPanel);
        mainFrame.pack();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
        mainFrame.setVisible(true);
    }
}