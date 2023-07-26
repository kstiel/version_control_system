package src;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static src.Utils.*;

public class CommandAssembler {

    static StagingArea stagingArea = new StagingArea();
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("EEE MMM F HH:mm:ss yyyy Z");

    static boolean defaultIsRemote = false;
    /********************************************************************************
     *         Methods that Assemble the Commands and Handle Inherent Errors        *
     ********************************************************************************/

    /**
     * Initialize the gitlet file system.
     * Serialize : the init commit.
     * Deserialize : None
     */
    static void init() throws IOException {

        if (Repository.GITLET_DIR.exists()) {
            throw error("A Gitlet version-control system already exists"
                    + " in the current directory.");
        }

        Repository.setupPersistence();
        stagingArea.saveStagingArea(defaultIsRemote);
        LocalTree.init();
    }

    /**
     * Staging an already-staged file overwrites the previous entry in the staging
     * area with the new contents. The staging area should be in .gitlet/stage file.
     * <p>
     * If the current working version of the file is identical to the version
     * in the current commit, do not stage it to be added, and remove it from
     * the staging area if it is already there
     * (as can happen when a file is changed, added, and then changed back to
     * it’s original version).
     * <p>
     * The file will no longer be staged for removal (see gitlet rm), if it was
     * at the time of the command.
     * <p>
     * Failure Case : If the file does not exist, print the error message "File does not exist."
     * and exit without changing anything.
     *
     * @param filename : the name of the file adding to the stage of addition
     */
    static void add(String filename) {

        stagingArea = StagingArea.readStagingArea(defaultIsRemote);
        File file = join(Repository.CWD, filename);

        if (!file.exists()) {
            throw error("File does not exist.");
        }

        String prevBlobHash = null;
        if (stagingArea.isInAddStage(filename)) {
            prevBlobHash = stagingArea.getFromAddStage(filename);
        }

        Blob blob = new Blob(readContentsAsString(file));

        boolean isVersion = LocalTree.isVersionOfFileInHead(filename, blob.getHashCode());
        if (isVersion) {
            stagingArea.deleteFromAddStage(filename);
        } else {
            stagingArea.addToAddStage(filename, blob.getHashCode());
        }

        if (stagingArea.isInRemoveStage(filename)) {
            stagingArea.deleteFromRemoveStage(filename);
        }

        // Before I save a blob in blobs folder, check if there is any unwanted blobs
        // in the folder. First look into stagingArea before it is overwritten or anything.
        if (prevBlobHash != null) {
            File deleteFile = join(Repository.BLOBS_FOLDER, prevBlobHash);
            restrictedDelete(deleteFile);
        }

        blob.saveBlob(defaultIsRemote);

        stagingArea.saveStagingArea(defaultIsRemote);
    }

    /**
     * Saves a snapshot of tracked files in the current commit and staging area so
     * they can be restored at a later time, creating a new commit.
     * The commit is said to be tracking the saved files.
     * <p>
     * By default, each commit’s snapshot of files will be exactly the same as
     * its parent commit’s snapshot of files; it will keep versions of files exactly as they are,
     * and not update them.
     * <p>
     * A commit will only update the contents of files it is tracking that have been staged
     * for addition at the time of commit, in which case the commit will now include
     * the version of the file that was staged instead of the version it got from its parent.
     * <p>
     * A commit will save and start tracking any files that were staged for addition but
     * weren’t tracked by its parent. Finally, files tracked in the current commit may be
     * untracked in the new commit as a result being staged for removal by the rm command (below).
     * <p>
     * Failure : If no files have been staged, abort. Print the message "No changes added
     * to the commit." Every commit must have a non-blank message. If it doesn’t, print the error
     * message "Please enter a commit message." It is not a failure for tracked files to be
     * missing from the working directory or changed in the working directory.
     * Just ignore everything outside the .gitlet directory entirely.
     *
     * @param message : commit message
     */
    static void commit(String message, boolean merge, String otherBranch) {

        if (message.isEmpty()) {
            throw error("Please enter a commit message.");
        }

        stagingArea = StagingArea.readStagingArea(defaultIsRemote);
        if (stagingArea.isEmpty()) {
            throw error("No changes added to the commit.");
        }

        Commit commitToSubmit;
        String secondParent = null;
        if (merge) {
            secondParent = LocalTree.getBranchCommitId(otherBranch);
        }

        commitToSubmit = new Commit(message, LocalTree.readHead(), secondParent);
        Commit parent = LocalTree.readHeadCommit();
        commitToCommit(parent, commitToSubmit);

        if (!stagingArea.isAddStageEmpty()) {
            Collection<String> stageTrackingFileNames = stagingArea.getAddStageFiles();
            loadStgAreaIntoCommit(stageTrackingFileNames, commitToSubmit, true);
        }

        if (!stagingArea.isRemoveStageEmpty()) {
            Collection<String> stageTrackingFileNames = stagingArea.getRemoveStageFiles();
            loadStgAreaIntoCommit(stageTrackingFileNames, commitToSubmit, false);
        }

        LocalTree.commit(commitToSubmit);

        stagingArea.empty();
        stagingArea.saveStagingArea(defaultIsRemote);
    }


