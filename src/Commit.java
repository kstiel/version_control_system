package src;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static src.Utils.*;


/** Represents a gitlet commit object.
 *
 *  @author KAUNG SI THU
 */

class Commit implements Serializable {


    /********************************************************************************
     *                  The File Pointers Relating to the Commit                    *
     ********************************************************************************/
    /** Directory of "commits" folder */

    static final String EPOCH = "00:00:00 UTC, Thursday, 1 January 1970";

    
    
    
    
    /********************************************************************************
     *                            Instance Variables                                *
     ********************************************************************************/

    /** The message of this commit. */
    private String message;

    /** The timestamp of this commit. */
    private Date timestamp;

    /** Hashed parent commit. */
    private List<String> parents;

    /** file-name keyed hashed-blob-content. */
    private TreeMap<String, String> fileBlobs;


    
    
    
    
    /********************************************************************************
     *                        Constructors of the Commit Class                      *
     ********************************************************************************/

    /**
     * Commit Constructor. Timestamp will be saved according to the commit time
     * with epoch time for the commit with null parent. 
     * For init commit, pass parent as null.
     * @param message commit message
     * @param parents the parents of this commit
     */
    // If parent empty, it is init commit. If parent 1, its parent is parent 1 and so on.
    Commit(String message, String... parents) {
        this.message = message;
        DateFormat df = new SimpleDateFormat("\\w\\w\\w \\w\\w\\w \\d+ \\d\\d:\\d\\d:\\d\\d \\d\\d\\d\\d [-+]\\d\\d\\d\\d");
        if (parents.length == 0) {
            timestamp = new Date(0);
        } else if (parents.length >= 1) {
            timestamp = new Date();
        }

        if (parents.length > 2) {
            throw error("Gitlet only allows atmost 2 parents");
        }

        this.parents = new ArrayList<>();
        this.parents.addAll(Arrays.asList(parents));

        fileBlobs = new TreeMap<>();
    }






    
    /********************************************************************************
     *                Methods that Change Information in this Commit                *
     ********************************************************************************/


    /**
     * Add (file name, hashed blob content) pair to this commit.
     * @param filename the name of the file this commit will be tracking
     * @param hashedContent the hashed content of the file
     */
    void addToFileBlob(String filename, String hashedContent) {
        fileBlobs.put(filename, hashedContent);
    }

    /**
     * Remove the filename from tracking list of this commit.
     * @param filename the name of the file this commit will be no longer 
     *                 be tracking.
     */
    void removeFromFileBlob(String filename) {
        fileBlobs.remove(filename);
    }




    
    
    /********************************************************************************
     *               Methods that Retrieve Information in this Commit               *
     ********************************************************************************/

    /**
     * Return the first parent of this commit, null if no such parent exists.
     * @return Hashed first-parent of this commit, null if it does not exists.
     */
    String getParent() {
        if (!this.parents.isEmpty()) {
            return parents.get(0);
        }
        return null;
    }

    /**
     * Return the second parent of this commit, null if no such parent exists.
     * @return Hashed second-parent of this commit, null if it does no exists.
     */
    String getSecondParent() {
        if (this.parents.size() >= 2) {
            return parents.get(1);
        }
        return null;
    }
    
    /**
     * Return the time stamp of this commit.
     * @return Date object representing the time stamp of this commit
     */
    Date getTimestamp() { 
        return this.timestamp; 
    }
    
    /**
     * Return the message log of this commit.
     * @return the message String of this commit
     */
    String getMessage() { 
        return this.message; 
    }
    
    /**
     * Return the file names this commit is tracking.
     * @return the file names this commit is tracking as Set<String>
     */
    Set<String> getFileNames() {
        return fileBlobs.descendingKeySet();
    }

    /**
     * Get the file name from tracking list of this commit, will return null
     * if no such file exists. 
     * @param filename the requested name of the file in this commit 
     * @return the hashed content of the file name requested, null if no such file exists
     */
    String getFromFileBlob(String filename) {
        return fileBlobs.get(filename);
    }

    /**
     * Return if this commit is currently tracking this version of the file.
     * Note : if this commit is head commit, call this through Commit Tree.
     * @param filename : Name of the file that is being seeked in the commit
     * @param version : Hashed content of the requested file
     * @return true if this specific filename-version pair is in the commit, 
     * false otherwise.
     */
    boolean isTracking(String filename, String version) {
        if (!fileBlobs.containsKey(filename)) {
            return false;
        }
        return fileBlobs.get(filename).equals(version);
    }

    /**
     * Return if this commit is currently tracking this file without concerning .
     * Note : if this commit is head commit, call this through Commit Tree.
     * the version of the file.
     * @param filename : Name of the file that is being seeked in the commit
     * @return true if this specific filename is in the commit. false otherwise.
     */
    boolean isTracking(String filename) {
        return fileBlobs.containsKey(filename);
    }
    


    
    
    
    
    /********************************************************************************
     *                     Methods to Read the Commit Object                        *
     ********************************************************************************/

    /**
     * Deserialize the Commit object from the filename. You should be doing this though 
     * the Commit Tree, i.e., acheive similar performance through Commit Tree class.
     * @param hashedCommitName the name of the commit in .gitlet/commits folder 
     * @return the commit object after deserializing the file in corresponding path,
     * null if failed.
     */
    static Commit readCommit(String hashedCommitName, boolean isRemote) {

        File f;
        if (isRemote) {
            f = join(Repository.REMOTE_COMMITS_FOLDER, hashedCommitName);
        } else {
            f = join(Repository.COMMITS_FOLDER, hashedCommitName);
        }

        if (!f.exists()) {
            return null;
        }
        return readObject(f, Commit.class);
    }





    
    
    /********************************************************************************
     *                       Methods to Save the Commit Object                      *
     ********************************************************************************/
    
    /**
     * Serialize this commit to a file in the .gitlet/commits folder. You should be
     * doing this though the Commit Tree, i.e., acheive similar performance through
     * Commit Tree class.
     * @param hashedCommitName The file name where the commit is being seriablized to;
     *                 should be sha1 name.
     */
    void saveCommit(String hashedCommitName, boolean isRemote) {

        File f;
        if (isRemote) {
            f = join(Repository.REMOTE_COMMITS_FOLDER, hashedCommitName);
        } else {
            f = join(Repository.COMMITS_FOLDER, hashedCommitName);
        }

        try {
            f.createNewFile();
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }

        writeObject(f, this);
    }
}
