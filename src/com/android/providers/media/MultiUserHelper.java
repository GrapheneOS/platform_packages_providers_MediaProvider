package com.android.providers.media;


import static com.android.providers.media.DatabaseBackupAndRecovery.getXattrOfLongValue;
import static com.android.providers.media.DatabaseBackupAndRecovery.setXattr;

import android.annotation.NonNull;
import android.os.UserHandle;
import android.util.Log;


public class MultiUserHelper {
    private static final String TAG = "MultiUserHelper";

    public static final String XATTR_PATH_FOR_VERSION = String.format(
            "/data/media/%s", UserHandle.myUserId());

    private static final String INTERNAL_DB_XATTR_VERSION_KEY = "user.intgosxattrversion".concat(
            String.valueOf(UserHandle.myUserId()));

    private static final String EXTERNAL_DB_XATTR_VERSION_KEY = "user.extgosxattrversion".concat(
            String.valueOf(UserHandle.myUserId()));

    /**
     * A modification of {@link DatabaseHelper#DATA_MEDIA_XATTR_DIRECTORY_PATH_OLD} so that xattrs
     * are set in specific locations.
     * For all users (full users, profiles), we generally use /data/user/[userId].
     */
    final String mDataMediaXattrDirectoryPathPerUser;

    private long mXattrSchemaVersion;

    /**
     * Do not modify the contents; this is a function for a specific xattr schema. See
     * {@link DatabaseHelper#migrateXattrKeyVersion0to1IfNeeded}
     */
    String getDataMediaXattrDirPathVersion1() {
        return String.format("/data/media/%s", UserHandle.myUserId());
    }

    private MultiUserHelper() {
        mDataMediaXattrDirectoryPathPerUser = getDataMediaXattrDirPathVersion1();
        Log.d(TAG, "mDataMediaXattrDirectoryPathPerUser " + mDataMediaXattrDirectoryPathPerUser);
    }

    enum DbType {
        INTERNAL, EXTERNAL
    }

    private String getXattrVersionKey(@NonNull DbType dbType) {
        switch (dbType) {
            case INTERNAL -> {
                return INTERNAL_DB_XATTR_VERSION_KEY;
            }
            case EXTERNAL -> {
                return EXTERNAL_DB_XATTR_VERSION_KEY;
            }
        }
        throw new IllegalStateException("unknown DbType " + dbType);
    }

    private long getXattrSchemaVersionFromDisk(@NonNull DbType dbType) {
        final var version = getXattrOfLongValue(XATTR_PATH_FOR_VERSION, getXattrVersionKey(dbType));
        if (version.isPresent()) {
            final long versionLong = version.get();
            Log.d(TAG, "getXattrSchemaVersionFromDisk(" + dbType + "): loaded version " + versionLong);
            return versionLong;
        } else {
            Log.d(TAG, "getXattrSchemaVersionFromDisk(" + dbType + "): using fresh version 0");
            return 0;
        }
    }

    long getXattrSchemaVersion(@NonNull DbType dbType) {
        final long versionFromDisk = getXattrSchemaVersionFromDisk(dbType);
        mXattrSchemaVersion = versionFromDisk;
        return versionFromDisk;
    }

    void updateXattrSchemaVersion(@NonNull DbType dbType, final long newVersion) {
        final int userId = UserHandle.myUserId();
        if (mXattrSchemaVersion != newVersion) {
            final long oldVersion = mXattrSchemaVersion;
            mXattrSchemaVersion = newVersion;
            if (setXattr(XATTR_PATH_FOR_VERSION, getXattrVersionKey(dbType), String.valueOf(newVersion))) {
                Log.d(TAG, "updateXattrSchemaVersion(" + dbType + "): migrated from version "
                        + oldVersion + " to " + newVersion + " for user " + userId);
            } else {
                Log.w(TAG, "updateXattrSchemaVersion(" + dbType + "): error setting version "
                        + "xattr from " + oldVersion + " to " + newVersion + " for user " + userId);
            }
        } else {
            Log.d(TAG, "updateXattrSchemaVersion(" + dbType + "): no migration needed for user "
                    + userId);
        }
    }

    public static MultiUserHelper create() {
        return new MultiUserHelper();
    }
}
