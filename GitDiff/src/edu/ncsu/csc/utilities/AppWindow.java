package edu.ncsu.csc.utilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

/**
 * The application's window instance.
 * 
 * @author Chris Stroud (clstroud@ncsu.edu)
 * @version 1.0.0
 */
public class AppWindow
{

    /** Controller instance responsible for handling the diff operation */
    GitDiffController             controller    = null;

    /** The root frame of the application window */
    JFrame                        frame;

    /** Model structure for the list of all available commits */
    LinkedHashMap<String, String> baseListModel = null;

    /**
     * Model structure used for storing a subset of the commitLists which correspond to all commits
     * made since the selected base commit
     */
    LinkedHashMap<String, String> newListModel  = null;

    /** UI element for listing all possible base commits and allowing for selection */
    JList                         baseList;

    /**
     * UI element for listing all commits made since the selected base commit. Displays all commits
     * if no base has been selected yet.
     */
    JList                         newList;

    /** UI element for displaying the message for the selected "new" commit */
    JTextPane                     newMsgTextPane;

    /** UI element for displaying the message for the selected "base" commit */
    JTextPane                     baseMsgTextPane;

    /**
     * Creates the application instance.
     */
    public AppWindow()
    {
        init();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void init()
    {
        // Window framej
        setFrame(new JFrame());
        getFrame().setBounds(100, 100, 580, 450);
        getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getFrame().getContentPane().setLayout(new BorderLayout(0, 0));

        // Menubar
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(SystemColor.window);

        // Open menu
        JMenu openRepoMenu = new JMenu("Open");
        openRepoMenu.setFont(new Font("Helvetica", Font.PLAIN, 14));
        JMenuItem openRepoMenuItem = new JMenuItem("Open Local Repository");
        openRepoMenuItem.setFont(new Font("Helvetica", Font.PLAIN, 14));
        openRepoMenuItem.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent arg0)
            {

                openRepositoryDirectorySelected();

            }
        });

        openRepoMenu.add(openRepoMenuItem);
        menuBar.add(openRepoMenu);
        getFrame().getContentPane().add(menuBar, BorderLayout.NORTH);

        // Window-backing panel
        JPanel windowPanel = new JPanel();
        windowPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        windowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        getFrame().getContentPane().add(windowPanel);
        windowPanel.setLayout(new MigLayout("", "[sg colgrp 25%][sg colgrp 25%][5%][sg colgrp 25%][sg colgrp 25%]", "[30px][63%][3%][20%][8%]"));

        JScrollPane newListScroller = new JScrollPane();
        newListScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        newListScroller.setBorder(new LineBorder(new Color(0, 0, 0)));
        newListScroller.setViewportView(this.newList);
        windowPanel.add(newListScroller, "cell 3 1 2 1,grow");

        this.newList = new JList();
        this.newList.addListSelectionListener(new ListSelectionListener()
        {

            @Override
            public void valueChanged(ListSelectionEvent arg0)
            {

                // Just update the new commit selection message text

                int idx = AppWindow.this.newList.getSelectedIndex();
                if (idx < 0) {
                    return;
                }

                Object[] valArray = AppWindow.this.baseListModel.values().toArray();
                AppWindow.this.newMsgTextPane.setText(valArray[idx].toString());

            }
        });

        this.newList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        newListScroller.setViewportView(this.newList);

        JScrollPane baseListScroller = new JScrollPane();
        baseListScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        baseListScroller.setBorder(new LineBorder(new Color(0, 0, 0)));
        baseListScroller.setViewportView(this.baseList);
        windowPanel.add(baseListScroller, "cell 0 1 2 1,grow");

        this.baseList = new JList();
        this.baseList.addListSelectionListener(new ListSelectionListener()
        {

            @Override
            public void valueChanged(ListSelectionEvent e)
            {

                int idx = AppWindow.this.baseList.getSelectedIndex();
                if (idx < 0) {
                    return;
                }

                // Update the model, then the UI
                updateBaseListModel();
                updateNewListModel(idx);
                updateNewListUI();

                // Update the base list selection message text
                Object[] valArray = AppWindow.this.baseListModel.values().toArray();
                AppWindow.this.baseMsgTextPane.setText(valArray[idx].toString());

            }
        });

        this.baseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        baseListScroller.setViewportView(this.baseList);

        this.baseMsgTextPane = new JTextPane();
        this.baseMsgTextPane.setEditable(false);
        this.baseMsgTextPane.setBackground(SystemColor.window);
        windowPanel.add(this.baseMsgTextPane, "cell 0 3 2 1,grow");

        this.newMsgTextPane = new JTextPane();
        this.newMsgTextPane.setBackground(SystemColor.window);
        this.newMsgTextPane.setEditable(false);
        windowPanel.add(this.newMsgTextPane, "cell 3 3 2 1,grow");

        // Title labels
        JTextPane txtpnBaseCommit = new JTextPane();
        txtpnBaseCommit.setBackground(SystemColor.window);
        txtpnBaseCommit.setFont(new Font("Helvetica", Font.BOLD, 14));
        txtpnBaseCommit.setEditable(false);
        txtpnBaseCommit.setText("Base Commit");
        windowPanel.add(txtpnBaseCommit, "cell 0 0 2 1,alignx center,growy");

        JTextPane txtpnNewCommit = new JTextPane();
        txtpnNewCommit.setText("New Commit");
        txtpnNewCommit.setFont(new Font("Helvetica", Font.BOLD, 14));
        txtpnNewCommit.setEditable(false);
        txtpnNewCommit.setBackground(SystemColor.window);
        windowPanel.add(txtpnNewCommit, "cell 3 0 2 1,alignx center,growy");

        // Diff button
        JButton diffButton = new JButton("Perform Diff");
        diffButton.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent arg0)
            {

                String baseObjId = AppWindow.this.baseListModel.keySet().toArray()[AppWindow.this.baseList.getSelectedIndex()].toString();
                String newObjId = AppWindow.this.baseListModel.keySet().toArray()[AppWindow.this.newList.getSelectedIndex()].toString();

                AppWindow.this.controller.performDiff(baseObjId, newObjId);
            }
        });
        windowPanel.add(diffButton, "cell 0 4 5 1,alignx center");
    }

    /**
     * Updates the "new" commits list such that its contents
     * are limited to only those commits starting at or after the
     * given index of the baseListModel.
     * 
     * @param idx
     *            The base index for restricting "new" commits.
     */
    void updateNewListModel(int idx)
    {

        // Get the SHA-1 hashes (keys) and commit messages (values)
        // from the baseListModel, then trim the base list down to
        // the correct sub-array and construct the new "newListModel" object.

        Object[] keyArray = this.baseListModel.keySet().toArray();
        Object[] valArray = this.baseListModel.values().toArray();

        keyArray = Arrays.copyOf(keyArray, idx);
        valArray = Arrays.copyOf(valArray, idx);

        if (this.newListModel == null) {
            this.newListModel = new LinkedHashMap<String, String>();
        } else {
            this.newListModel.clear();
        }

        for (int i = 0; i < keyArray.length; i++) {
            this.newListModel.put(keyArray[i].toString(), valArray[i].toString());
        }

    }

    /**
     * Updates the model for the base list UI, then
     * subsequently the model for the "new" list UI.
     */
    void updateBaseListModel()
    {
        this.baseListModel = this.controller.getCommitListFromCommit(null);
    }

    /**
     * Populates the list of candidate "new" commits.
     * 
     * Ensures that the list only displays commits that are more recent than the
     * selected base commit.
     */
    void updateNewListUI()
    {
        // Fetch all commits from the newListModel, then get the
        // commit SHA-1's (keys) and message strings (values) as
        // separate Object arrays. Loop through them and create
        // the display strings to be used in the JList for "new" commits

        if (this.newListModel == null) {
            return;
        }

        Object[] keys = this.newListModel.keySet().toArray();
        Object[] messages = this.newListModel.values().toArray();

        String[] outputKeys = new String[keys.length];

        for (int i = 0; i < keys.length; i++) {

            String commitHash = keys[i].toString().substring(0, 5);
            String commitMessage = messages[i].toString();

            outputKeys[i] = commitHash + "....." + commitMessage;

        }

        this.newList.setListData(outputKeys);

    }

    /**
     * Populates the list of candidate "Base" commits, then subsequently the "new" commits.
     */
    private void updateBaseListUI()
    {

        // Get the commit SHA-1's (keys) and message strings (values) as
        // separate Object arrays from the base list model.
        // Loop through them and create the display strings to
        // be used in the JList for "base" commits

        if (this.baseListModel == null) {
            return;
        }

        Object[] keys = this.baseListModel.keySet().toArray();
        Object[] messages = this.baseListModel.values().toArray();

        String[] outputKeys = new String[keys.length];

        for (int i = 0; i < keys.length; i++) {

            String commitHash = keys[i].toString().substring(0, 5);
            String commitMessage = messages[i].toString();

            outputKeys[i] = commitHash + "....." + commitMessage;
        }

        this.baseList.setListData(outputKeys);
    }

    /**
     * Callback for when the "File -> Open" menu is selected.
     * 
     * @throws HeadlessException
     *             if a HeadlessException occurs
     */
    void openRepositoryDirectorySelected() throws HeadlessException
    {

        // Make the file chooser directories-only and
        // hide hidden files for clarity. If the user
        // makes a selection, update the UI accordingly.

        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setFileHidingEnabled(true);
        int returnVal = fc.showOpenDialog(getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            this.controller.setRepositoryFilePath(file.getAbsolutePath());
            updateBaseListModel();
            updateBaseListUI();
        }

    }

    /**
     * Sets the visability of the app window
     * 
     * @param visible
     *            Whether the window should remain visible.
     */
    public void setVisible(boolean visible)
    {
        getFrame().setVisible(visible);
    }

    /**
     * Returns the current GitDiffController instance
     * 
     * @return the diff controller
     */
    public GitDiffController getDiffController()
    {
        return this.controller;
    }

    /**
     * Sets the current GitDiffController instance
     * 
     * @param controller
     *            The controller instance to use
     */
    public void setDiffController(GitDiffController controller)
    {
        this.controller = controller;
        this.controller.setAppWindow(this);
    }

    /**
     * Returns the window's root JFrame instance
     * 
     * @return the frame
     */
    protected JFrame getFrame()
    {
        return this.frame;
    }

    /**
     * Sets the window's root JFrame instance
     * 
     * @param frame
     *            The frame to set
     */
    protected void setFrame(JFrame frame)
    {
        this.frame = frame;
    }
}