    /**
     * Unstage the file if it is currently staged for addition.
     * <p>
     * If the file is tracked in the current commit, stage it for removal and
     * remove the file from the working directory if the user has not already done so
     * (do not remove it unless it is tracked in the current commit).
     * <p>
     * Failure : If the file is neither staged nor tracked by the head commit,
     * print the error message "No reason to remove the file."
     *
     * @param filename : the name of the file removing
     */
    static void rm(String filename) {

        stagingArea = StagingArea.readStagingArea(defaultIsRemote);

        if (!LocalTree.isFileInHead(filename) && !stagingArea.isInAddStage(filename)) {
            throw error("No reason to remove the file.");
        }

        stagingArea.deleteFromAddStage(filename);

        if (LocalTree.isFileInHead(filename)) {
            stagingArea.addToRemoveStage(filename); // because this is a set: no duplicates.

            File repoFile = join(Repository.CWD, filename);
            if (repoFile.exists()) {
                restrictedDelete(repoFile);
            }
        }

        stagingArea.saveStagingArea(defaultIsRemote);
    }

    /**
     * Starting at the current head commit, display information about each commit backwards
     * along the commit tree until the initial commit, following the first parent commit links,
     * ignoring any second parents found in merge commits.
     * (In regular Git, this is what you get with git log --first-parent).
     * <p>
     * This set of commit nodes is called the commit’s history. For every node in this history,
     * the information it should display is the commit id, the time the commit was made, and
     * the commit message. Here is an example of the exact format it should follow:
     * <p>
     * ===
     * commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee48
     * Date: Thu Nov 9 20:00:05 2017 -0800
     * A commit message.
     * <p>
     * ===
     * commit 3e8bf1d794ca2e9ef8a4007275acf3751c7170ff
     * Date: Thu Nov 9 17:01:33 2017 -0800
     * Another commit message.
     * <p>
     * ===
     * commit e881c9575d180a215d1a636545b8fd9abfb1d2bb
     * Date: Wed Dec 31 16:00:00 1969 -0800
     * initial commit
     */
    static void log() {

        String pseudoHead = LocalTree.readHead();
        do {
            printLog(pseudoHead);
            pseudoHead = LocalTree.retrieveCommit(pseudoHead).getParent();
        } while (pseudoHead != null);

    }

    /**
     * 1. java gitlet.Main checkout -- [file name]
     * Takes the version of the file as it exists in the head commit and
     * puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     * <p>
     * 2. java gitlet.Main checkout [commit id] -- [file name]
     * Takes the version of the file as it exists in the commit with the given id, and
     * puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     * <p>
     * 3. java gitlet.Main checkout [branch name]
     * Takes all files in the commit at the head of the given branch, and
     * puts them in the working directory, overwriting the versions of the files
     * that are already there if they exist. Also, at the end of this command,
     * the given branch will now be considered the current branch (HEAD).
     * Any files that are tracked in the current branch but are not present
     * in the checked-out branch are deleted. The staging area is cleared,
     * unless the checked-out branch is the current branch (see Failure cases below).
     * <p>
     * Failure cases:
     * <p>
     * If the file does not exist in the previous commit, abort,
     * printing the error message "File does not exist in that commit." Do not change the CWD.
     * <p>
     * If no commit with the given id exists, print "No commit with that id exists."
     * Otherwise, if the file does not exist in the given commit, print the same message
     * as for failure case 1. Do not change the CWD.
     * <p>
     * If no branch with that name exists, print "No such branch exists." If that branch is
     * the current branch, print No need to checkout the current branch. If a working file is
     * untracked in the current branch and would be overwritten by the checkout, print "There is
     * an untracked file in the way; delete it, or add and commit it first." and exit; perform
     * this check before doing anything else. Do not change the CWD.
     *
     * @param branchName The name of the branch from which the last commit is retrieving,
     *                   put null if the commit command does not follow the format:
     *                   commit [branch name]
     * @param commitId   The name of the file in .gitlet/commits where the interested commit
     *                   is serialized, put null if the commit command does not follow the
     *                   format: commit [commit id] -- [filename]
     * @param filename   The name of the file retrieving from a specific commit or the
     *                   current commit, put null if the commit command does not follow the
     *                   format: commit -- [filename] or commit [commit id] -- [filename]
     */
    static void checkout(String branchName, String commitId, String filename) {

        if (branchName != null) {
            checkoutWithBranch(branchName);
        } else if (commitId != null) {
            checkoutWithCommitId(commitId, filename, false);
        } else {
            checkoutWithFile(filename);
        }
    }

    /**
     * Like log, except displays information about all commits ever made.
     * The order of the commits does not matter. Hint: there is a useful
     * method in gitlet.Utils that will help you iterate over files within
     * a directory.
     */
    static void globallog() {

        List<String> commitFiles = plainFilenamesIn(Repository.COMMITS_FOLDER);
        for (String commitFile : commitFiles) {
            printLog(commitFile);
        }
    }

    /**
     * Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits, it prints the ids
     * out on separate lines. The commit message is a single operand;
     * to indicate a multiword message, put the operand in quotation marks,
     * as for the commit command below.
     * <p>
     * Failure: If no such commit exists, prints the error message "Found no commit with
     * that message."
     *
     * @param message the message we are finding in the commits
     */
    static void find(String message) {
        List<String> commitFiles = plainFilenamesIn(Repository.COMMITS_FOLDER);
        boolean isCommitExists = false;
        for (String commitFile : commitFiles) {
            String commitMessage = LocalTree.retrieveCommit(commitFile).getMessage();
            if (message.equals(commitMessage)) {
                System.out.println(commitFile);
                isCommitExists = true;
            }
        }

        if (!isCommitExists) {
            throw error("Found no commit with that message.");
        }
    }


