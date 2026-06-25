package dev.tslib;

/**
 * A TeamSpeak channel (immutable snapshot).
 */
public class Channel {
    public final long id;
    public final long parentId;
    public final String name;
    public final String topic;
    public final String description;
    public final int order;
    public final boolean isPermanent;
    public final boolean isSemiPermanent;
    public final boolean isDefault;
    public final boolean hasPassword;
    public final byte codec;
    public final byte codecQuality;
    public final int maxClients;
    public final int maxFamilyClients;
    public final int neededTalkPower;
    public final long iconId;
    public final long permissionHints;

    // Permission hint bit flags
    public static final long PERM_FILE_UPLOAD = 64;
    public static final long PERM_FILE_DOWNLOAD = 128;
    public static final long PERM_FILE_DELETE = 256;
    public static final long PERM_FILE_RENAME = 512;
    public static final long PERM_FILE_BROWSE = 1024;
    public static final long PERM_FILE_DIRECTORY_CREATE = 2048;

    public Channel(long id, long parentId, String name, String topic,
                   String description, int order, boolean isPermanent,
                   boolean isSemiPermanent, boolean isDefault,
                   boolean hasPassword, byte codec, byte codecQuality,
                   int maxClients, int maxFamilyClients,
                   int neededTalkPower, long iconId, long permissionHints) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.topic = topic;
        this.description = description;
        this.order = order;
        this.isPermanent = isPermanent;
        this.isSemiPermanent = isSemiPermanent;
        this.isDefault = isDefault;
        this.hasPassword = hasPassword;
        this.codec = codec;
        this.codecQuality = codecQuality;
        this.maxClients = maxClients;
        this.maxFamilyClients = maxFamilyClients;
        this.neededTalkPower = neededTalkPower;
        this.iconId = iconId;
        this.permissionHints = permissionHints;
    }

    public Channel(long id, long parentId, String name, String topic,
                   String description, int order, boolean isPermanent,
                   boolean isSemiPermanent, boolean isDefault,
                   boolean hasPassword, byte codec, byte codecQuality,
                   int maxClients, int maxFamilyClients,
                   int neededTalkPower, long iconId) {
        this(id, parentId, name, topic, description, order, isPermanent,
             isSemiPermanent, isDefault, hasPassword, codec, codecQuality,
             maxClients, maxFamilyClients, neededTalkPower, iconId, 0L);
    }

    public boolean canUploadFile() {
        return (permissionHints & PERM_FILE_UPLOAD) != 0;
    }

    public boolean canDownloadFile() {
        return (permissionHints & PERM_FILE_DOWNLOAD) != 0;
    }

    public boolean canDeleteFile() {
        return (permissionHints & PERM_FILE_DELETE) != 0;
    }

    public boolean canRenameFile() {
        return (permissionHints & PERM_FILE_RENAME) != 0;
    }

    public boolean canBrowseFiles() {
        return (permissionHints & PERM_FILE_BROWSE) != 0;
    }

    public boolean canCreateDirectory() {
        return (permissionHints & PERM_FILE_DIRECTORY_CREATE) != 0;
    }

    @Override
    public String toString() {
        return "Channel(id=" + id + ", name='" + name + "')";
    }
}
