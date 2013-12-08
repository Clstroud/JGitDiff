package edu.ncsu.csc.utilities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Responsible for maintaining the session data while
 * the application is processing commit data. This class
 * holds any parsed data until the session is over, at
 * which time it formats said data for output.
 * 
 * @author Chris Storud (clstroud@ncsu.edu)
 * @version 1.0.0
 */
public class DiffSession
{

    /**
     * A sorted Map in format <Classname, Method Names> where Classname is a String amd Method Names
     * is a Sorted Set of Strings
     */
    TreeMap<String, TreeSet<String>> signatures     = new TreeMap<String, TreeSet<String>>();

    /** A sorted set of SQL filenames that have been altered */
    private Set<String>              sqlFiles       = new TreeSet<String>();

    /** A sorted set of JSP filenames that have been altered */
    private Set<String>              jspFiles       = new TreeSet<String>();

    /** The display name of the repository being diff'd */
    private String                   repositoryName = "";

    /** The username of the local repository owner */
    private String                   userName       = "";

    /** The contact email for the local repository owner */
    private String                   userEmail      = "";

    /** A String representation of the number of changes made between the user-selected commits */
    private String                   deltaCount     = "";

    /** Model representation of the user-selected "base" commit */
    private CommitModel              baseCommit     = null;

    /** Model representation of the user-selected "new" commit */
    private CommitModel              newCommit      = null;

    /**
     * Adds a new JSP file to the session store
     * 
     * @param filename
     *            The filename which should be added
     */
    public void addJspFile(String filename)
    {
        if (filename == null || filename.equals("null")) {
            return;
        }

        this.jspFiles.add(filename);
    }

    /**
     * Adds a new SQL file to the session store
     * 
     * @param filename
     *            The filename which should be added
     */
    public void addSqlFile(String filename)
    {
        if (filename == null || filename.equals("null")) {
            return;
        }
        this.sqlFiles.add(filename);
    }

    /**
     * Stores the local repository name
     * 
     * @param name
     *            The name of the local repository
     */
    public void setRepositoryName(String name)
    {
        this.repositoryName = name;
    }

    /**
     * Converts the given metadata into a commit model object for the base commit and stores it in
     * the session
     * 
     * @param sha1
     *            The commit SHA-1
     * @param dateString
     *            The date of the commit in String format
     * @param messageShortString
     *            The commit message
     */
    public void setBaseCommitMetadata(String sha1, String dateString, String messageShortString)
    {
        this.baseCommit = new CommitModel(sha1, dateString, messageShortString);
    }

    /**
     * Converts the given metadata into a commit model object for the new commit and stores it in
     * the session
     * 
     * @param sha1
     *            The commit SHA-1
     * @param dateString
     *            The date of the commit in String format
     * @param messageShortString
     *            The commit message
     */
    public void setNewCommitMetadata(String sha1, String dateString, String messageShortString)
    {
        this.newCommit = new CommitModel(sha1, dateString, messageShortString);
    }

    /**
     * Retrieves the calculated number of changes between the user-selected diffs
     * 
     * @return The deltaCount
     */
    public String getDeltaCount()
    {
        return this.deltaCount;
    }

    /**
     * Stores the given value of the delta count in the session
     * 
     * @param deltaCount
     *            The deltaCount value
     */
    public void setDeltaCount(String deltaCount)
    {
        this.deltaCount = deltaCount;
    }

    /**
     * Stores the username of the local repository owner
     * 
     * @param userName
     *            the userName to set
     */
    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    /**
     * Stores the email of the local repository owner
     * 
     * @param userEmail
     *            the userEmail to set
     */
    public void setUserEmail(String userEmail)
    {
        this.userEmail = userEmail;
    }

    /**
     * Adds a representation of a changed method to the session store based on the given metadata
     * 
     * @param pkg
     *            The fully qualified pakage path in which the method resides
     * 
     * @param method
     *            The method signature of the altered method
     */
    public void addChangedMethod(String pkg, String method)
    {
        if (method == null) {
            return;
        }

        TreeSet<String> methods = this.signatures.get(pkg);
        if (methods == null) {
            methods = new TreeSet<String>();
        }
        methods.add(method);
        this.signatures.put(pkg, methods);
    }