    /**
     * Displays what branches currently exist, and marks the current branch with a *.
     * Also displays what files have been staged for addition or removal.
     * An example of the exact format it should follow is as follows.
     * <p>
     * <p>
     * === Branches ===
     * *master
     * other-branch
     * <p>
     * === Staged Files ===
     * wug.txt
     * wug2.txt
     * <p>
     * === Removed Files ===
     * goodbye.txt
     * <p>
     * === Modifications Not Staged For Commit ===
     * junk.txt (deleted)
     * wug3.txt (modified)
     * <p>
     * === Untracked Files ===
     * random.stuff
     */
    static void status() {

        String currentBranchName = LocalTree.readCurrentBranch();
        Set<String> dummySet = LocalTree.getBranchNames();

        System.out.println("=== Branches ===");
        for (String branch : dummySet) {
            if (currentBranchName.equals(branch)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println();

        stagingArea = StagingArea.readStagingArea(defaultIsRemote);
        System.out.println("=== Staged Files ===");
        dummySet = stagingArea.getAddStageFiles();
        for (String file : dummySet) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        dummySet = stagingArea.getRemoveStageFiles();
        for (String file : dummySet) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        dummySet = LocalTree.currentlyTrackingFiles();
        for (String file : dummySet) {
            boolean isChangedInWorkingDir = Repository.isChangedInWorkingDir(file,
                                                                LocalTree.readHead());
            boolean isStaged = stagingArea.isInStage(file);
            if (isChangedInWorkingDir && !isStaged) {
                System.out.println(file + " " + "(modified)");
                continue;
            }

            boolean isDeletedInCWD = !join(Repository.CWD, file).exists();
            boolean isStageForRemoval = stagingArea.isInRemoveStage(file);
            if (!isStageForRemoval && isDeletedInCWD) {
                System.out.println(file + " " + "(deleted)");
            }
        }

        dummySet = stagingArea.getAddStageFiles();
        for (String file : dummySet) {

            boolean isChangedInWorkingDir = Repository.isChangedInWorkingDir(file, stagingArea);
            if (isChangedInWorkingDir) {
                System.out.println(file + " " + "(modified)");
                continue;
            }

            boolean isDeletedInCWD = !join(Repository.CWD, file).exists();
            if (isDeletedInCWD) {
                System.out.println(file + " " + "(deleted)");
            }
        }
        System.out.println();

        System.out.println("=== Untracked Files ===");
        List<String> filesInCWD = plainFilenamesIn(Repository.CWD);
        for (String file : filesInCWD) {
            boolean isInWorkingDirectory = join(Repository.CWD, file).exists();
            boolean isStageForAddition = stagingArea.isInAddStage(file);
            boolean isCurrentlyTracking = LocalTree.isFileInHead(file);

            if (isInWorkingDirectory && !isStageForAddition && !isCurrentlyTracking) {
                System.out.println(file);
            }
        }
        System.out.println();
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
    static void branch(String branchName) {
        LocalTree.createBranch(branchName);
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
    static void removeBranch(String branchName) {
        LocalTree.removeBranch(branchName);
    }


    /**
     * Checks out all the files tracked by the given commit. Removes tracked files
     * that are not present in that commit. Also moves the current branch’s head to
     * that commit node. See the intro for an example of what happens to the head pointer
     * after using reset. The [commit id] may be abbreviated as for checkout. The staging
     * area is cleared. The command is essentially checkout of an arbitrary commit that
     * also changes the current branch head.
     *
     * Failure:  If no commit with the given id exists, print "No commit with that id exists."
     * If a working file is untracked in the current branch and would be overwritten by the reset,
     * print `There is an untracked file in the way; delete it, or add and commit it first.`
     * and exit; perform this check before doing anything else.
     * @param commitId the commitId of the commit where the head will be reset.
     */
    static void reset(String commitId) {
        Commit givenCommit = LocalTree.retrieveCommit(commitId);
        if (givenCommit == null) {
            throw error("No commit with that id exists.");
        }

        Set<String> currentlyTrackingFiles = LocalTree.currentlyTrackingFiles();
        Set<String> givenCommitFiles = givenCommit.getFileNames();

        for (String workingFile : plainFilenamesIn(Repository.CWD)) {
            if (!currentlyTrackingFiles.contains(workingFile)
                    && givenCommitFiles.contains(workingFile)) {
                throw error("There is an untracked file in the way; delete it, "
                        + "or add and commit it first.");
            }
        }

        for (String filename : givenCommitFiles) {
            checkoutWithCommitId(commitId, filename, false);
        }

        for (String currentlyTrackingFile : currentlyTrackingFiles) {
            if (!givenCommitFiles.contains(currentlyTrackingFile)) {
                File cwdFile = join(Repository.CWD, currentlyTrackingFile);
                restrictedDelete(cwdFile);
            }
        }

        LocalTree.changeHeadToCommit(commitId);
        LocalTree.changeBranch(LocalTree.readCurrentBranch(), commitId);
        stagingArea = StagingArea.readStagingArea(defaultIsRemote);
        stagingArea.empty();
        stagingArea.saveStagingArea(defaultIsRemote);
    }


    /**
     * Merges files from the given branch into the current branch.
     *
     * If the split point is the same commit as the given branch, then we do nothing;
     * the merge is complete, and the operation ends with the message "Given branch is
     * an ancestor of the current branch."
     *
     * If the split point is the current branch, then the effect is to check out the given
     * branch, and the operation ends after printing the message "Current branch fast-forwarded."
     *
     * 1. Any files that have been modified in the given branch since the split point,
     * but not modified in the current branch since the split point should be changed
     * to their versions in the given branch (checked out from the commit at the front
     * of the given branch).
     * These files should then all be automatically staged. To clarify, if a file is
     * "modified in the given branch since the split point" this means the version of
     * the file as it exists in the commit at the front of the given branch has
     * different content from the version of the file at the split point.
     * Remember: blobs are content addressable!
     *
     * 2. Any files that have been modified in the current branch but not in the given branch
     * since the split point should stay as they are.
     *
     * 3. Any files that have been modified in both the current and given branch in the same way
     * (i.e., both files now have the same content or were both removed) are left unchanged by the
     * merge. If a file was removed from both the current and given branch, but a file of the same
     * name is present in the working directory, it is left alone and continues to be absent
     * (not tracked nor staged) in the merge.
     *
     * 4. Any files that were not present at the split point and are present
     * only in the current branch should remain as they are.
     *
     * 5. Any files that were not present at the split point and
     * are present only in the given branch should be checked out and staged.
     *
     * 6. Any files present at the split point, unmodified in the current branch,
     * and absent in the given branch should be removed (and untracked).
     *
     * 7. Any files present at the split point, unmodified in the given branch,
     * and absent in the current branch should remain absent.
     *
     * 8. Any files modified in different ways in the current and given branches are in conflict.
     * "Modified in different ways" can mean that the contents of both are changed and different
     * from other, or the contents of one are changed and the other file is deleted, or the file
     * was absent at the split point and has different contents in the given and current branches.
     * In this case, replace the contents of the conflicted file with
     * <<<<<<< HEAD
     * contents of file in current branch
     * =======
     * contents of file in given branch
     * >>>>>>>
     * and stage the result. Treat a deleted file in a branch as an empty file.
     *
     * Failure:
     * 1. If there are staged additions or removals present, print the error message
     * "You have uncommitted changes." and exit.
     * 2. If a branch with the given name does not exist,
     * print the error message "A branch with that name does not exist."
     * 3. If attempting to merge a branch with itself, print the error message
     * "Cannot merge a branch with itself."
     * 4. If merge would generate an error because the commit that it does has no changes in it,
     * just let the normal commit error message for this go through.
     * 5. If an untracked file in the current commit would be overwritten or deleted by the merge,
     * print "There is an untracked file in the way; delete it, or add and commit it first."
     * and exit; perform this check before doing anything else.
     * @param branchName
     */
    static void merge(String branchName) {
        stagingArea = StagingArea.readStagingArea(defaultIsRemote);
        String currBranch;
        currBranch = readContentsAsString(Repository.CWB);

        // Failure Cases
        if (!stagingArea.isEmpty()) {
            throw error("You have uncommitted changes.");
        }

        if (!LocalTree.isABranch(branchName)) {
            throw error("A branch with that name does not exist.");
        }

        if (branchName.equals(currBranch)) {
            throw error("Cannot merge a branch with itself.");
        }

        Commit givenBranchCommit = LocalTree.readBranchCommit(branchName);
        Set<String> givenBranchFileNames = givenBranchCommit.getFileNames();

        Commit currentBranchCommit = LocalTree.readCurrentBranchCommit();
        Set<String> currBranchFileNames = currentBranchCommit.getFileNames();

        Commit splitPointCommit = LocalTree.getSplitPointCommit(branchName,
                currBranch);
        Set<String> splitPointFileNames = splitPointCommit.getFileNames();

        List<String> cwdFiles = plainFilenamesIn(Repository.CWD);
        for (String cwdFile: cwdFiles) {
            if (!currBranchFileNames.contains(cwdFile)
                    && givenBranchFileNames.contains(cwdFile)
                    && !splitPointFileNames.contains(cwdFile)) {
                throw error("There is an untracked file in the way; delete it, "
                        + "or add and commit it first.");
            }
        }

        // Real Cases
        if (LocalTree.isBranchCommitInBranchPath(branchName, currBranch)) {
            throw error("Given branch is an ancestor of the current branch.");
        }

        if (LocalTree.isBranchCommitInBranchPath(currBranch, branchName)) {
            checkoutWithBranch(branchName);
            throw error("Current branch fast-forwarded.");
        }

        boolean isConflict = false;
        isConflict = mergeCheckSplitFiles(branchName,
                splitPointFileNames, splitPointCommit,
                givenBranchFileNames, givenBranchCommit,
                currBranchFileNames, currentBranchCommit,
                isConflict);


        // Case 5
        isConflict = mergeCheckGivenFiles(branchName,
                splitPointFileNames, splitPointCommit,
                givenBranchFileNames, givenBranchCommit,
                currBranchFileNames, currentBranchCommit,
                isConflict);

        stagingArea.saveStagingArea(defaultIsRemote);

        // Case 3 :/
        // Case 7 :/

        String logMsg = "Merged " + branchName + " into "
                + LocalTree.readCurrentBranch() + ".";
        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        commit(logMsg, true, branchName);
    }


    static void addRemote(String remoteName, String relativePath) throws IOException {
        String fixedPath = relativePath.replace('/', File.separatorChar);
        Remote.saveRemote(remoteName, fixedPath);
    }

    static void removeRemote(String remoteName) {
        Remote.deleteRemote(remoteName);
    }

    static void remotePush(String remoteName, String remoteBranchName) throws IOException {
        // Assign remote paths
        assignRemotePaths(remoteName);

        // If local repo doesn't have the given branch name.
        if (!LocalTree.isABranch(remoteBranchName)) {
            throw error(remoteBranchName + " is not a branch is local repository.");
        }

        String localBranchCommitId = LocalTree.getBranchCommitId(remoteBranchName);

        // If the remote name is in the repository, check if its pointing commit
        // is in the path (ancestors) of the local branch
        // If there is no such branch, create a branch (temporarily pointing remote head)
        boolean isRemoteInPathOfLocal;
        if (RemoteTree.isABranch(remoteBranchName)) {
            String remoteBranchHeadId = RemoteTree.getBranchCommitId(remoteBranchName);
            isRemoteInPathOfLocal = LocalTree.getAncestors(
                    LocalTree.getBranchCommitId(remoteBranchName)).contains(remoteBranchHeadId);

            if (!isRemoteInPathOfLocal) {
                throw error("Please pull down remote changes before pushing.");
            }

        } else {
            RemoteTree.createBranch(remoteBranchName);
            // RemoteTree.changeBranch(remoteBranchName, localBranchCommitId);
            // the above is factored out
        }

        RemoteTree.changeBranch(remoteBranchName, localBranchCommitId);

        // I should still be able to push from non head pointing branch
        Set<String> localNonRepeatingFamily = LocalTree.getNonRepeatingingFamily(
                localBranchCommitId);

        // For each commit copied
        for (String hashedCommit : localNonRepeatingFamily) {
            File localCommit = join(Repository.COMMITS_FOLDER, hashedCommit);
            Path localCommitPath = localCommit.toPath();
            File remoteCommit = join(Repository.REMOTE_COMMITS_FOLDER, hashedCommit);
            Path remoteCommitPath = remoteCommit.toPath();
            Files.copy(localCommitPath, remoteCommitPath);
            // handle diff init case
            /*
            Commit commit = readObject(localCommit, Commit.class);
            if (readContentsAsString(Repository.INIT).equals(commit.getParent())) {
                commit.setParent(readContentsAsString(Repository.REMOTE_INIT));
                writeObject(remoteCommit, commit);
            }
            */

            // Copy the corresponding file blobs
            Set<String> localTrackingFiles = LocalTree.retrieveCommit(hashedCommit).getFileNames();
            for (String fileName : localTrackingFiles) {
                String hashedBlob = LocalTree.retrieveCommit(hashedCommit).getFromFileBlob(
                        fileName);
                File localFile = join(Repository.BLOBS_FOLDER, hashedBlob);
                File remoteFile = join(Repository.REMOTE_BLOBS_FOLDER, hashedBlob);
                if (!remoteFile.exists()) {
                    Path localBlobPath = localFile.toPath();
                    Path remoteBlobPath = remoteFile.toPath();
                    Files.copy(localBlobPath, remoteBlobPath);
                }
            }
        }

        // If the remote head is at remote branch, change the head (reset).
        // otherwise leave the head and current branch pointer.
        if (LocalTree.getAncestors(LocalTree.getBranchCommitId(remoteBranchName)).contains(
                RemoteTree.readHead())) {
            remoteReset(localBranchCommitId);
        }

    }

    static void remoteFetch(String remoteName, String remoteBranchName) throws IOException {

        // Assign remote paths
        assignRemotePaths(remoteName);

        // If local repo doesn't have the given branch name.
        if (!RemoteTree.isABranch(remoteBranchName)) {
            throw error("That remote does not have that branch.");
        }

        String remoteBranchCommitId = RemoteTree.getBranchCommitId(remoteBranchName);

        // If the remote name is in the repository, check if its pointing commit
        // is in the path (ancestors) of the local branch
        // If there is no such branch, create a branch (temporarily pointing remote head)
        if (!LocalTree.isABranch(remoteBranchName)) {
            LocalTree.createBranch(remoteBranchName);
            // RemoteTree.changeBranch(remoteBranchName, localBranchCommitId);
            // the above is factored out
        }

        LocalTree.changeBranch(remoteName + "/" + remoteBranchName, remoteBranchCommitId);

        // I should still be able to push from non head pointing branch
        Set<String> remoteNonRepeatingFamily = RemoteTree.getNonRepeatingFamily(
                remoteBranchCommitId);

        // For each commit copied
        for (String hashedCommit : remoteNonRepeatingFamily) {
            File remoteCommit = join(Repository.REMOTE_COMMITS_FOLDER, hashedCommit);
            Path remoteCommitPath = remoteCommit.toPath();
            File localCommit = join(Repository.COMMITS_FOLDER, hashedCommit);
            Path localCommitPath = localCommit.toPath();
            Files.copy(remoteCommitPath, localCommitPath);
            // handle diff init case
            /*
            Commit commit = readObject(remoteCommit, Commit.class);
            if (readContentsAsString(Repository.REMOTE_INIT).equals(commit.getParent())) {
                commit.setParent(readContentsAsString(Repository.INIT));
                writeObject(localCommit, commit);
            }
            */

            // Copy the corresponding file blobs
            Set<String> remoteTrackingFiles = RemoteTree.retrieveCommit(
                    hashedCommit).getFileNames();
            for (String fileName : remoteTrackingFiles) {
                String hashedBlob = RemoteTree.retrieveCommit(hashedCommit).getFromFileBlob(
                        fileName);
                File remoteFile = join(Repository.REMOTE_BLOBS_FOLDER, hashedBlob);
                File localFile = join(Repository.BLOBS_FOLDER, hashedBlob);
                if (!localFile.exists()) {
                    Path localBlobPath = localFile.toPath();
                    Path remoteBlobPath = remoteFile.toPath();
                    Files.copy(remoteBlobPath, localBlobPath);
                }
            }
        }

    }

    static void remotePull(String remoteName, String remoteBranchName) throws IOException {
        remoteFetch(remoteName, remoteBranchName);
        merge(remoteName + "/" + remoteBranchName);

    }

    private static void assignRemotePaths(String remoteName) {

        Repository.REMOTE_CWD = join(Remote.readRemote(remoteName), "..");
        Repository.REMOTE_GITLET_DIR = new File(Remote.readRemote(remoteName));
        if (!Repository.REMOTE_GITLET_DIR.exists()) {
            throw error("Remote directory not found.");
        }
        Repository.REMOTE_HEAD = join(Repository.REMOTE_GITLET_DIR, Repository.HEAD_NAME);
        Repository.REMOTE_CWB = join(Repository.REMOTE_GITLET_DIR, Repository.CWB_NAME);
        Repository.REMOTE_BLOBS_FOLDER = join(Repository.REMOTE_GITLET_DIR,
                Repository.BLOBS_FOLDER_NAME);
        Repository.REMOTE_BRANCHES = join(Repository.REMOTE_GITLET_DIR, Repository.BRANCHES_NAME);
        Repository.REMOTE_COMMITS_FOLDER = join(Repository.REMOTE_GITLET_DIR,
                Repository.COMMITS_FOLDER_NAME);
        Repository.REMOTE_STAGE = join(Repository.REMOTE_GITLET_DIR, Repository.STAGE_NAME);
        Repository.REMOTE_REMOTE = join(Repository.REMOTE_GITLET_DIR, Repository.REMOTE_NAME);
        // Repository.REMOTE_INIT = join(Repository.REMOTE_GITLET_DIR, Repository.INIT_NAME);
    }


    private static void remoteReset(String commitId) {
        Commit givenCommit = RemoteTree.retrieveCommit(commitId);
        if (givenCommit == null) {
            throw error("No commit with that id exists.");
        }

        Set<String> currentlyTrackingFiles = RemoteTree.currentlyTrackingFiles();
        Set<String> givenCommitFiles = givenCommit.getFileNames();

        for (String workingFile : plainFilenamesIn(Repository.REMOTE_CWD)) {
            if (!currentlyTrackingFiles.contains(workingFile)
                    && givenCommitFiles.contains(workingFile)) {
                throw error("There is an untracked file in the way; delete it, "
                        + "or add and commit it first.");
            }
        }

        for (String filename : givenCommitFiles) {
            checkoutWithCommitId(commitId, filename, true);
        }

        for (String currentlyTrackingFile : currentlyTrackingFiles) {
            if (!givenCommitFiles.contains(currentlyTrackingFile)) {
                File cwdFile = join(Repository.REMOTE_CWD, currentlyTrackingFile);
                restrictedDelete(cwdFile);
            }
        }

        RemoteTree.changeHeadToCommit(commitId);
        RemoteTree.changeBranch(RemoteTree.readCurrentBranch(), commitId);
        stagingArea = StagingArea.readStagingArea(true);
        stagingArea.empty();
        stagingArea.saveStagingArea(true);
    }




    /********************************************************************************
     *                            Private Helper Methods                            *
     ********************************************************************************/

    /**
     * 3. java gitlet.Main checkout [branch name]
     * Takes all files in the commit at the head of the given branch, and
     * puts them in the working directory, overwriting the versions of the files
     * that are already there if they exist. Also, at the end of this command,
     * the given branch will now be considered the current branch (HEAD).
     * Any files that are tracked in the current branch but are not present
     * in the checked-out branch are deleted. The staging area is cleared,
     * unless the checked-out branch is the current branch (see Failure cases below).
     * <p>
     * If no branch with that name exists, print "No such branch exists." If that branch is
     * the current branch, print No need to checkout the current branch. If a working file is
     * untracked in the current branch and would be overwritten by the checkout, print "There is
     * an untracked file in the way; delete it, or add and commit it first." and exit; perform
     * this check before doing anything else. Do not change the CWD.
     *
     * @param branchName The name of the branch from which the commit is retrieving
     */

    private static void checkoutWithBranch(String branchName) {

        if (!LocalTree.isABranch(branchName)) {
            throw error("No such branch exists.");
        }

        if (branchName.equals(LocalTree.readCurrentBranch())) {
            throw error("No need to checkout the current branch.");
        }

        Commit currCommit = LocalTree.readCurrentBranchCommit();
        Set<String> currCommitFileNames = currCommit.getFileNames();
        List<String> cwdFiles = plainFilenamesIn(Repository.CWD);

        Commit branchCommit = LocalTree.readBranchCommit(branchName);
        Set<String> branchCommitFileNames = branchCommit.getFileNames();

        for (String f : cwdFiles) {
            if (!currCommitFileNames.contains(f) && branchCommitFileNames.contains(f)) {
                throw error("There is an untracked file in the way; delete it,"
                        + " or add and commit it first.");
            }
        }


        for (String f : branchCommitFileNames) {
            Blob copyBlob = Blob.readBlob(branchCommit.getFromFileBlob(f), defaultIsRemote);
            File replaceFile = join(Repository.CWD, f);
            try {
                replaceFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            writeContents(join(Repository.CWD, f), copyBlob.getContent());
        }

        for (String f : currCommitFileNames) {
            if (!branchCommitFileNames.contains(f)) {
                restrictedDelete(f);
            }
        }

        stagingArea = StagingArea.readStagingArea(defaultIsRemote);
        stagingArea.empty();
        stagingArea.saveStagingArea(defaultIsRemote);
        LocalTree.changeHeadToBranchCommit(branchName);
    }

    /**
     * 2. java gitlet.Main checkout [commit id] -- [file name]
     * Takes the version of the file as it exists in the commit with the given id, and
     * puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     * <p>
     * If no commit with the given id exists, print "No commit with that id exists."
     * Otherwise, if the file does not exist in the given commit, print the same message
     * as for failure case 1. Do not change the CWD.
     *
     * @param commitId the id of the commit from which the file is retrieving
     * @param filename the retrieving file name
     */
    private static void checkoutWithCommitId(String commitId, String filename, boolean isRemote) {

        File workingDirectory;
        if (isRemote) {
            workingDirectory = Repository.REMOTE_CWD;
        } else {
            workingDirectory = Repository.CWD;
        }

        File replaceFile = join(workingDirectory, filename);
        Commit commit;

        if (isRemote) {
            commit = RemoteTree.retrieveCommit(commitId);
        } else {
            commit = LocalTree.retrieveCommit(commitId);
        }

        if (commit == null) {
            throw error("No commit with that id exists.");
        }
        if (!commit.isTracking(filename)) {
            throw error("File does not exist in that commit.");
        }

        try {
            replaceFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Blob copyBlob = Blob.readBlob(commit.getFromFileBlob(filename), isRemote);
        writeContents(join(workingDirectory, filename), copyBlob.getContent());

    }

    /**
     * 1. java gitlet.Main checkout -- [file name]
     * Takes the version of the file as it exists in the head commit and
     * puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     * <p>
     * If the file does not exist in the previous commit, abort,
     * printing the error message "File does not exist in that commit." Do not change the CWD.
     */
    private static void checkoutWithFile(String filename) {
        Blob copyBlob = Blob.readBlob(LocalTree.readHeadCommit().getFromFileBlob(filename),
                defaultIsRemote);
        File replaceFile = join(Repository.CWD, filename);
        try {
            if (!replaceFile.exists()) {
                throw error("File does not exist in that commit.");
            }
            replaceFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeContents(join(Repository.CWD, filename), copyBlob.getContent());
    }

    /**
     * Move staged filename-blob pair into the commit.
     * @param fileNames The filenames from specific stage area : addStage or removeSatge
     * @param commit    The commit where filenames-hashedblob pairs will be inserted,
     *                  with filenames from stage.
     * @param isAdd     The boolean flag to indicate if we are adding to the commit.
     */
    private static void loadStgAreaIntoCommit(Collection<String> fileNames,
                                              Commit commit,
                                              boolean isAdd) {
        for (String filename : fileNames) {
            String hashedBlob = stagingArea.getFromAddStage(filename);
            if (isAdd) {
                commit.addToFileBlob(filename, hashedBlob);
            } else {
                commit.removeFromFileBlob(filename);
            }
        }
    }

    /**
     * Copy filenames-hashedblob pair from "from" to "to".
     * Attention : All commit saves filename-hashedblob pair.
     *
     * @param from : Commit Object where filename-hashedblob pair is being copied from.
     * @param to   : Commit object where the copied content of "from" is being copied to.
     */
    private static void commitToCommit(Commit from, Commit to) {
        Collection<String> fromTrackingFileNames = from.getFileNames();
        if (fromTrackingFileNames == null) {
            return;
        }
        for (String filename : fromTrackingFileNames) {
            String hashedBlob = from.getFromFileBlob(filename);
            to.addToFileBlob(filename, hashedBlob);
        }
    }

    /**
     * Print the information about a commit according to spec format.
     *
     * @param filename the name of the file where commit is in .gitlet/commits
     */
    private static void printLog(String filename) {
        Commit currCommit = LocalTree.retrieveCommit(filename);
        System.out.println("===");
        System.out.println("commit " + filename);
        if (currCommit.getSecondParent() != null) {
            System.out.println("Merge: " + currCommit.getParent().substring(0, 7)
                    + " " + currCommit.getSecondParent().substring(0, 7));
        }
        System.out.println("Date: " + DATE_FORMAT.format(currCommit.getTimestamp()));
        System.out.println(currCommit.getMessage());
        System.out.println();
    }


    private static void mergeConflictWrite(String target,
                                           String curContent, String givenContent) {

        File f = join(Repository.CWD, target);
        String newContent = "<<<<<<< HEAD" + "\n" + curContent
                + "=======" + "\n" + givenContent + ">>>>>>>\n";

        new Blob(newContent).saveBlob(defaultIsRemote);
        stagingArea.addToAddStage(target, sha1(newContent));
        writeContents(f, newContent);
    }

    private static boolean mergeCheckGivenFiles(String branchName,
                                                Set<String> splitPointFileNames,
                                                Commit splitPointCommit,
                                                Set<String> givenBranchFileNames,
                                                Commit givenBranchCommit,
                                                Set<String> currBranchFileNames,
                                                Commit currentBranchCommit,
                                                boolean isConflict) {
        boolean prevIsConflict = isConflict;
        for (String givenBranchFile : givenBranchFileNames) {

            String givenBranchHashedContent = givenBranchCommit.getFromFileBlob(
                    givenBranchFile);

            if (!splitPointFileNames.contains(givenBranchFile)
                    && !currBranchFileNames.contains(givenBranchFile)) {

                String branchCommitId = LocalTree.getBranchCommitId(branchName);

                checkoutWithCommitId(branchCommitId, givenBranchFile, false);
                stagingArea.addToAddStage(givenBranchFile, givenBranchHashedContent);
            }

            // Case 8 the file was absent at the split point and has different contents
            // in the given and current branches
            if (!splitPointFileNames.contains(givenBranchFile)
                    && currBranchFileNames.contains(givenBranchFile)) {

                prevIsConflict = true;
                String givenBranchContent = Blob.readBlob(
                        givenBranchHashedContent, defaultIsRemote).getContent();
                String currBranchHashedContent = currentBranchCommit.getFromFileBlob(
                        givenBranchFile);
                String currBranchContent = Blob.readBlob(
                        currBranchHashedContent, defaultIsRemote).getContent();
                mergeConflictWrite(givenBranchFile, currBranchContent, givenBranchContent);
            }
        }
        return prevIsConflict;
    }

    private static boolean mergeCheckSplitFiles(String branchName,
                        Set<String> splitPointFileNames, Commit splitPointCommit,
                        Set<String> givenBranchFileNames, Commit givenBranchCommit,
                        Set<String> currBranchFileNames, Commit currentBranchCommit,
                                                boolean isConflict) {
        boolean prevIsConflict = isConflict;
        for (String splitPointFileName : splitPointFileNames) {
            String splitBranchHashedContent = splitPointCommit.getFromFileBlob(
                    splitPointFileName);
            if (givenBranchFileNames.contains(splitPointFileName)
                    && currBranchFileNames.contains(splitPointFileName)) {

                String givenBranchHashedContent = givenBranchCommit.getFromFileBlob(
                        splitPointFileName);
                String currBranchHashedContent = currentBranchCommit.getFromFileBlob(
                        splitPointFileName);


                if (!givenBranchHashedContent.equals(splitBranchHashedContent)
                        && currBranchHashedContent.equals(splitBranchHashedContent)) {

                    String branchCommitId = LocalTree.getBranchCommitId(branchName);
                    checkoutWithCommitId(branchCommitId, splitPointFileName, false);
                    stagingArea.addToAddStage(splitPointFileName, givenBranchHashedContent);
                }

                if (!givenBranchHashedContent.equals(splitBranchHashedContent)
                        && !currBranchHashedContent.equals(splitBranchHashedContent)
                        && !currBranchHashedContent.equals(givenBranchHashedContent)) {
                    prevIsConflict = true;
                    String currBranchContent = Blob.readBlob(
                            currBranchHashedContent, defaultIsRemote).getContent();
                    String givenBranchContent = Blob.readBlob(
                            givenBranchHashedContent, defaultIsRemote).getContent();
                    mergeConflictWrite(splitPointFileName, currBranchContent, givenBranchContent);
                }
            }

            if (givenBranchFileNames.contains(splitPointFileName)
                    && !currBranchFileNames.contains(splitPointFileName)) {

                String givenBranchHashedContent = givenBranchCommit.getFromFileBlob(
                        splitPointFileName);

                if (!givenBranchHashedContent.equals(splitBranchHashedContent)) {
                    prevIsConflict = true;
                    String givenBranchContent = Blob.readBlob(
                            givenBranchHashedContent, defaultIsRemote).getContent();
                    mergeConflictWrite(splitPointFileName, "", givenBranchContent);
                }
            }

            if (currBranchFileNames.contains(splitPointFileName)
                    && !givenBranchFileNames.contains(splitPointFileName)) {

                String currBranchHashedContent = currentBranchCommit.getFromFileBlob(
                        splitPointFileName);

                if (!currBranchHashedContent.equals(splitBranchHashedContent)) {
                    prevIsConflict = true;
                    String currBranchContent = Blob.readBlob(
                            currBranchHashedContent, defaultIsRemote).getContent();
                    mergeConflictWrite(splitPointFileName, currBranchContent, "");
                }
            }

            if (!givenBranchFileNames.contains(splitPointFileName)
                    && currBranchFileNames.contains(splitPointFileName)) {

                String currBranchHashedContent = currentBranchCommit.getFromFileBlob(
                        splitPointFileName);

                if (currBranchHashedContent.equals(splitBranchHashedContent)) {
                    restrictedDelete(join(Repository.CWD, splitPointFileName));
                    stagingArea.addToRemoveStage(splitPointFileName);
                }
            }
        }
        return prevIsConflict;
    }

    public static void main(String[] args) {
        File cwd = join(new File(System.getProperty("user.dir")), "..", "..", "f.txt");
        System.out.println(cwd.exists());

    }
}
