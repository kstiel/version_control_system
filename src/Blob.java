package src;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static src.Utils.*;

public class Blob implements Serializable {



    /* Instance Variables */
    String content;
    String hashCode;

    /**
     * The Blob constructor. This will save the content of the blob and the content's hash code.
     * @param content the content of this file
     */
    public Blob(String content) {
        this.content = content;
        hashCode = sha1(this.content);
    }

    /**
     * Get the content of this Blob.
     * @return the content of this Blob.
     */
    public String getContent() {
        return content;
    }

    /**
     * Get the hashed content of this Blob.
     * @return the hashed content of this Blob
     */
    String getHashCode() {
        return hashCode;
    }

    /**
     * Serialize this blob to a file in the .gitlet/blobs folder.
     */
    void saveBlob(boolean isRemote) {
        File f;
        if (isRemote) {
            f = join(Repository.REMOTE_BLOBS_FOLDER, hashCode);
        } else {
            f = join(Repository.BLOBS_FOLDER, hashCode);
        }

        try {
            f.createNewFile();
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
        writeObject(f, this);
    }

    /**
     * Deserialize the Blob object from the file.
     * @param hashedContent the name of the blob in .gitlet/blobs folder
     * @return the Blob object after deserializing the file in corresponding path, null if failed.
     */
    static Blob readBlob(String hashedContent, boolean isRemote) {
        File f;
        if (isRemote) {
            f = join(Repository.REMOTE_BLOBS_FOLDER, hashedContent);
        } else {
            f = join(Repository.BLOBS_FOLDER, hashedContent);
        }

        if (!f.exists()) {
            return null;
        }

        return readObject(f, Blob.class);
    }
}
