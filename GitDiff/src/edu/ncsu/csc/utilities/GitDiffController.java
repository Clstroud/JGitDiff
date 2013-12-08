package edu.ncsu.csc.utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * The controller class designed to encapsulate
 * all mechanisms for the Git tree traversal and
 * diff operations.
 * 
 * @author Chris Stroud (clstroud@ncsu.edu)
 * @version 1.0.0
 */
public class GitDiffController
{

    /** The repository directory path */
    private String    filePath  = null;

    /** Reference to the application's window instance */
    private AppWindow appWindow = null;

    /** JGit interface to the loaded repository */
    private Git       gitInstance;

    /**
     * Attempts to load the repository based on the previously-provided filePath
     */
    public void loadRepository()
    {
        // Attempt to construct the Repository object
        Repository repo = null;
        try {

            RepositoryBuilder builder = new RepositoryBuilder();
            builder.setMustExist(true);
            builder.setGitDir(new File(this.filePath.concat("/.git")));
            repo = builder.build();

            this.gitInstance = new Git(repo);

        } catch (IOException e) {
            System.out.println("Failed to obtain repo: " + this.filePath + " " + e.getMessage());
            showAlert("Invalid Repository Path - " + e.getMessage());
            return;
        }

    }

    /**
     * Constructs a Linked Map of type SHA-1 (key), Message (value). The contained
     * values represent all commits (in order) starting from the "oldest-ancestor"
     * commit (indicated by the given hash), up until the most recent commit on that branch.
     * 
     * @param sha1
     *            The SHA-1 of the ancestor commit
     * @return Linked Map of commit data
     */
    public LinkedHashMap<String, String> getCommitListFromCommit(String sha1)
    {
        LinkedHashMap<String, String> retVal = new LinkedHashMap<String, String>();

        try {

            Iterable<RevCommit> commits = this.gitInstance.log().all().call();

            boolean foundTree = (sha1 == null);
            for (RevCommit commit : commits) {

                if (foundTree == false && commit.getName().equalsIgnoreCase(sha1)) {
                    foundTree = true;
                }

                if (foundTree) {
                    retVal.put(commit.getName(), commit.getShortMessage());
                }
            }

        } catch (IOException e) {
            return retVal;
        } catch (NoHeadException e1) {
            return retVal;
        } catch (GitAPIException e1) {
            return retVal;
        }

        return retVal;
    }

    /**
     * Returns a commit matching the given SHA-1 hash
     * 
     * @param hash
     *            The commit's SHA-1 hash
     * 
     * @return The commit revision instance
     */
    private RevCommit getCommitForHash(String hash)
    {
        try {

            Iterable<RevCommit> commits = this.gitInstance.log().all().call();

            for (RevCommit commit : commits) {
                if (commit.getName().equalsIgnoreCase(hash)) {
                    return commit;
                }
            }

        } catch (IOException e) {
            return null;
        } catch (NoHeadException e1) {
            return null;
        } catch (GitAPIException e1) {
            return null;
        }

        return null;
    }

