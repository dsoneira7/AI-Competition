package PSI1;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class GUI extends JFrame implements ActionListener {
    JLabel leftPanelRoundsLabel;
    JLabel leftPanelExtraInformation;
    JList<String> list;
    private MainAgent mainAgent;
    private JPanel rightPanel;
    private JTextArea rightPanelLoggingTextArea;
    private LoggingOutputStream loggingOutputStream;
    private JTable payoffTable;

    public GUI() {
        initUI();
    }

    public GUI(MainAgent agent) {
        mainAgent = agent;
        initUI();
        loggingOutputStream = new LoggingOutputStream(rightPanelLoggingTextArea);
    }

    public void log(String s) {
        Runnable appendLine = () -> {
            rightPanelLoggingTextArea.append('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
            rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
        };
        SwingUtilities.invokeLater(appendLine);
    }

    public OutputStream getLoggingOutputStream() {
        return loggingOutputStream;
    }

    public void logLine(String s) {
        log(s + "\n");
    }

    public void setPlayersUI(String[] players) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String s : players) {
            listModel.addElement(s);
        }
        list.setModel(listModel);
    }

    public void initUI() {
        setTitle("GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(600, 400));
        setPreferredSize(new Dimension(1000, 600));
        setJMenuBar(createMainMenuBar());
        setContentPane(createMainContentPane());
        pack();
        setVisible(true);
    }

    private Container createMainContentPane() {
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridy = 0;
        gc.weightx = 0.5;
        gc.weighty = 0.5;

        //LEFT PANEL
        gc.gridx = 0;
        gc.weightx = 1;
        pane.add(createLeftPanel(), gc);

        //CENTRAL PANEL
        gc.gridx = 1;
        gc.weightx = 8;
        pane.add(createCentralPanel(), gc);

        //RIGHT PANEL
        gc.gridx = 2;
        gc.weightx = 8;
        pane.add(createRightPanel(), gc);
        return pane;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        leftPanelRoundsLabel = new JLabel("Round 0 / null");
        JButton leftPanelNewButton = new JButton("New");
        leftPanelNewButton.addActionListener(actionEvent -> mainAgent.newGame());
        JButton leftPanelStopButton = new JButton("Stop");
        leftPanelStopButton.addActionListener(actionEvent -> mainAgent.slp = true);
        JButton leftPanelContinueButton = new JButton("Continue");
        leftPanelContinueButton.addActionListener(actionEvent -> mainAgent.slp = false);

        leftPanelExtraInformation = new JLabel("Parameters - NSRIP");

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;
        gc.weightx = 0.5;
        gc.weighty = 0.5;

        gc.gridy = 0;
        leftPanel.add(leftPanelRoundsLabel, gc);
        gc.gridy = 1;
        leftPanel.add(leftPanelNewButton, gc);
        gc.gridy = 2;
        leftPanel.add(leftPanelStopButton, gc);
        gc.gridy = 3;
        leftPanel.add(leftPanelContinueButton, gc);
        gc.gridy = 4;
        gc.weighty = 10;
        leftPanel.add(leftPanelExtraInformation, gc);

        return leftPanel;
    }

    private JPanel createCentralPanel() {
        JPanel centralPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;

        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;

        gc.gridy = 0;
        gc.weighty = 1;
        centralPanel.add(createCentralTopSubpanel(), gc);
        gc.gridy = 1;
        gc.weighty = 4;
        centralPanel.add(createCentralBottomSubpanel(), gc);

        return centralPanel;
    }

    private JPanel createCentralTopSubpanel() {
        JPanel centralTopSubpanel = new JPanel(new GridBagLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Empty");
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(list);

        JLabel info1 = new JLabel("Selected player info");
        JButton updatePlayersButton = new JButton("Update players");
        updatePlayersButton.addActionListener(actionEvent -> mainAgent.updatePlayers());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.weighty = 0.5;
        gc.anchor = GridBagConstraints.CENTER;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 666;
        gc.fill = GridBagConstraints.BOTH;
        centralTopSubpanel.add(listScrollPane, gc);
        gc.gridx = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        centralTopSubpanel.add(info1, gc);
        gc.gridy = 1;
        centralTopSubpanel.add(updatePlayersButton, gc);

        return centralTopSubpanel;
    }

    private JPanel createCentralBottomSubpanel() {
        JPanel centralBottomSubpanel = new JPanel(new GridBagLayout());

        Object[] nullPointerWorkAround = {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"};

        Object[][] data = {
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*"}
        };

        JLabel payoffLabel = new JLabel("Payoff matrix");
        payoffTable = new JTable(data, nullPointerWorkAround);
        payoffTable.setTableHeader(null);
        payoffTable.setEnabled(false);

        JScrollPane player1ScrollPane = new JScrollPane(payoffTable);

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weighty = 0.5;
        centralBottomSubpanel.add(payoffLabel, gc);
        gc.gridy = 1;
        gc.gridx = 0;
        gc.weighty = 2;
        centralBottomSubpanel.add(player1ScrollPane, gc);

        return centralBottomSubpanel;
    }

    private JPanel createRightPanel() {
        rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.weighty = 1d;
        c.weightx = 1d;

        rightPanelLoggingTextArea = new JTextArea("");
        rightPanelLoggingTextArea.setEditable(false);
        JScrollPane jScrollPane = new JScrollPane(rightPanelLoggingTextArea);
        rightPanel.add(jScrollPane, c);
        return rightPanel;
    }

    private JMenuBar createMainMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        JMenuItem exitFileMenu = new JMenuItem("Exit");
        exitFileMenu.setToolTipText("Exit application");
        exitFileMenu.addActionListener(actionEvent -> System.exit(-1));

        JMenuItem newGameFileMenu = new JMenuItem("New Game");
        newGameFileMenu.setToolTipText("Start a new game");
        newGameFileMenu.addActionListener(actionEvent -> mainAgent.newGame());

        menuFile.add(newGameFileMenu);
        menuFile.add(exitFileMenu);
        menuBar.add(menuFile);

        JMenu menuEdit = new JMenu("Edit");
        JMenuItem resetPlayerEditMenu = new JMenuItem("Reset Players");
        resetPlayerEditMenu.setToolTipText("Reset all player");
        resetPlayerEditMenu.setActionCommand("reset_players");
        resetPlayerEditMenu.addActionListener(this);

        JMenuItem parametersEditMenu = new JMenuItem("Parameters");
        parametersEditMenu.setToolTipText("Modify the parameters of the game"); //Acabar de añadir codigo para a modificacion de parametros
        parametersEditMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String input = JOptionPane.showInputDialog(new Frame("Configure parameters"), "Enter parameters N,S,R,I,P");
                if (input.length() > 0 && input.split(",").length == 5) {
                    logLine("Parameters: " + input);
                    mainAgent.setNewParams(input.split(","));
                } else logLine("Bad input");
            }
        });

        menuEdit.add(resetPlayerEditMenu);
        menuEdit.add(parametersEditMenu);
        menuBar.add(menuEdit);

        JMenu menuRun = new JMenu("Run");

        JMenuItem newRunMenu = new JMenuItem("New");
        newRunMenu.setToolTipText("Starts a new series of games");
        newRunMenu.addActionListener(actiionEvent -> mainAgent.newGame());

        JMenuItem stopRunMenu = new JMenuItem("Stop");
        stopRunMenu.setToolTipText("Stops the execution of the current round");
        stopRunMenu.addActionListener(actionEvent -> mainAgent.slp = true);

        JMenuItem continueRunMenu = new JMenuItem("Continue");
        continueRunMenu.setToolTipText("Resume the execution");
        continueRunMenu.addActionListener(actionEvent -> mainAgent.slp = false);

        JMenuItem roundNumberRunMenu = new JMenuItem("Number Of rounds");
        roundNumberRunMenu.setToolTipText("Change the number of rounds"); //Easy
        roundNumberRunMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String input = JOptionPane.showInputDialog(new Frame("Configure rounds"), "How many rounds?");
                if (input.length() > 0) {
                    logLine(input + " rounds.");
                    mainAgent.setNewParams(Integer.parseInt(input));
                } else logLine("Bad input");
            }
        });
        menuRun.add(newRunMenu);
        menuRun.add(stopRunMenu);
        menuRun.add(continueRunMenu);
        menuRun.add(roundNumberRunMenu);
        menuBar.add(menuRun);

        JMenu menuWindow = new JMenu("Window");

        JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
        toggleVerboseWindowMenu.addActionListener(actionEvent -> rightPanel.setVisible(toggleVerboseWindowMenu.getState()));

        menuWindow.add(toggleVerboseWindowMenu);
        menuBar.add(menuWindow);

        return menuBar;
    }

    public void modifyMatrix(Object[][] newData, int size) {
        Object[] m = new Object[size];
        for (int i = 0; i < size; i++) {
            m[i] = (i + 1) + "";
        }
        DefaultTableModel tm = new DefaultTableModel(null, m);
        for (int i = 0; i < size; i++) {
            tm.addRow(newData[i]);
        }
        payoffTable.setModel(tm);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            logLine("Button " + button.getText());
        } else if (e.getSource() instanceof JMenuItem) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            logLine("Menu " + menuItem.getText());
        }
    }

    public class LoggingOutputStream extends OutputStream {
        private JTextArea textArea;

        public LoggingOutputStream(JTextArea jTextArea) {
            textArea = jTextArea;
        }

        @Override
        public void write(int i) throws IOException {
            textArea.append(String.valueOf((char) i));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }
}