    /**
     * Takes the current stored session data and converts it into a
     * formatted string that can be placed directly in a text file
     * for human consumption.
     * 
     * @return The constructed output string
     */
    public String getOutputString()
    {
        int msgSplitLen = 35; // The length at which a wrap is desired for commit messages

        StringBuilder output = new StringBuilder();

        output.append(String.format("Repository:  %s%n", this.repositoryName));
        output.append(String.format("Date:        %s%n", new SimpleDateFormat("dd/MM/yyyy").format(new Date())));
        output.append(String.format("User:        %s<%s>%n", this.userName, this.userEmail));
        output.append(String.format("Delta Count: %s%n%n", Integer.valueOf(this.deltaCount)));
        output.append(String.format("Base Commit:%n    SHA-1: %s%n    Date: %s%n    Message: ", this.baseCommit.getSha1(), this.baseCommit.getDateStr()));

        List<String> wrappedBaseMessage = wrapWordsInString(this.baseCommit.getMessage(), msgSplitLen);
        Iterator<String> baseMsgIterator = wrappedBaseMessage.listIterator();
        
        if (baseMsgIterator.hasNext()) {
            output.append(baseMsgIterator.next());
        }
        
        while (baseMsgIterator.hasNext()) {
            output.append(String.format("%n             %s", baseMsgIterator.next()));
        }

        output.append(String.format("%n%n New Commit:%n    SHA-1: %s%n    Date: %s%n    Message: ", this.newCommit.getSha1(), this.newCommit.getDateStr()));

        List<String> wrappedNewMessage = wrapWordsInString(this.newCommit.getMessage(), msgSplitLen);
        Iterator<String> newMsgIterator = wrappedNewMessage.listIterator();
        
        if (newMsgIterator.hasNext()) {
            output.append(newMsgIterator.next());
        }
        
        while (newMsgIterator.hasNext()) {
            output.append(String.format("%n             %s", newMsgIterator.next()));
        }

        output.append(String.format("%n%n"));
        output.append(String.format("JSP Files (%d):%n", Integer.valueOf(this.jspFiles.size())));
        output.append(String.format("====================%n%n"));

        for (String jspFile : this.jspFiles) {
            output.append(String.format("    %s%n", jspFile));
        }

        output.append("\n");
        output.append(String.format("SQL Files (%s):%n", Integer.valueOf(this.sqlFiles.size())));
        output.append(String.format("====================%n%n"));

        for (String sqlFile : this.sqlFiles) {
            output.append(String.format("    %s%n", sqlFile));
        }

        output.append(String.format("%nJava Files (%s):%n", Integer.valueOf(this.signatures.size())));
        output.append(String.format("====================%n%n"));

        for (Entry<String, TreeSet<String>> pkg : this.signatures.entrySet()) {

            output.append(String.format("%s%n", pkg.getKey()));

            if (pkg.getValue().size() > 0) {

                for (String aMethod : pkg.getValue()) {
                    output.append(String.format("    %s%n", aMethod));
                }

            } else {

                output.append(String.format("    No Changes Within Method Contexts%n"));

            }

            output.append("\n");
        }

        return output.toString();
    }

    /**
     * Wraps words from a given string conforming to the
     * given length of characters for a particular line
     * 
     * @param str
     *            The string to wrap
     * @param length
     *            The number of characters permitted on a line
     * 
     * @return List of the wrapped strings
     */
    private static List<String> wrapWordsInString(String str, int length)
    {
        ArrayList<String> retVal = new ArrayList<String>();   // Return value

        String[] splitResult = str.split("\\s"); // Split based on whitespace characters

        StringBuilder lineContents = new StringBuilder();	// Buffer for the line

        // Loop through all words
        for (String atom : splitResult) {

            // If the lineContents are longer than the length of the
            // character limit, it's necessary to flush the buffer
            // into the return structure before working on this
            // atom since it needs to be on a new line

            if (lineContents.length() + atom.length() > length) {

                retVal.add(lineContents.toString());                   // Flush the line
                lineContents.delete(0, lineContents.length());     // Reset the contents of the buffer

                // Is the word we're dealing with longer than the limit?
                if (atom.length() > length) {

                    // Make a temp copy that can be widdled-away at
                    // Then hyphenate as neccessary.
                    String tempAtom = atom;
                    while (tempAtom.length() > 0) {

                        // Is the tempAtom longer than the line max?
                        int idx = Math.min(length, tempAtom.length());

                        // If the string fits, write it and we're done
                        if (idx == tempAtom.length()) {

                            retVal.add(tempAtom.substring(0, idx));
                            break;

                        }

                        idx--;
                        retVal.add(tempAtom.substring(0, idx).concat("-"));  // Append the hyphenated
                                                                            // word
                        tempAtom = tempAtom.substring(idx + 1);              // Widdle away and keep going
                    }
                }
            }

            lineContents.append(" " + atom);
        }

        // If anything is left in the buffer (likely is), then add to return structure
        if (lineContents.length() > 0) {
            retVal.add(lineContents.toString());
        }

        return retVal;
    }

    /**
     * Convenience class for storing miscellaneous metadata about
     * a particular commit.
     * 
     * @author Chris Stroud (clstroud@ncsu.edu)
     * @version 1.0.0
     */
    private static class CommitModel
    {
        /** The SHA-1 hash of the commit */
        private String sha1    = "";

        /** The String representation of the commit date */
        private String dateStr = "";

        /** The commit message */
        private String message = "";

        /**
         * Constructs a new CommitModel instance from the given metadata
         * 
         * @param sha1
         *            The commit hash
         * @param dateStr
         *            The commit date as a String
         * @param message
         *            The commit message
         */
        public CommitModel(String sha1, String dateStr, String message)
        {
            super();
            this.sha1 = sha1;
            this.dateStr = dateStr;
            this.message = message;
        }

        /**
         * @return The commit message
         */
        public String getMessage()
        {
            return this.message;
        }

        /**
         * @return The date string for the commit date
         */
        public String getDateStr()
        {
            return this.dateStr;
        }

        /**
         * @return The commit's SHA-1 hash
         */
        public String getSha1()
        {
            return this.sha1;
        }
    }
}