    /**
     * The driver of the diffing processing between the two given commit hashes.
     * If you were looking for the party, this is where it's at.
     * 
     * @param baseObjId
     *            The base commit hash
     * @param newObjId
     *            The new commit hash
     */
    public void performDiff(String baseObjId, String newObjId)
    {
        Repository repo = this.gitInstance.getRepository();
        RevCommit baseRev = getCommitForHash(baseObjId);
        RevCommit newRev = getCommitForHash(newObjId);

        DiffSession diffSession = new DiffSession();

        diffSession.setUserName(repo.getConfig().getString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_NAME));
        diffSession.setUserEmail(repo.getConfig().getString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_EMAIL));

        diffSession.setRepositoryName(new File(this.gitInstance.getRepository().getDirectory().getParent()).getName());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String baseDate = dateFormat.format(new Date(baseRev.getCommitTime() * 1000L));
        String newDate = dateFormat.format(new Date(newRev.getCommitTime() * 1000L));

        diffSession.setBaseCommitMetadata(baseObjId, baseDate, baseRev.getShortMessage());
        diffSession.setNewCommitMetadata(newObjId, newDate, newRev.getShortMessage());

        List<DiffEntry> diff;
        try {
            AbstractTreeIterator oldTreeParser = prepareTreeParser(repo, baseObjId);
            AbstractTreeIterator newTreeParser = prepareTreeParser(repo, newObjId);
            diff = this.gitInstance.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).call();
        } catch (MissingObjectException e1) {
            System.out.println("ERROR: " + e1.getMessage());
            return;
        } catch (IncorrectObjectTypeException e1) {
            System.out.println("ERROR: " + e1.getMessage());
            return;
        } catch (IOException e1) {
            System.out.println("ERROR: " + e1.getMessage());
            return;
        } catch (GitAPIException e1) {
            System.out.println("ERROR: " + e1.getMessage());
            return;
        }

        diffSession.setDeltaCount(diff.size() + "");

        // Get the plots for each commit
        PlotCommitList<PlotLane> oldPlots = getPlotCommits(baseObjId);
        PlotCommitList<PlotLane> newPlots = getPlotCommits(newObjId);

        // Construct ordered hash sets to isolate our targets in
        LinkedHashSet<PlotCommit<PlotLane>> rejectGroup = new LinkedHashSet<PlotCommit<PlotLane>>();
        LinkedHashSet<PlotCommit<PlotLane>> acceptGroup = new LinkedHashSet<PlotCommit<PlotLane>>();

        // Add all but the actual base commit to the rejection group
        for (PlotCommit<PlotLane> item : oldPlots) {
            if (!item.getName().equalsIgnoreCase(baseObjId)) {
                rejectGroup.add(item);
            }
        }

        // Remove the commit preceeding the base commit from the filter
        // This is so that we can measure the delta created by the base commit
        if (oldPlots.size() > 1) {
            rejectGroup.remove(oldPlots.get(1));
        }

        // Basically filter out the "new" list to get only the relevant commits
        for (PlotCommit<PlotLane> item : newPlots) {
            if (!rejectGroup.contains(item)) {
                acceptGroup.add(item);
            }
        }

        // This is the most concise solution I've conjured yet. Tricky business, this is.
        // Each commit except for the first and last can effectively act as a "before"
        // and "after," each role creating entirely different results and thus need to
        // be handled mutually exclusively.
        //
        // I'm now treating this like a fence post problem in that the first time, I'll
        // make a pass where I diff the first commit against the second, etc. This makes
        // the first commit the "old" one and the second commit the "new" one. The
        // iterators happily allow me to advance things along, but a second pass will be
        // required so that the *second* commit can be the first "old" commit, then so
        // forth it will iterate until the last commit is the final "new" commit.
        //
        // In other words, as far as diffing is concerned, the first pass excludes the
        // last ("new" user-selected) commit, while the second pass excludes the first
        // user-selected commit. This exhausts all possible changes through the history
        // and provides a fully comprehensive diff report. Perhaps there's a more
        // efficient means to do this... It simply hasn't occurred to me yet.

        Iterator<PlotCommit<PlotLane>> iterator;
        iterator = acceptGroup.iterator();

        for (int i = 0; i < 2; i++) {

            while (iterator.hasNext()) {

                PlotCommit<PlotLane> current = iterator.next();
                if (iterator.hasNext()) {

                    PlotCommit<PlotLane> previous = iterator.next();

                    List<DiffEntry> diffs = diffSingleCommits(previous.getName(), current.getName());

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DiffFormatter df = new DiffFormatter(out);
                    df.setContext(0);
                    df.setRepository(this.gitInstance.getRepository());
                    df.setDetectRenames(false);
                    df.setAbbreviationLength(40);

                    for (DiffEntry aDiff : diffs) {

                        try {

                            df.format(aDiff);
                            String diffText = out.toString("UTF-8");
                            out.reset();

                            boolean useOld = false;
                            boolean useNew = false;

                            if (aDiff.getChangeType() == ChangeType.ADD) {

                                useNew = true;

                            } else if (aDiff.getChangeType() == ChangeType.DELETE) {

                                useOld = true;

                            } else if (aDiff.getChangeType() == ChangeType.MODIFY) {

                                useOld = true;
                                useNew = true;

                            }

                            // Process the old blob
                            if (useOld) {

                                if (processAsJavaFile(aDiff.getOldPath(), diffSession) == false) {
                                    continue;
                                }

                                String oldBlob = fetchBlob(previous.getId(), aDiff.getOldPath());
                                List<Integer> oldLines = oldLinesAffectedByDiff(oldBlob, diffText);
                                JavaClassModel oldClass = new JavaClassModel(oldBlob);
                                String packageName = oldClass.getPackageName();

                                for (int line : oldLines) {
                                    diffSession.addChangedMethod(packageName, oldClass.methodSignatureForLine(line));
                                }
                            }

                            // Process the new blob
                            if (useNew) {

                                if (processAsJavaFile(aDiff.getNewPath(), diffSession) == false) {
                                    continue;
                                }

                                String newBlob = fetchBlob(current.getId(), aDiff.getNewPath());

                                List<Integer> newLines = newLinesAffectedByDiff(newBlob, diffText);

                                JavaClassModel newClass = new JavaClassModel(newBlob);
                                String packageName = newClass.getPackageName();

                                for (int line : newLines) {
                                    diffSession.addChangedMethod(packageName, newClass.methodSignatureForLine(line));
                                }

                            }

                        } catch (UnsupportedEncodingException e) {
                            System.out.println("Unsupported Encoding Exception: " + e.getMessage());
                        } catch (IOException e) {
                            System.out.println("IOException: " + e.getMessage());
                        }
                    }
                }
            }

            // Reset the iterator
            iterator = acceptGroup.iterator();

            // Move past the first item, it's irrelevant now
            if (iterator.hasNext()) {
                iterator.next();
            }
        }

        // Attempt to write the output file

        FileOutputStream outputStream = null;
        try {

            File outputFile = File.createTempFile("JGitDiffTemp", ".txt");
            outputStream = new FileOutputStream(outputFile);
            outputStream.write(diffSession.getOutputString().getBytes(Charset.forName("UTF-8")));

            java.awt.Desktop.getDesktop().edit(outputFile);
            outputFile.deleteOnExit();

        } catch (IOException e) {
            System.out.println("Failed to write output file: ".concat(e.getLocalizedMessage()));
        } finally {

            // Make sure the output stream gets closed
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Determines whether the file at the given path should be processed as a Java file or not.
     * 
     * @param path
     *            The path to the file
     * @param diffSession
     *            The current diff session
     * 
     * @return Whether the file should be treated as a Java file
     */
    private static boolean processAsJavaFile(String path, DiffSession diffSession)
    {
        // If the file is JSP or SQL, it should be handed straight to the diff session
        // Otherwise, if it's not Java, reject it completed.

        if (path.matches("(.*?)\\.[jJ][sS][pP]\\s*")) {

            diffSession.addJspFile(path);

        } else if (path.matches("(.*?)\\.[sS][qQ][lL]\\s*")) {

            diffSession.addSqlFile(path);

        } else if (path.matches("(.*?)\\.[jJ][aA][vV][aA]\\s*")) {

            return true;
        }

        return false;
    }

    /**
     * Parses a commit diff blob and retrieves the lines affected by
     * changes in the old file (left side).
     * 
     * @param blob
     *            The commit blob
     * @param diffOutput
     *            The output from the diff operation
     * 
     * @return A list of modified lines
     */
    private static List<Integer> oldLinesAffectedByDiff(String blob, String diffOutput)
    {
        ArrayList<Integer> retVal = new ArrayList<Integer>();

        Matcher generalMatcher = Pattern.compile("(?<=@@\\s[-+])[0-9]*(,?\\s*)[0-9]*(\\s*[-+])[0-9]*(,?\\s*)[0-9]*(?=\\s@@)").matcher(diffOutput);

        while (generalMatcher.find()) {

            String token = generalMatcher.group().trim();
            token = token.replaceAll("[+-]", "");
            String[] splitSpace = token.split("\\s");
            String nibblet = splitSpace[0];

            if (nibblet.length() == 0) {
                continue;
            }

            if (nibblet.contains(",")) {

                String[] firstChunks = nibblet.split(",");
                int line = Integer.parseInt(firstChunks[0].replaceAll("[^0-9]*", ""));
                int range = Integer.parseInt(firstChunks[1].replaceAll("[^0-9]*", ""));

                for (int i = 0; i <= range; i++) {
                    retVal.add(Integer.valueOf(line++));
                }

            } else {
                retVal.add(Integer.valueOf(Integer.parseInt(nibblet.replaceAll("[^0-9]*", ""))));
            }
        }

        return retVal;
    }

    /**
     * Parses a commit diff blob and retrieves the lines affected by
     * changes in the new file (right side).
     * 
     * @param blob
     *            The commit blob
     * @param diffOutput
     *            The output from the diff operation
     * 
     * @return A list of modified lines
     */
    private static List<Integer> newLinesAffectedByDiff(String blob, String diffOutput)
    {
        ArrayList<Integer> retVal = new ArrayList<Integer>();

        Matcher generalMatcher = Pattern.compile("(?<=@@\\s[-+])[0-9]*(,?\\s*)[0-9]*(\\s*[-+])[0-9]*(,?\\s*)[0-9]*(?=\\s@@)").matcher(diffOutput);

        while (generalMatcher.find()) {

            String token = generalMatcher.group().trim();
            String[] splitSpace = token.split("\\s");
            if (splitSpace.length < 2) {
                continue;
            }
            String nibblet = splitSpace[1];

            if (nibblet.contains(",")) {

                String[] firstChunks = nibblet.split(",");
                int line = Integer.parseInt(firstChunks[0].replaceAll("[^0-9]*", ""));
                int range = Integer.parseInt(firstChunks[1].replaceAll("[^0-9]*", ""));

                for (int i = 0; i <= range; i++) {
                    retVal.add(Integer.valueOf(line++));
                }

            } else {
                retVal.add(Integer.valueOf(Integer.parseInt(nibblet.replaceAll("[^0-9]*", ""))));
            }
        }

        return retVal;
    }

    /**
     * Receives two commit hashes, then performs a diff on their "descendant" commit trees.
     * 
     * @param oldHash
     *            The base commit SHA-1
     * @param newHash
     *            The new commit SHA-1
     * 
     * @return All DiffEntries computed between those commits
     */
    private List<DiffEntry> diffSingleCommits(String oldHash, String newHash)
    {
        try {

            AbstractTreeIterator oldTreeParser = prepareTreeParser(this.gitInstance.getRepository(), oldHash);
            AbstractTreeIterator newTreeParser = prepareTreeParser(this.gitInstance.getRepository(), newHash);
            return this.gitInstance.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).call();

        } catch (GitAPIException e) {
            System.out.println("(1) Failed to diff SingleCommits: " + e.getMessage());
        } catch (MissingObjectException e) {
            System.out.println("(2) Failed to diff SingleCommits: " + e.getMessage());
        } catch (IncorrectObjectTypeException e) {
            System.out.println("(3) Failed to diff SingleCommits: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("(4) Failed to diff SingleCommits: " + e.getMessage());
        }

        return new ArrayList<DiffEntry>();

    }

    /**
     * Given a commit SHA-1, this returns the commit list from the given resource path.
     * 
     * @param path
     *            The resource path
     * 
     * @return The list of plot commits
     */
    public PlotCommitList<PlotLane> getPlotCommits(String path)
    {
        Repository repo = this.gitInstance.getRepository();
        PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<PlotLane>();
        PlotWalk revWalk = new PlotWalk(repo);
        try {

            ObjectId rootId = repo.resolve(path);
            if (rootId != null) {
                RevCommit root = revWalk.parseCommit(rootId);
                revWalk.markStart(root);
                plotCommitList.source(revWalk);
                plotCommitList.fillTo(Integer.MAX_VALUE);
            }

        } catch (AmbiguousObjectException ex) {

        } catch (IOException ex) {

        }
        return plotCommitList;
    }

    /**
     * Fetches a blob from the given commit object identifier at the given file path.
     * Essentially reconstructs the file at the snapshot in time of the commit.
     * 
     * @param id
     *            The commit identifier
     * @param path
     *            The path of the file for which we need the blob.
     * 
     * @return A String representation of the file
     */
    private String fetchBlob(ObjectId id, String path)
    {
        Repository repo = this.gitInstance.getRepository();

        // Makes it simpler to release the allocated resources in one go
        ObjectReader reader = repo.newObjectReader();

        try {
            // Get the commit object for that revision
            RevWalk walk = new RevWalk(reader);

            RevCommit commit = walk.parseCommit(id);

            // Get the revision's file tree
            RevTree tree = commit.getTree();
            // .. and narrow it down to the single file's path
            TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

            if (treewalk != null) {
                // use the blob id to read the file's data
                byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
                return new String(data, "utf-8");
            }

            return null;

        } catch (MissingObjectException e) {
            return "ERROR 1: " + e.getMessage();
        } catch (IncorrectObjectTypeException e) {
            return "ERROR 2: " + e.getMessage();
        } catch (IOException e) {
            return "ERROR 3: " + e.getMessage();
        } catch (NullPointerException e) {
            return null;
        } finally {
            reader.release();
        }
    }

    /**
     * Prepares a new object tree iterator from the given ref and repository.
     * 
     * @param repository
     *            The local repository housing the ref
     * @param ref
     *            The ref from which the iterator will walk
     * 
     * @return The tree iterator
     * 
     * @throws IOException
     *             if an IOException occurs
     * @throws MissingObjectException
     *             If a MissingObjectException occurs
     * @throws IncorrectObjectTypeException
     *             If an IncorrectObjectTypeException occurs
     */
    private static AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException, MissingObjectException, IncorrectObjectTypeException
    {
        RevWalk walk = new RevWalk(repository);
        RevTree tree = walk.parseTree(repository.resolve(ref));

        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
        ObjectReader oldReader = repository.newObjectReader();

        try {
            oldTreeParser.reset(oldReader, tree.getId());
        } finally {
            oldReader.release();
        }
        
        return oldTreeParser;
    }

    /**
     * @return the appWindow
     */
    protected AppWindow getAppWindow()
    {
        return this.appWindow;
    }

    /**
     * @param appWindow
     *            the appWindow to set
     */
    protected void setAppWindow(AppWindow appWindow)
    {
        this.appWindow = appWindow;
    }

    /**
     * @return the filePath
     */
    public String getRepositoryFilePath()
    {
        return this.filePath;
    }

    /**
     * @param filePath
     *            the filePath to set
     */
    public void setRepositoryFilePath(String filePath)
    {
        this.filePath = filePath;
        this.loadRepository();
    }

    /**
     * Displays an alert message upon error. Use sparingly, if possible.
     * 
     * @param message
     *            The message that should be displayed to the user
     */
    public void showAlert(String message)
    {
        JOptionPane.showMessageDialog(this.appWindow.getFrame(), message, "JGitDiff", JOptionPane.ERROR_MESSAGE);
    }

}
