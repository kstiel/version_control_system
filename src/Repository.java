package src;

// import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.io.IOException;

import static src.Utils.*;


/** Represents a gitlet repository.
 *
 *  @author KAUNG SI THU
 *
 * .gitlet/ -- top level folder for all persistent data
 *    - commits/ -- folder containing all of the persistent data for commits
 *    - fileBlobs/ -- folder contianing all of the persistent data for fileBlobs
 *    - head -- file that saves the hash code of the commit node that header points to.
 *    - stage -- file that saves the staging area.
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    static final String GITLET_NAME = ".gitlet";
    static final String BLOBS_FOLDER_NAME = "blobs";
    static final String COMMITS_FOLDER_NAME = "commits";
    static final String STAGE_NAME = "STAGE";
    static final String HEAD_NAME = "HEAD";
    static final String CWB_NAME = "CWB";
    static final String BRANCHES_NAME = "BRANCHES";
    static final String REMOTE_NAME = "remote";
    // static final String INIT_NAME = "INIT";
    /********************************************************************************
     *                         Current Working Repository                           *
     ********************************************************************************/

    static final File CWD = new File(System.getProperty("user.dir"));
    static final File GITLET_DIR = join(CWD, GITLET_NAME);
    static final File BLOBS_FOLDER = join(GITLET_DIR, BLOBS_FOLDER_NAME);
    static final File COMMITS_FOLDER = join(GITLET_DIR, COMMITS_FOLDER_NAME);
    static final File STAGE = join(GITLET_DIR, STAGE_NAME);
    static final File HEAD = join(GITLET_DIR, HEAD_NAME);
    static final File CWB = join(GITLET_DIR, CWB_NAME);
    static final File BRANCHES = join(GITLET_DIR, BRANCHES_NAME);
    static final File REMOTE = join(GITLET_DIR, REMOTE_NAME);
    // static final File INIT = join(GITLET_DIR, INIT_NAME);


    /********************************************************************************
     *                          Remote Working Repository                           *
     ********************************************************************************/
    static File REMOTE_CWD;
    static File REMOTE_GITLET_DIR;
    static File REMOTE_BLOBS_FOLDER;
    static File REMOTE_COMMITS_FOLDER;
    static File REMOTE_STAGE;
    static File REMOTE_HEAD;
    static File REMOTE_CWB;
    static File REMOTE_BRANCHES;
    static File REMOTE_REMOTE;
    // static File REMOTE_INIT;



    // Reminder : Make every .gitlet things under this class

    static void setupPersistence() throws IOException {
        GITLET_DIR.mkdir();
        BLOBS_FOLDER.mkdir();
        COMMITS_FOLDER.mkdir();
        REMOTE.mkdir();
        STAGE.createNewFile();
        HEAD.createNewFile();
        BRANCHES.createNewFile();
        CWB.createNewFile();
    }


    /**
     * Return if the working dir file has different content from the commit tracking version
     * @param filename the name of the file whose content is in interest
     * @param commitFile the file name of saved commit in .gitlet/commits folder
     * @return true if the file contents are different, false if they are different
     * or the file doesn't exist in the working directory or in the commit.
     */
    static boolean isChangedInWorkingDir(String filename, String commitFile) {

        boolean isRemote = false;
        File cwdFile = join(CWD, filename);
        if (!cwdFile.exists()) {
            return false;
        }

        Commit commit = CommitTree.retrieveCommit(commitFile, isRemote);
        String hashedContent = commit.getFromFileBlob(filename);

        if (hashedContent == null) {
            return false;
        }

        String contentInCommit = Blob.readBlob(hashedContent, isRemote).getContent();
        String currentContent = readContentsAsString(cwdFile);
        return !currentContent.equals(contentInCommit);
    }

    /**
     * Return if the working dir file has different content from the staged version
     * @param filename the file name whose content is in interest
     * @param stgArea the staging area
     * @return true if the file contents are different, false if they are different
     * or the file doesn't exist in the working directory or in the staging area.
     */
    static boolean isChangedInWorkingDir(String filename, StagingArea stgArea) {
        File cwdFile = join(CWD, filename);
        if (!cwdFile.exists()) {
            return false;
        }

        if (stgArea.isInStage(filename)) {
            return false;
        }

        Blob cwdBlob = new Blob(readContentsAsString(cwdFile));
        return !stgArea.isVersionInAddStage(filename, cwdBlob.getHashCode());
    }


    static void checkGitletRepo() {
        if (!GITLET_DIR.exists()) {
            throw error("Not in an initialized Gitlet directory.");
        }
    }


}
