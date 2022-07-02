/*
 * Copyright (C) 2022 GrapheneOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.providers.media;

import android.annotation.Nullable;
import android.app.StorageScope;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.GosPackageState;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import com.android.providers.media.util.DatabaseUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.content.pm.GosPackageState.DFLAG_EXPECTS_ALL_FILES_ACCESS;
import static android.content.pm.GosPackageState.DFLAG_EXPECTS_STORAGE_WRITE_ACCESS;
import static android.content.pm.GosPackageState.FLAG_STORAGE_SCOPES_ENABLED;

class StorageScopesHooks {

    private static final String TAG = "StorageScopesHooks";

    // Media#getFilesInDirectoryForFuse(String path, int uid),
    // when the caller doesn't have any storage permission
    static String[] obtainDirContents(MediaProvider mp, final String dirPath,
                                      Uri volumeUri, final String dirRelativePath,
                                      LocalCallingIdentity callingIdentity, ArrayList<String> fileNamesList)
    {
        /*
        Upstream MediaProvider allows apps that don't have any storage permission to read the
        list of all external storage directories.

        Address this metadata leak by returning only those directories that either
         - have files that the app created
         - visible via StorageScopes
         */

        Bundle queryArgs = new Bundle(3);
        //  ESCAPE '\\' is needed by DatabaseUtils.escapeForLike() below
        final String selection = MediaColumns.DATA + " LIKE ? ESCAPE '\\' and " + MediaColumns.MIME_TYPE + " not like 'null'";

        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, new String[] { DatabaseUtils.escapeForLike(dirPath) + "/%" });
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_GROUP_BY, MediaColumns.RELATIVE_PATH);

        final String[] projection = { MediaColumns.RELATIVE_PATH };

        final ArraySet<String> dirEntries;
        final ArrayList<String> dirNamesList;

        try (final Cursor cursor = mp.query(volumeUri, projection, queryArgs, null)) {
            dirNamesList = new ArrayList<>(cursor.getCount() + StorageScope.maxArrayLength() + 1);

            // needed to ensure uniqueness of all dir entries (both files and dirs)
            dirEntries = new ArraySet<>(fileNamesList.size() + dirNamesList.size());
            dirEntries.addAll(fileNamesList);

            if (dirRelativePath.equals("/")) {
                // "Android" dir should always be visible for compatibility
                maybeAddDirEntry("Android", dirEntries, dirNamesList);
            }

            while (cursor.moveToNext()) {
                String childRelativePath = cursor.getString(0);

                String childDirName = maybeExtractChildDirNameFromRelativePaths(
                        dirRelativePath, childRelativePath);

                if (childDirName != null) {
                    maybeAddDirEntry(childDirName, dirEntries, dirNamesList);
                }
            }
        }

        StorageScope[] scopes = callingIdentity.getStorageScopes();

        if (scopes != null) {
            for (StorageScope scope : scopes) {
                maybeExtractChildNameFromStorageScope(dirPath, scope,
                        dirEntries, dirNamesList, fileNamesList);
            }
        }

        fileNamesList.ensureCapacity(1 + dirNamesList.size());

        // separator between files and dirs
        fileNamesList.add("");

        fileNamesList.addAll(dirNamesList);

        return fileNamesList.toArray(new String[0]);
    }

    // #shouldBypassFuseRestrictions(boolean forWrite, String filePath)
    static boolean isAllowedPath(String filePath, LocalCallingIdentity callingIdentity, boolean forWrite) {
        return isAllowedPath(callingIdentity.getStorageScopes(), filePath, forWrite);
    }

    private static boolean isAllowedPath(@Nullable StorageScope[] scopes, @Nullable String filePath, boolean forWrite) {
        if (scopes == null || filePath == null) {
            return false;
        }

        for (StorageScope scope : scopes) {
            if (forWrite && !scope.isWritable()) {
                continue;
            }

            String scopePath = scope.path;

            if (scope.isDirectory()) {
                if (filePath.startsWith(scopePath)) {
                    int len = scopePath.length();
                    if (filePath.length() == len) {
                        // exact match
                        return true;
                    }
                    if (filePath.charAt(len) == '/') {
                        // filePath is a sub-path of scopePath
                        return true;
                    }
                }
            } else if (scope.isFile()) {
                if (filePath.equals(scopePath)) {
                    // in case a directory was created with the same name
                    if (new File(filePath).isFile()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /*
    Allow to perform write operations that are normally allowed only if the app holds the
    "All files access" permission, but only if these operations don't affect files that the app
    doesn't have access to.

    #isOpendirAllowedForFuse, #isDirectoryCreationOrDeletionAllowedForFuse: allow to create
    top-level directories, allow to delete empty top-level directories

    #ensureFileColumns, #insertFileIfNecessaryForFuse: allow to create files of any type in all
    accessible directories

    #renameForFuse: allow to rename files / directories in (and into) the root of external storage,
    but only if all affected files are writable by the app
     */
    static boolean shouldRelaxWriteRestrictions(LocalCallingIdentity callingIdentity) {
        GosPackageState ps = callingIdentity.getGosPackageState();
        int requiredDflags = DFLAG_EXPECTS_ALL_FILES_ACCESS | DFLAG_EXPECTS_STORAGE_WRITE_ACCESS;
        return ps != null
                && ps.hasFlag(FLAG_STORAGE_SCOPES_ENABLED)
                && (ps.derivedFlags & requiredDflags) == requiredDflags;
    }

    private static final ThreadLocal<Boolean> queryBuilderHookInhibited = new ThreadLocal<>();

    static void inhibitQueryBuilderHook() {
        queryBuilderHookInhibited.set(Boolean.TRUE);
    }

    static void uninhibitQueryBuilderHook() {
        queryBuilderHookInhibited.set(Boolean.FALSE);
    }

    /*
    An app that doesn't have a storage permission is allowed to see the files that it contributed
    itself. MediaProvider implements this by recording the app's package name and matching against
    it for later queries / updates / deletes. Note that the MediaProvider enforces permissions
    per uid and thus matches against the list of all package names that share the given uid.
    In vast majority of cases there is only one package name per uid.

    This hook modifies the SQL clause that matches against the list of shared packages to also
    include the list of app's StorageScopes.
     */
    // #getQueryBuilderInternal
    static String maybeModifyMatchSharedPackagesClause(String orig, LocalCallingIdentity callingIdentity, boolean forWrite) {
        StorageScope[] scopes = callingIdentity.getStorageScopes();
        if (scopes == null) {
            return orig;
        }

        if (queryBuilderHookInhibited.get() == Boolean.TRUE) {
            return orig;
        }

        String fragment = sqlFragment(scopes, callingIdentity, forWrite);
        if (TextUtils.isEmpty(fragment)) { // empty fragments are cached too
            return orig;
        }

        StringBuilder sb = new StringBuilder(orig.length() + fragment.length() + 6);
        sb.append('(');
        sb.append(orig);
        sb.append(" OR ");
        sb.append(fragment);
        sb.append(')');

        return sb.toString();
    }

    // PermissionActivity, after shouldShowActionDialog() returns true
    static boolean shouldSkipConfirmationDialog(Context context, String packageName, List<Uri> uris) {
        if (uris.size() > 10) {
            // checking each Uri is slow when there's too many of them
            return false;
        }

        GosPackageState ps = GosPackageState.get(packageName);
        if (ps == null) {
            return false;
        }

        if (!(ps.hasDerivedFlag(GosPackageState.DFLAG_HAS_MANAGE_MEDIA_DECLARATION) && ps.hasFlag(FLAG_STORAGE_SCOPES_ENABLED))) {
            return false;
        }

        StorageScope[] scopes = StorageScope.deserializeArray(ps);

        String[] projection = { MediaColumns.DATA };
        for (Uri uri : uris) {
            try {
                try (Cursor c = context.getContentResolver().query(uri, projection, null, null)) {
                    if (c == null || !c.moveToFirst()) {
                        return false;
                    }

                    if (!isAllowedPath(scopes, c.getString(0), true)) {
                        return false;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "", e);
                return false;
            }
        }

        return true;
    }

    // #openFileAndEnforcePathPermissionsHelper
    static boolean shouldBypassMediaLocationPermissionCheck(LocalCallingIdentity callingIdentity, File file) {
        GosPackageState ps = callingIdentity.getGosPackageState();

        if (ps == null || !ps.hasDerivedFlag(GosPackageState.DFLAG_HAS_ACCESS_MEDIA_LOCATION_DECLARATION)) {
            return false;
        }

        // ACCESS_MEDIA_LOCATION permission is auto-granted on stock OS when READ_EXTERNAL_STORAGE or
        // MANAGE_EXTERNAL_STORAGE are granted. Not spoofing it for StorageScopes paths would lead to
        // SecurityExceptions in apps that use MediaStore#setRequireOriginal(uri)

        return isAllowedPath(file.getAbsolutePath(), callingIdentity, false);
    }

    private static String sqlFragment(StorageScope[] scopes, LocalCallingIdentity callingIdentity, boolean forWrite) {
        String cache = forWrite ? callingIdentity.storageScopesSqlFragmentForWrite : callingIdentity.storageScopesSqlFragment;
        if (cache != null) {
            return cache;
        }

        final int scopeCount = scopes.length;

        if (scopeCount == 0) {
            return null;
        }

        boolean atLeastOneReadOnly = false;

        StorageScope[] dirs = null;
        int dirCount = 0;
        StorageScope[] files = null;
        int fileCount = 0;

        int sbCapacityEstimate = 0;

        for (int scopeIdx = 0; scopeIdx < scopeCount; ++scopeIdx) {
            StorageScope scope = scopes[scopeIdx];
            if (!scope.isWritable()) {
                atLeastOneReadOnly = true;

                if (forWrite) {
                    continue;
                }
            }

            int pathLen = scope.path.length();

            if (scope.isDirectory()) {
                if (dirs == null) {
                    dirs = new StorageScope[scopeCount - scopeIdx];
                }
                dirs[dirCount++] = scope;
                sbCapacityEstimate += pathLen + 25;
            } else if (scope.isFile()) {
                if (files == null) {
                    files = new StorageScope[scopeCount - scopeIdx];
                }
                files[fileCount++] = scope;
                sbCapacityEstimate += pathLen + 10;
            }
        }

        StringBuilder sb = new StringBuilder(sbCapacityEstimate);

        if (dirCount != 0) {
            for (int dirIdx = 0; dirIdx < dirCount; ++dirIdx) {
                if (dirIdx != 0) {
                    sb.append(" OR ");
                }
                StorageScope dir = dirs[dirIdx];
                String path = dir.path;

                sb.append(MediaColumns.DATA + " LIKE '");

                // unfortunately, binding arguments is unsupported by the SQLiteQueryBuilder

                for (int i = 0, m = path.length(); i < m; ++i) {
                    char c = path.charAt(i);
                    switch (c) {
                        case '%':
                        case '_':
                        case '\\':
                            sb.append('\\');
                            break;
                        case '\'':
                            sb.append('\'');
                            break;
                    }
                    sb.append(c);
                }
                sb.append("/%' ESCAPE '\\'");
            }
        }

        if (fileCount != 0) {
            if (dirCount != 0) {
                sb.append(" OR ");
            }

            sb.append(MediaColumns.DATA + " IN (");

            for (int fileIdx = 0; fileIdx < fileCount; ++fileIdx) {
                if (fileIdx != 0) {
                    sb.append(',');
                }

                StorageScope file = files[fileIdx];

                String path = file.path;

                sb.append('\'');

                for (int i = 0, m = path.length(); i < m; ++i) {
                    char c = path.charAt(i);
                    if (c == '\'') {
                        // escape '
                        sb.append('\'');
                    }
                    sb.append(c);
                }

                sb.append('\'');
            }

            sb.append(')');
        }

        String res = sb.toString();

        if (forWrite) {
            callingIdentity.storageScopesSqlFragmentForWrite = res;
            if (!atLeastOneReadOnly) {
                callingIdentity.storageScopesSqlFragment = res;
            }
        } else {
            callingIdentity.storageScopesSqlFragment = res;
            if (!atLeastOneReadOnly) {
                callingIdentity.storageScopesSqlFragmentForWrite = res;
            }
        }
        return res;
    }

    // MediaProvider doesn't enforce validity of file paths in all cases, which means that the
    // values of MediaColumns.DATA and MediaColumns.RELATIVE_PATH columns might be malformed
    private static boolean validateDirEntryName(String n) {
        return n.length() != 0
                && n.indexOf('/') < 0
                && n.indexOf('\0') < 0;
    }

    private static void maybeAddDirEntry(String dirEntry, ArraySet<String> dirEntries, ArrayList<String> dest) {
        if (!validateDirEntryName(dirEntry)) {
            Log.e(TAG, "invalid dirEntry name" + dirEntry);
            return;
        }

        if (dirEntries.add(dirEntry)) {
            dest.add(dirEntry);
        } // else dir entry was already present
    }

    @Nullable
    private static String maybeExtractChildDirNameFromRelativePaths(String parentPath, String childPath) {
        int adjustedParentLen = parentPath.length();
        if (adjustedParentLen == 1) {
            // relative path of the volume root is "/" instead of "", which is inconsistent with all
            // other relative paths (eg "Android/", not "/Android/")

            if (!parentPath.equals("/")) {
                Log.e(TAG, "unexpected parentPath " + parentPath);
                return null;
            }
            adjustedParentLen = 0;
        }

        int childLen = childPath.length();

        int minChildLen = adjustedParentLen + 2; // two extra characters: '/' and a single-letter file name

        if (childLen < minChildLen) {
            if (childLen != parentPath.length()) {
                Log.w(TAG, "inconsistent relative path " + childPath);
            }
            return null;
        }

        int dirNameStart = adjustedParentLen;
        int dirNameEnd = childPath.indexOf('/', dirNameStart);

        if (dirNameEnd <= dirNameStart) {
            Log.w(TAG, "dirNameEnd not found, childPath: " + childPath);
            return null;
        }

        return childPath.substring(dirNameStart, dirNameEnd);
    }

    private static void maybeExtractChildNameFromStorageScope(
            String dirPath, StorageScope scope, ArraySet<String> dirEntries,
            ArrayList<String> dirNames, ArrayList<String> fileNames)
    {
        final String scopePath = scope.path;

        final int dirPathLen = dirPath.length();
        final int minScopePathLen = dirPathLen + 2; // two characters: '/' and a single-letter dir entry name

        if (scopePath.length() < minScopePathLen) {
            return;
        }

        if (!scopePath.startsWith(dirPath)) {
            return;
        }

        if (scopePath.charAt(dirPathLen) != '/') {
            return;
        }

        final int nameStart = dirPathLen + 1;
        int nameEnd = scopePath.indexOf('/', nameStart);

        boolean fullPath = false;
        if (nameEnd < 0) {
            nameEnd = scopePath.length();
            fullPath = true;
        }

        final String name = scopePath.substring(nameStart, nameEnd);

        if (fullPath) {
            File scopeFile = new File(scopePath);

            if (scope.isDirectory()) {
                if (scopeFile.isDirectory()) {
                    maybeAddDirEntry(name, dirEntries, dirNames);
                }
            } else if (scope.isFile()) {
                if (scopeFile.isFile()) {
                    maybeAddDirEntry(name, dirEntries, fileNames);
                }
            }
        } else {
            File child = new File(scopePath.substring(0, nameEnd));
            if (child.isDirectory()) {
                maybeAddDirEntry(name, dirEntries, dirNames);
            }
        }
    }
}
