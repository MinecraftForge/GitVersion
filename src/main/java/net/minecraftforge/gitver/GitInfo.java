package net.minecraftforge.gitver;

import java.io.Serializable;

/**
 * Represents information about a git repository. This can be used to access other information when the standard
 * versioning methods in {@link GitVersion} do not suffice.
 */
public final class GitInfo implements Serializable {
    public static final GitInfo EMPTY = new GitInfo();

    String tag;
    String offset;
    String hash;
    String branch;
    String commit;
    String abbreviatedId;
    String url;

    GitInfo() { }

    public String getTag() {
        return this.tag;
    }

    public String getOffset() {
        return this.offset;
    }

    public String getHash() {
        return this.hash;
    }

    public String getBranch() {
        return this.branch;
    }

    public String getBranch(boolean versionFriendly) {
        if (!versionFriendly || this.branch == null) return this.branch;

        var branch = this.branch;
        if (branch.startsWith("pulls/"))
            branch = "pr" + Util.rsplit(branch, "/", 1)[1];
        return branch.replaceAll("[\\\\/]", "-");
    }

    public String getCommit() {
        return this.commit;
    }

    public String getAbbreviatedId() {
        return this.abbreviatedId;
    }

    public String getUrl() {
        return this.url;
    }
}
