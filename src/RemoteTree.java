package src;

import java.util.*;

import static src.Utils.*;

class RemoteTree {


    static final boolean IS_REMOTE = true;
    /********************************************************************************
     *                   Methods that Transform the Commit Tree                     *
     ********************************************************************************/

    /**
     * Submit the commit into the Commit Tree by saving its serialized file
     * in the .gitlet/commits folder.
     *
     * @param commit commit to be saved.
     */
    /*
    static void commit(Commit commit) {
        CommitTree.commit(commit, IS_REMOTE);
    }

     */

    /**
     * Change the head pointer to a branch name.
     * The current branch of the Commit Tree will also changed to that branch.
     * Finally both head pointer and current branch name will be saved into
     * corresponding files in .gitlet folder without serializing.
     *
     * branch the name of the branch head will now be pointing
     */
    /*
    static void changeHeadToBranch(String branch) {
        CommitTree.changeHeadToBranchCommit(branch, IS_REMOTE);
    }

     */

    static void changeHeadToCommit(String commitId) {
        CommitTree.changeHeadToCommit(commitId, IS_REMOTE);
    }


    /**
     * Initialize the Commit Tree by placing a dummy initial commit.
     * Initial commit has no parent and its message will be "initial commit".
     * The timestamp will be that of epoch time.
     */
    /*
    static void init() {
        CommitTree.init(IS_REMOTE);
    }
     */

    /**
     * Creates a new branch with the given name, and points it at the current head commit.
     * A branch is nothing more than a name for a reference (a SHA-1 identifier) to a commit node.
     * This command does NOT immediately switch to the newly created branch (just as in real Git).
     * Before you ever call branch, your code should be running with a default branch
     * called "master"
     *
     * Failure: If a branch with the given name already exists, print the error message
     * "A branch with that name already exists."
     *
     * @param branchName The name of the new branch
     */
    static void createBranch(String branchName) {
        CommitTree.createBranch(branchName, IS_REMOTE);
    }

    /**
     * Deletes the branch with the given name. This only means to delete
     * the pointer associated with the branch; it does not mean to delete
     * all commits that were created under the branch, or anything like that.
     *
     * Failure: If a branch with the given name does not exist, aborts.
     * Print the error message "A branch with that name does not exist."
     * If you try to remove the branch you’re currently on, aborts,
     * printing the error message "Cannot remove the current branch."
     *
     * @param branchName the name of the removing branch
     */
    /*
    static void removeBranch(String branchName) {
        CommitTree.removeBranch(branchName, IS_REMOTE);
    }
     */

    /**
     * Change the commit, that a specific branch pointer is pointing to,
     * to the commit which is saved in .gitlet/commits/commitId file.
     * @param branchName the name of the branch pointer we are changing
     * @param commitId the file name (sha1 name) of the commit in commits
     *                 folder
     */
    static void changeBranch(String branchName, String commitId) {
        CommitTree.changeBranch(branchName, commitId, IS_REMOTE);
    }






    /********************************************************************************
     *        Methods that Retrieve Information from the Commit Tree by User        *
     *        Note: See also Read Methods                                           *
     ********************************************************************************/

    /**
     * Return the commit object read from the file name in .gitlet/commits folder.
     * The file name can also be the shorter version of first 7 characters of the
     * real file name.
     * Return null if no such file is found.
     *
     * @param hashedCommit : The file name of the serialized commit in .gitlet/commits folder.
     * @return The commit object read from the file name in .gitlet/commits folder,
     * null if the file is not found
     */
    static Commit retrieveCommit(String hashedCommit) {
        return CommitTree.retrieveCommit(hashedCommit, IS_REMOTE);
    }

    /**
     * Return if a specific version of the file is in the current working commit.
     *
     * @param filename Name of the file
     * @param version  Hashed content currently seeking in the file
     * @return true if the version of the filename is in the current working. false otherwise.
     */
    /*
    static boolean isVersionOfFileInHead(String filename, String version) {
        return CommitTree.isVersionOfFileInHead(filename, version, IS_REMOTE);
    }

     */

    /**
     * Return if the file is tracked in the current working commit.
     *
     * @param filename Name of the file
     * @return true if the file is in the current working. false otherwise.
     */
    /*
    static boolean isFileInHead(String filename) {
        return CommitTree.isFileInHead(filename, IS_REMOTE);
    }

     */

    /**
     * Return if there is the requested branch name in the Commit Tree.
     * @param branch the name of the requested branch
     * @return true if the requested branch is in the Commit Tree, false otherwise.
     */
    static boolean isABranch(String branch) {
        return CommitTree.isABranch(branch, IS_REMOTE);
    }

