package src;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static src.Utils.*;

class CommitTree {

    /********************************************************************************
     *               The File Pointers Relating to the Commit Tree                  *
     ********************************************************************************/

    static final int SHORT_UID_LENGTH = 7;
    static final int FULL_UID_LENGTH = 40;
    static final String MASTER_BRANCH_NAME = "master";
    static final String INIT_COMMIT_MSG = "initial commit";






    /********************************************************************************
     *                   Instance Variables of the Commit Tree                      *
     ********************************************************************************/

    /**
     * Hash Code of Current Working Commit.
     */
    static String head;

    /**
     * Name of the Current Branch.
     */
    static String currentBranch;

    /**
     * Branch-name Keyed Hashed Commit Nodes
     */
    static TreeMap<String, String> branches;







    /********************************************************************************
     *                   Methods that Transform the Commit Tree                     *
     ********************************************************************************/

    /**
     * Initialize the Commit Tree by placing a dummy initial commit.
     * Initial commit has no parent and its message will be "initial commit".
     * The timestamp will be that of epoch time.
     */
    static void init(boolean isRemote) {

        Commit initCommit = new Commit(INIT_COMMIT_MSG);
        commit(initCommit, isRemote);
    }

    /**
     * Submit the commit into the Commit Tree by saving its serialized file
     * in the .gitlet/commits folder.
     *
     * @param commit commit to be saved.
     */
    static void commit(Commit commit, boolean isRemote) {
        String hashedCommit = sha1(serialize(commit));
        head = hashedCommit;
        commit.saveCommit(hashedCommit, isRemote);

        String parent = commit.getParent();
        if (parent == null) {
            currentBranch = MASTER_BRANCH_NAME;
            branches = new TreeMap<>();
        } else {
            currentBranch = readCurrentBranch(isRemote);
            branches = readBranches(isRemote);
        }

        branches.put(currentBranch, hashedCommit);
        saveFields(isRemote);
    }

    /**
     * Change the head pointer to a branch name.
     * The current branch of the Commit Tree will also changed to that branch.
     * Finally both head pointer and current branch name will be saved into
     * corresponding files in .gitlet folder without serializing.
     *
     * @param branch the name of the branch head will now be pointing
     */
    static void changeHeadToBranchCommit(String branch, boolean isRemote) {
        branches = readBranches(isRemote);
        head = branches.get(branch);
        currentBranch = branch;
        saveHead(isRemote);
        saveCurrentBranch(isRemote);
    }


