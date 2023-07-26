package src;

import static src.Utils.*;

import java.io.File;
import java.io.IOException;


class Remote {


    /**
     * Save the remote file path (relative to .gitlet dir), in a file
     * with the given name.
     * @param remoteName Give name to the remote repo
     * @param filePath The file path of the repo/.gitlet relative
     *                 current .gitlet. This path will be saved
     *                 as it is.
     */
    static void saveRemote(String remoteName, String filePath) throws IOException {
        File f = join(Repository.REMOTE, remoteName);
        if (f.exists()) {
            throw error("A remote with that name already exists.");
        }

        f.createNewFile();

        writeContents(f, filePath);
    }

    static void deleteRemote(String remoteName) {
        File f = join(Repository.REMOTE, remoteName);
        if (!f.exists()) {
            throw error("A remote with that name does not exist.");
        }

        f.delete();
    }

    static String readRemote(String remoteName) {
        File f = join(Repository.REMOTE, remoteName);
        if (!f.exists()) {
            throw error("A remote with that name does not exist.");
        }

        return readContentsAsString(f);
    }

    static boolean isExist(String remoteName) {
        File f = join(Repository.REMOTE, remoteName);
        return f.exists();
    }

    /**
     * If the remote branch’s head is not in the history of the current local head,
     * print the error message "Please pull down remote changes before pushing."
     * If the remote .gitlet directory does not exist, print "Remote directory not found."
     */
    /*
    static void remotePush(String remoteName, String remoteBranchName) {
        File f = join(REMOTE, remoteName);
        if (!f.exists()) {
            throw error("Remote directory not found.");
        }

        if ()
    }

    static
    */
}