    /**
     * Return the commitId
     * @param branch
     * @return
     */
    static String getBranchCommitId(String branch) {
        return CommitTree.getBranchCommitId(branch, IS_REMOTE);
    }

    /**
     * Return Commit Object that is branch pointer is pointing to.
     * Return null if the branch is not in the Commit Tree or
     * the serialized file is not found in .gitlet/commit.
     *
     * @param branch the name of the branch where requesting commit is the last commit
     * @return the last commit of the branch
     */
    /*
    static Commit readBranchCommit(String branch) {
        return CommitTree.readBranchCommit(branch, IS_REMOTE);
    }

     */

    /**
     * Return the names of the branches in the Commit Tree.
     * @return a set of strings that represents the names of the branches.
     */
    /*
    static Set<String> getBranchNames() {
        return CommitTree.getBranchNames(IS_REMOTE);
    }

     */

    /**
     * Return the files that the head commit is tracking.
     * @return a set of files that the head commit is tracking.
     */
    static Set<String> currentlyTrackingFiles() {
        return CommitTree.currentlyTrackingFiles(IS_REMOTE);
    }

    /**
     * Return if the inBranchName pointing commit is in the path of commits
     * starting from the outBranhName pointing commit
     * @param inBranchName the name of the branch in question of if its pointing
     *                     commit is in the path
     * @param outBranchName the name of the branch in question of if its path commits
     *                      contains the inBranchName commit
     * @return true if the commit is in the commit path, false otherwise.
     */
    /*
    static boolean isBranchCommitInBranchPath(String inBranchName, String outBranchName) {
        return CommitTree.isBranchCommitInBranchPath(inBranchName, outBranchName, IS_REMOTE);
    }

     */

    /**
     * Find the split point commit from two branches. Before calling this method,
     * check if each branch are on the other branch's path.
     *  branch1 the name of first branch
     *  branch2 the name of second branch
     * @return the commit at the split point or null if branch names are invalid
     * or cannot find the split point.
     */
    /*
    static Commit getSplitPointCommit(String branch1, String branch2) {
        return CommitTree.getSplitPointCommit(branch1, branch2, IS_REMOTE);
    }

    static Set<String> getNonRepeatingAncestors(String hashedCommit) {
        return CommitTree.getNonRepeatingAncestors(hashedCommit, IS_REMOTE);
    }

    static Set<String> getAncestors(String hashedCommit) {
        return CommitTree.getAncestors(hashedCommit, IS_REMOTE);
    }
    */


    static Set<String> getNonRepeatingFamily(String hashedCommit) {
        return CommitTree.getNonRepeatingFamily(hashedCommit, IS_REMOTE);
    }








    /********************************************************************************
     *                      Methods to Save Instance Variables                      *
     ********************************************************************************/







    /********************************************************************************
     *                        Methods to Get Instance Variables                     *
     ********************************************************************************/

    /**
     * Read the String from .gitlet/HEAD file. This will be the hashed commit of
     * the current working commit.
     *
     * @return The hashed commit that the head pointer is pointing to
     */
    static String readHead() {
        return readContentsAsString(Repository.REMOTE_HEAD);
    }

    /**
     * Read the current branch name from .gitlet/CWB file.
     *
     * @return The name of the current working branch
     */
    static String readCurrentBranch() {
        return readContentsAsString(Repository.REMOTE_CWB);
    }






    /********************************************************************************
     *              Methods to Read Objects from Instance Variables                 *
     ********************************************************************************/

    /**
     * Return Commit Object that HEAD is pointing to.
     * Return null if the serialized file is not found in .gitlet/commits.
     *
     * @return Commit Object that the head pointer is pointing to, null if fails to retrieve
     */
    /*
    static Commit readHeadCommit() {
        return CommitTree.readHeadCommit(IS_REMOTE);
    }

     */

    /**
     * Read (branch name, hashed commit) map from .gitlet/BRANCHES file.
     *
     * @return The (branch name, hashed commit) pairs that are currently in Commit Tree.
     */
    /*
    private static TreeMap<String, String> readBranches() {
        return readObject(Repository.REMOTE_BRANCHES, TreeMap.class);
    }

     */

    /**
     * Return Commit Object that Current Branch was pointing to.
     * Return null if the serialized file is not found in .gitlet/commits.
     *
     * @return The branch name of the currently working commit
     */
    /*
    static Commit readCurrentBranchCommit() {

        String hashedCommitName = readBranches().get(readCurrentBranch());
        return Commit.readCommit(hashedCommitName, IS_REMOTE);
    }

     */

}
