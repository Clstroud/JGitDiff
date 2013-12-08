package edu.ncsu.csc.utilities;

import java.awt.EventQueue;

/**
 * Base class for the application. Handles launch and instantiation of the app window.
 * 
 * @author Chris Stroud (clstroud@ncsu.edu)
 * @version 1.0.0
 */
public class GitDiff
{

    /** The diff controller that will be used to drive the app instance */
    static GitDiffController controller = new GitDiffController();

    /**
     * Commandline entry-point for the application
     * 
     * @param args
     *            Commandline arguments
     */
    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    AppWindow window = new AppWindow();
                    window.setVisible(true);
                    window.setDiffController(controller);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
