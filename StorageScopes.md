For an introduction, read https://developer.android.com/training/data-storage#permissions and
https://source.android.com/devices/storage/scoped

The purpose of the Storage Scopes feature is to allow the user to configure which files a given
app has access to.

As a baseline, MediaProvider allows a modern app (targetSdk >= 29, unless legacy external storage
is enabled for that app) to see all files that were created by it. No storage permission is needed
for this type of access.

When the Storage Scopes feature is enabled, modern external storage is enforced regardless of the
targetSdk value, and the app assumes that it has all of storage permissions that were declared in its
AndroidManifest (both runtime and app-op storage permission self-checks are spoofed for that app).
Enabling the Storage Scopes feature is allowed only when the app isn't granted any storage permission.

If the app expects to be granted the "All files access" permission (it has an explicit
"All files access" permission declaration or would normally use legacy external storage and has
declared the WRITE_EXTERNAL_STORAGE permission), then the write restrictions that are normally
enforced for apps that don't have a storage permission are slightly relaxed:
- app is allowed to create directories in any external storage directory (except Android/data
and Android/obb). Normally, directory creation is allowed only inside the standard directories
(Documents, Music, Pictures etc)
- app is allowed to create files in any external storage directory (again, except Android/data and
Android/obb). Normally, file creation is allowed only inside the standard directories and the file type
is enforced (eg lyrics.txt file is not allowed to be created inside the Music directory)
- app is allowed to remove empty top-level directories. Normally, removal is allowed only for
non-top-level empty directories
- app is allowed to rename files / directories in (and into) the root of external storage, but
only if all affected files are writable by the app

Optionally, the user can specify additional files and directories that the app can access (ie those
that weren't created by the app itself). The standard SAF picker is used to select them, and the list
of picked locations (scopes) is persisted via GrapheneOS-specific PackageManagerService extension
(GosPackageState).

Read-write access to user-picked scopes is granted only if the app declares the need for write
storage access in its AndroidManifest, otherwise read-only access is granted.

All of the above specified changes apply to both traditional file access APIs (POSIX, java.io.File etc)
and MediaProvider APIs (query(), insert(), update(), delete() etc)

# Changes to visibility of directory names
Upstream MediaProvider allows apps that don't have any storage permission to read the list of
all external storage directories via readdir(), java.io.File#list() etc.

This metadata leak is addressed by returning only those directories for which at least one of the
following conditions is true:
- it has files that the app created
- it has files / directories that are in the list of user-picked scopes
- it's inside a user-picked directory

Note that unlike all other changes described above, this change applies to all apps that don't have
a storage permission, regardless of whether the Storage Scopes feature is enabled.
This behavior of the upstream MediaProvider is undocumented, only a small number of apps (if any)
will be negatively affected by this change.

# Limitations
- If the app is reinstalled, it will lose access to all files that it created previously. This doesn't
affect user-picked directories / files.
- Access to files that the app created inside external storage, but outside its private Android/data
directory and outside user-picked directories is significantly slower than accessing files inside those
directories. This affects apps that use shared external storage instead of their private Android/data
or internal /data/data directories to store large number of files that they access frequently.
As a workaround, if these files are inside an app-specific directory (they almost always are),
this directory can be added to the list of user-picked directories.
Note that this performance difference applies only to operations that involve paths (open(), stat(),
getdents() etc). Operations on file descriptors (read(), write(), fstat(), lseek() etc) aren't affected.
- A directory that is created outside the user-picked directories isn't visible in the directory
listing until at least one file is created inside of it. This kind of directory is still visible
via stat() (which is used by java.io.File#{exists(), isDirectory()} etc).
