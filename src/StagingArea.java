package src;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static src.Utils.*;

class StagingArea implements Serializable {

    /********************************************************************************
     *               The File Pointers Relating to the Commit Tree                  *
     ********************************************************************************/

    /** The file path of the staging area starting from the currently working directory. */








    /********************************************************************************
     *                            Instance Variables                                *
     ********************************************************************************/

    /** The stage for addition */
    private Map<String, String> addStage;

    /** The stage for removal */
    private Set<String> removeStage;






    /********************************************************************************
     *                        Constructors of the Commit Class                      *
     ********************************************************************************/

    /**
     * Create a staging area to prepare for commiting.
     */
    StagingArea() {
        addStage = new TreeMap<>();
        removeStage = new TreeSet<>();
    }






    /********************************************************************************
     *                Methods that Change Information in this Commit                *
     ********************************************************************************/

    /**
     * Add the file name with its hashed content to the stage of addition in the staging area.
     * @param filename the file name being added to stage of addition
     * @param hashedContent the hashed content of the file
     */
    void addToAddStage(String filename, String hashedContent) {
        addStage.put(filename, hashedContent);
    }

    /**
     * Add the file name to the removal stage in the staging area.
     * @param filename the file name being added to stage of removal
     */
    void addToRemoveStage(String filename) {
        removeStage.add(filename);
    }

    /**
     * Delete from the removal stage in the staging area.
     * @param filename the name of the removing file
     */
    void deleteFromRemoveStage(String filename) {
        removeStage.remove(filename);
    }

    /**
     * Delete from the stage of addition in the staging area.
     * @param filename the name of the removing file
     */
    void deleteFromAddStage(String filename) {
        addStage.remove(filename);
    }

    /**
     * Clear the staging area, i.e., both the stage of addition and the removal stage
     */
    void empty() {
        addStage.clear();
        removeStage.clear();
    }






    /********************************************************************************
     *               Methods that Retrieve Information in this Commit               *
     ********************************************************************************/

    /**
     * Return the file names in the stage of addition.
     * @return a set of file names in the stage of addition
     */
    Set<String> getAddStageFiles() {
        return addStage.keySet();
    }

    /**
     * Return the file names in the removal stage.
     * @return a list of file names in the removal stage.
     */
    Set<String> getRemoveStageFiles() {
        return removeStage;
    }

    /**
     * Return if the stage of addition is empty.
     * @return true if the stage of addition is empty, false otherwise.
     */
    boolean isAddStageEmpty() {
        return addStage.isEmpty();
    }

    /**
     * Return if the removal stage is empty.
     * @return true if the removal stage is empty, false otherwise.
     */
    boolean isRemoveStageEmpty() {
        return removeStage.isEmpty();
    }

    /**
     * Return if the stage area itself is empty.
     * @return true if the stage area is empty, false otherwise.
     */
    boolean isEmpty() {
        return isAddStageEmpty() && isRemoveStageEmpty();
    }

    /**
     * Return if the file is in removal stage.
     * @param filename the name of the file
     * @return true if the file is in removal stage, false otherwise.
     */
    boolean isInRemoveStage(String filename) {
        return removeStage.contains(filename);
    }

    /**
     * Return if the file is in stage of addition.
     * @param filename the name of the file
     * @return true if the file is in stage of addition, false otherwise.
     */
    boolean isInAddStage(String filename) {
        return addStage.containsKey(filename);
    }

    /**
     *
     */
    boolean isVersionInAddStage(String filename, String version) {
        return addStage.containsKey(filename) && addStage.containsValue(version);
    }

    /**
     * Return hashed-content of the file from the stage of addition,
     * null if there is no such mapping.
     * @param filename the name of the file
     * @return the hashed content of the file, null if there is no such hashed content
     */
    String getFromAddStage(String filename) {
        return addStage.get(filename);
    }

    boolean isInStage(String filename) {
        return isInAddStage(filename) || isInRemoveStage(filename);
    }






    /********************************************************************************
     *                    Methods to Save the StagingArea Object                    *
     ********************************************************************************/

    /**
     * Serialize this StagingArea to .gitlet/STAGE file.
     */
    void saveStagingArea(boolean isRemote) {
        File f;
        if (isRemote) {
            f = Repository.REMOTE_STAGE;
        } else {
            f = Repository.STAGE;
        }

        try {
            f.createNewFile();
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }

        writeObject(f, this);
    }






    /**
     * Deserialize the StagingArea object from STAGE file
     * @return the StagingArea object after deserializing the file in corresponding path,
     * null if failed.
     */

    /********************************************************************************
     *                     Methods to Read the Commit Object                        *
     ********************************************************************************/
    static StagingArea readStagingArea(boolean isRemote) {
        File f;
        if (isRemote) {
            f = Repository.REMOTE_STAGE;
        } else {
            f = Repository.STAGE;
        }

        if (!f.exists()) {
            return null;
        }

        return readObject(f, StagingArea.class);
    }
}