    static void changeHeadToCommit(String commitId, boolean isRemote) {
        head = commitId;
        saveHead(isRemote);
    }

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
    static void createBranch(String branchName, boolean isRemote) {
        branches = readBranches(isRemote);
        if (branches.containsKey(branchName)) {
            throw error("A branch with that name already exists.");
        }
        branches.put(branchName, readHead(isRemote));
        saveBranches(isRemote);
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
    static void removeBranch(String branchName, boolean isRemote) {
        branches = readBranches(isRemote);
        currentBranch = readCurrentBranch(isRemote);
        if (branchName.equals(currentBranch)) {
            throw error("Cannot remove the current branch.");
        } else if (!branches.containsKey(branchName)) {
            throw error("A branch with that name does not exist.");
        }

        branches.remove(branchName);
        saveBranches(isRemote);
    }

    /**
     * Change the specific branch pointer
     * to the commit which is saved in .gitlet/commits/commitId file.
     * @param branchName the name of the branch pointer we are changing
     * @param commitId the file name (sha1 name) of the commit in commits
     *                 folder
     */
    static void changeBranch(String branchName, String commitId, boolean isRemote) {
        branches = readBranches(isRemote);
        // currentBranch = readCurrentBranch();
        branches.put(branchName, commitId);
        saveBranches(isRemote);
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
    static Commit retrieveCommit(String hashedCommit, boolean isRemote) {
        if (hashedCommit.length() == FULL_UID_LENGTH) {
            return Commit.readCommit(hashedCommit, isRemote);
        }

        for (String commitId : plainFilenamesIn(Repository.COMMITS_FOLDER)) {
            if (hashedCommit.substring(0, SHORT_UID_LENGTH).equals(
                    commitId.substring(0, SHORT_UID_LENGTH))) {
                return Commit.readCommit(commitId, isRemote);
            }
        }
        return null;
    }

    /**
     * Return if a specific version of the file is in the current working commit.
     *
     * @param filename Name of the file
     * @param version  Hashed content currently seeking in the file
     * @return true if the version of the filename is in the current working. false otherwise.
     */
    static boolean isVersionOfFileInHead(String filename, String version, boolean isRemote) {
        Commit headCommit = readHeadCommit(isRemote);
        if (headCommit == null) {
            return false;
        }
        return headCommit.isTracking(filename, version);
    }

    /**
     * Return if the file is tracked in the current working commit.
     *
     * @param filename Name of the file
     * @return true if the file is in the current working. false otherwise.
     */
    static boolean isFileInHead(String filename, boolean isRemote) {
        Commit headCommit = readHeadCommit(isRemote);
        if (headCommit == null) {
            return false;
        }
        return headCommit.isTracking(filename);
    }

    /**
     * Return if there is the requested branch name in the Commit Tree.
     * @param branch the name of the requested branch
     * @return true if the requested branch is in the Commit Tree, false otherwise.
     */
    static boolean isABranch(String branch, boolean isRemote) {
        branches = readBranches(isRemote);
        return branches.containsKey(branch);
    }

    /**
     * Return the commitId
     * @param branch
     * @return
     */
    static String getBranchCommitId(String branch, boolean isRemote) {
        branches = readBranches(isRemote);
        return branches.get(branch);
    }

    /**
     * Return Commit Object that is branch pointer is pointing to.
     * Return null if the branch is not in the Commit Tree or
     * the serialized file is not found in .gitlet/commit.
     *
     * @param branch the name of the branch where requesting commit is the last commit
     * @return the last commit of the branch
     */
    static Commit readBranchCommit(String branch, boolean isRemote) {
        branches = readBranches(isRemote);
        String hashedCommit = branches.get(branch);
        if (hashedCommit == null) {
            return null;
        }
        return Commit.readCommit(hashedCommit, isRemote);
    }

    /**
     * Return the names of the branches in the Commit Tree.
     * @return a set of strings that represents the names of the branches.
     */
    static Set<String> getBranchNames(boolean isRemote) {
        branches = readBranches(isRemote);
        return branches.keySet();
    }

    /**
     * Return the files that the head commit is tracking.
     * @return a set of files that the head commit is tracking.
     */
    static Set<String> currentlyTrackingFiles(boolean isRemote) {
        return readHeadCommit(isRemote).getFileNames();
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
    static boolean isBranchCommitInBranchPath(String inBranchName,
                                              String outBranchName,
                                              boolean isRemote) {

        branches = readBranches(isRemote);
        String inBranchHashedCommit = branches.get(inBranchName);

        String commitHashed = branches.get(outBranchName);
        Commit commit = retrieveCommit(commitHashed, isRemote);

        while (true) {

            commitHashed = commit.getParent();
            if (commitHashed == null) {
                return false;
            } else if (commitHashed.equals(inBranchHashedCommit)) {
                return true;
            }
            commit = retrieveCommit(commitHashed, isRemote);
        }
    }

    /**
     * Find the split point commit from two branches. Before calling this method,
     * check if each branch are on the other branch's path.
     * @param branch1 the name of first branch
     * @param branch2 the name of second branch
     * @return the commit at the split point or null if branch names are invalid
     * or cannot find the split point.
     */
    static Commit getSplitPointCommit(String branch1, String branch2, boolean isRemote) {
        branches = readBranches(isRemote);
        if (!branches.containsKey(branch1) || !branches.containsKey(branch2)) {
            return null;
        }

        Set<String> branch1Ancestors = getAncestors(branches.get(branch1), isRemote);

        return getSplitPointCommit(branches.get(branch2), branch1Ancestors, isRemote);
    }


    // To squeeze out performance a little bit more
    private static Commit getSplitPointCommit(String branchHash,
                                              Set<String> otherBranchAncestors,
                                              boolean isRemote) {
        Queue<String> fringe = new ArrayDeque<>();
        Queue<String> expanded = new ArrayDeque<>();

        fringe.add(branchHash);
        while (!fringe.isEmpty()) {
            String poppedHashedCommit = fringe.remove();
            if (expanded.contains(poppedHashedCommit)) {
                continue;
            }

            expanded.add(poppedHashedCommit);
            Commit poppedCommit = retrieveCommit(poppedHashedCommit, isRemote);
            String parent = poppedCommit.getParent();
            if (parent != null) {
                if (otherBranchAncestors.contains(parent)) {
                    return retrieveCommit(parent, isRemote);
                }
                fringe.add(parent);
            }

            String secondParent = poppedCommit.getSecondParent();
            if (secondParent != null) {
                if (otherBranchAncestors.contains(secondParent)) {
                    return retrieveCommit(secondParent, isRemote);
                }
                fringe.add(secondParent);
            }
        }
        return null;
    }

    /**
     * Return the ancestors of a commit plus that commit.
     * @param hashedCommit the file name of the commit in .gitlet/commits
     * @param isRemote remote or local tree
     * @return a set of family members
     */
    static Set<String> getFamily(String hashedCommit, boolean isRemote) {
        Set<String> family = getAncestors(hashedCommit, isRemote);
        family.add(hashedCommit);
        return family;
    }

    /**
     * Return the ancestors of a commit.
     * @param hashedCommit the file name of the commit in .gitlet/commits
     * @param isRemote remote or local tree
     * @return a queue of ancestors
     */
    static Set<String> getAncestors(String hashedCommit, boolean isRemote) {
        Queue<String> fringe = new ArrayDeque<>();
        Set<String> returnQueue = new HashSet<>();
        Set<String> expanded = new HashSet<>();

        fringe.add(hashedCommit);

        while (!fringe.isEmpty()) {
            String poppedHashedCommit = fringe.remove();
            if (expanded.contains(poppedHashedCommit)) {
                continue;
            }
            expanded.add(hashedCommit);
            Commit poppedCommit = retrieveCommit(poppedHashedCommit, isRemote);
            String parent = poppedCommit.getParent();
            if (parent != null) {
                fringe.add(parent);
                // expanded.add(parent);
                returnQueue.add(parent);
            }

            String secondParent = poppedCommit.getSecondParent();
            if (secondParent != null) {
                fringe.add(secondParent);
                // expanded.add(secondParent);
                returnQueue.add(secondParent);
            }
        }

        return returnQueue;
    }

    static Set<String> getNonRepeatingFamily(String hashedCommit, boolean isRemote) {
        List<String> existingCommits;
        if (isRemote) {
            existingCommits = plainFilenamesIn(Repository.COMMITS_FOLDER);
        } else {
            existingCommits = plainFilenamesIn(Repository.REMOTE_COMMITS_FOLDER);
        }

        Set<String> family = getNonRepeatingAncestors(hashedCommit, isRemote);
        Commit commit = retrieveCommit(hashedCommit, isRemote);
        if (commit.getParent() != null && !existingCommits.contains(hashedCommit)) {
            family.add(hashedCommit);
        }

        return family;
    }

    static Set<String> getNonRepeatingAncestors(String hashedCommit, boolean isRemote) {

        Queue<String> fringe = new ArrayDeque<>();
        Set<String> returnQueue = new HashSet<>();
        Set<String> expanded = new HashSet<>();
        List<String> existingCommits;
        if (isRemote) {
            existingCommits = plainFilenamesIn(Repository.COMMITS_FOLDER);
        } else {
            existingCommits = plainFilenamesIn(Repository.REMOTE_COMMITS_FOLDER);
        }

        fringe.add(hashedCommit);

        while (!fringe.isEmpty()) {
            String poppedHashedCommit = fringe.remove();
            if (expanded.contains(poppedHashedCommit)
                    || existingCommits.contains(poppedHashedCommit)) {
                continue;
            }

            expanded.add(hashedCommit);
            Commit poppedCommit = retrieveCommit(poppedHashedCommit, isRemote);
            String parent = poppedCommit.getParent();
            if (parent != null && !existingCommits.contains(parent)) {
                /*
                if (defaultIsRemote && parent.equals(readContentsAsString(
                Repository.REMOTE_INIT))) {
                    continue;
                }
                */
                fringe.add(parent);
                // expanded.add(parent);
                returnQueue.add(parent);
            }

            String secondParent = poppedCommit.getSecondParent();
            if (secondParent != null && !existingCommits.contains(secondParent)) {
                fringe.add(secondParent);
                // expanded.add(secondParent);
                returnQueue.add(secondParent);
            }
        }

        return returnQueue;
    }





    /********************************************************************************
     *                      Methods to Save Instance Variables                      *
     ********************************************************************************/

    /**
     * Save the hashed commit pointed by HEAD into .gitlet/HEAD file without serializing.
     */
    private static void saveHead(boolean isRemote) {
        File f;
        if (isRemote) {
            f = Repository.REMOTE_HEAD;
        } else {
            f = Repository.HEAD;
        }
        try {
            f.createNewFile();
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
        writeContents(f, head);
    }

    /**
     * Save the (branch name, hashed commit) map into .gitlet/BRANCHES file by serializing.
     */
    private static void saveBranches(boolean isRemote) {
        File f;
        if (isRemote) {
            f = Repository.REMOTE_BRANCHES;
        } else {
            f = Repository.BRANCHES;
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }

        writeObject(f, branches);
    }

    /**
     * Save the name of the current branch into .gitlet/CWB without serializing.
     */
    private static void saveCurrentBranch(boolean isRemote) {
        File f;
        if (isRemote) {
            f = Repository.REMOTE_CWB;
        } else {
            f = Repository.CWB;
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }

        writeContents(f, currentBranch);
    }

    /**
     * Save head, branches, and currentBranch variables altogether.
     */
    private static void saveFields(boolean isRemote) {
        saveHead(isRemote);
        saveBranches(isRemote);
        saveCurrentBranch(isRemote);
    }






    /********************************************************************************
     *                        Methods to Get Instance Variables                     *
     ********************************************************************************/

    /**
     * Read the String from .gitlet/HEAD file. This will be the hashed commit of
     * the current working commit.
     *
     * @return The hashed commit that the head pointer is pointing to
     */
    static String readHead(boolean isRemote) {
        if (isRemote) {
            return readContentsAsString(Repository.REMOTE_HEAD);
        }
        return readContentsAsString(Repository.HEAD);
    }

    /**
     * Read (branch name, hashed commit) map from .gitlet/BRANCHES file.
     *
     * @return The (branch name, hashed commit) pairs that are currently in Commit Tree.
     */
    private static TreeMap<String, String> readBranches(boolean isRemote) {
        if (isRemote) {
            return readObject(Repository.REMOTE_BRANCHES, TreeMap.class);
        }
        return readObject(Repository.BRANCHES, TreeMap.class);
    }

    /**
     * Read the current branch name from .gitlet/CWB file.
     *
     * @return The name of the current working branch
     */
    static String readCurrentBranch(boolean isRemote) {
        if (isRemote) {
            return readContentsAsString(Repository.REMOTE_CWB);
        }

        return readContentsAsString(Repository.CWB);
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
    static Commit readHeadCommit(boolean isRemote) {
        return Commit.readCommit(readHead(isRemote), isRemote);
    }

    /**
     * Return Commit Object that Current Branch was pointing to.
     * Return null if the serialized file is not found in .gitlet/commits.
     *
     * @return The branch name of the currently working commit
     */
    static Commit readCurrentBranchCommit(boolean isRemote) {

        String hashedCommitName = readBranches(isRemote).get(readCurrentBranch(isRemote));
        return Commit.readCommit(hashedCommitName, isRemote);
    }
}




