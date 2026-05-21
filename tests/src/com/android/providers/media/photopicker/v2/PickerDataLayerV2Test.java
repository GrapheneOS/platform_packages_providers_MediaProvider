/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.providers.media.photopicker.v2;

import static android.provider.MediaStore.MY_USER_ID;
import static android.provider.MediaStore.PER_USER_RANGE;

import static com.android.providers.media.photopicker.util.PickerDbTestUtils.ALBUM_ID;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.CLOUD_ID;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.CLOUD_ID_1;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.CLOUD_ID_2;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.CLOUD_ID_3;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.CLOUD_ID_4;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.CLOUD_PROVIDER;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.DATE_TAKEN_MS;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.DATE_TAKEN_MS_1;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.DATE_TAKEN_MS_2;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.DATE_TAKEN_MS_3;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.DURATION_MS;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.GENERATION_MODIFIED;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.GIF_IMAGE_MIME_TYPE;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.HEIGHT;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.JPEG_IMAGE_MIME_TYPE;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.LOCAL_ID;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.LOCAL_ID_1;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.LOCAL_ID_2;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.LOCAL_ID_3;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.LOCAL_ID_4;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.LOCAL_PROVIDER;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.MP4_VIDEO_MIME_TYPE;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.ORIENTATION;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.PACKAGE_NAME1;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.PNG_IMAGE_MIME_TYPE;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.RES_ID1;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.STANDARD_MIME_TYPE_EXTENSION;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.TEST_DIFFERENT_PACKAGE_NAME;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.TEST_PACKAGE_NAME;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.WIDTH;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.assertAddAlbumMediaOperation;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.assertAddMediaOperation;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.assertInsertGrantsOperation;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.getAlbumCursor;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.getAlbumMediaCursor;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.getCloudMediaCursor;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.getLocalMediaCursor;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.getMediaCategoriesCursor;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.getMediaCursor;
import static com.android.providers.media.photopicker.util.PickerDbTestUtils.getMediaGrantsCursor;
import static com.android.providers.media.photopicker.v2.PickerDataLayerV2.COLUMN_GRANTS_COUNT;
import static com.android.providers.media.photopicker.v2.PickerDataLayerV2.PREFS_KEY_SEARCH_STATE_ENABLED;
import static com.android.providers.media.photopicker.v2.model.AlbumsCursorWrapper.EMPTY_MEDIA_ID;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.Manifest;
import android.compat.testing.PlatformCompatChangeRule;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Process;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.CloudMediaProviderContract;
import android.provider.MediaStore;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.util.Pair;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;

import com.android.providers.media.MediaProvider;
import com.android.providers.media.PickerUriResolver;
import com.android.providers.media.TestConfigStore;
import com.android.providers.media.cloudproviders.CloudProviderPrimary;
import com.android.providers.media.cloudproviders.SearchProvider;
import com.android.providers.media.flags.Flags;
import com.android.providers.media.photopicker.CategoriesState;
import com.android.providers.media.photopicker.PickerSyncController;
import com.android.providers.media.photopicker.SearchState;
import com.android.providers.media.photopicker.data.ItemsProvider;
import com.android.providers.media.photopicker.data.PickerDatabaseHelper;
import com.android.providers.media.photopicker.data.PickerDbFacade;
import com.android.providers.media.photopicker.data.model.UserId;
import com.android.providers.media.photopicker.sync.PickerSyncLockManager;
import com.android.providers.media.photopicker.util.PickerDbTestUtils;
import com.android.providers.media.photopicker.util.exceptions.RequestObsoleteException;
import com.android.providers.media.photopicker.v2.model.MediaGroup;
import com.android.providers.media.photopicker.v2.model.MediaInMediaSetSyncRequestParams;
import com.android.providers.media.photopicker.v2.model.MediaSetsSyncRequestParams;
import com.android.providers.media.photopicker.v2.model.MediaSource;
import com.android.providers.media.photopicker.v2.model.SearchSuggestion;
import com.android.providers.media.photopicker.v2.model.SearchSuggestionRequest;
import com.android.providers.media.photopicker.v2.model.SearchTextRequest;
import com.android.providers.media.photopicker.v2.sqlite.MediaInMediaSetsDatabaseUtil;
import com.android.providers.media.photopicker.v2.sqlite.MediaSetsDatabaseUtil;
import com.android.providers.media.photopicker.v2.sqlite.PickerSQLConstants;
import com.android.providers.media.photopicker.v2.sqlite.SearchResultsDatabaseUtil;
import com.android.providers.media.photopicker.v2.sqlite.SearchSuggestionsDatabaseUtils;
import com.android.providers.media.photopicker.v2.sqlite.SearchSuggestionsQuery;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import libcore.junit.util.compat.CoreCompatChangeRule.EnableCompatChanges;

import kotlin.Triple;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class PickerDataLayerV2Test {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private PickerSyncController mMockSyncController;
    @Mock
    private Context mMockContext;
    @Mock
    private PackageManager mMockPackageManager;
    @Mock
    private SearchState mSearchState;
    @Mock
    private WorkManager mMockWorkManager;
    @Mock
    private Operation mMockOperation;
    @Mock
    private WorkContinuation mMockWorkContinuation;
    @Mock
    private ListenableFuture<Operation.State.SUCCESS> mMockFuture;
    @Mock
    CategoriesState mCategoriesState;
    @Mock
    private SharedPreferences mUserPrefs;
    @Mock
    private SharedPreferences.Editor mEditor;
    private PickerDbFacade mFacade;
    private Context mContext;
    private MockContentResolver mMockContentResolver;
    private TestContentProvider mLocalProvider;
    private TestContentProvider mCloudProvider;
    private TestConfigStore mTestConfigStore;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule
    public final TestRule mCompatChangeRule = new PlatformCompatChangeRule();


    private static class TestContentProvider extends MockContentProvider {
        private Cursor mQueryResult = null;

        TestContentProvider() {
            super();
        }

        @Override
        public Cursor query(Uri uri,
                String[] projection,
                Bundle queryArgs,
                CancellationSignal cancellationSignal) {
            return mQueryResult;
        }

        public void setQueryResult(Cursor queryResult) {
            this.mQueryResult = queryResult;
        }
    }

    @Before
    public void setUp() {
        initMocks(this);
        PickerSyncController.setInstance(mMockSyncController);
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File dbPath = mContext.getDatabasePath(PickerDatabaseHelper.PICKER_DATABASE_NAME);
        dbPath.delete();
        mFacade = new PickerDbFacade(mContext, new PickerSyncLockManager(), LOCAL_PROVIDER);
        mFacade.setCloudProvider(CLOUD_PROVIDER);
        mLocalProvider = new TestContentProvider();
        mCloudProvider = new TestContentProvider();
        mMockContentResolver = new MockContentResolver();
        mMockContentResolver.addProvider(LOCAL_PROVIDER, mLocalProvider);
        mMockContentResolver.addProvider(CLOUD_PROVIDER, mCloudProvider);

        doReturn(LOCAL_PROVIDER).when(mMockSyncController).getLocalProvider();
        doReturn(CLOUD_PROVIDER).when(mMockSyncController).getCloudProvider();
        doReturn(CLOUD_PROVIDER).when(mMockSyncController).getCloudProviderOrDefault(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaSets(any(), any());
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaSets(any());
        doReturn(mFacade).when(mMockSyncController).getDbFacade();
        doReturn(mSearchState).when(mMockSyncController).getSearchState();
        doReturn(mCategoriesState).when(mMockSyncController).getCategoriesState();
        doReturn(new PickerSyncLockManager()).when(mMockSyncController).getPickerSyncLockManager();
        doReturn(mMockContentResolver).when(mMockContext).getContentResolver();
        doReturn(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getResources())
                .when(mMockContext).getResources();

        doReturn(Process.myUserHandle()).when(mMockContext).getUser();

        mTestConfigStore = new TestConfigStore();

        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity(
                        Manifest.permission.LOG_COMPAT_CHANGE,
                        Manifest.permission.READ_COMPAT_CHANGE_CONFIG,
                        Manifest.permission.READ_DEVICE_CONFIG);
    }

    @After
    public void tearDown() {
        if (mFacade != null) {
            mFacade.setCloudProvider(null);
        }
    }

    @Test
    public void testAvailableProvidersNoCloudProvider() {
        doReturn(/* cloudProviderAuthority */ null)
                .when(mMockSyncController).getCloudProviderOrDefault(any());

        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.packageName = LOCAL_PROVIDER;
        providerInfo.name = "LOCAL_PROVIDER";
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.nonLocalizedLabel = providerInfo.name;
        providerInfo.applicationInfo = applicationInfo;
        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        doReturn(providerInfo)
                .when(mMockPackageManager)
                .resolveContentProvider(any(), anyInt());

        try (Cursor availableProviders = PickerDataLayerV2.queryAvailableProviders(mMockContext)) {
            availableProviders.moveToFirst();

            assertEquals(
                    "Only local provider should be available when cloud provider is null",
                    /* expected */ 1,
                    availableProviders.getCount()
            );

            assertEquals(
                    "Available provider should serve local media",
                    /* expected */ MediaSource.LOCAL,
                    MediaSource.valueOf(availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .MEDIA_SOURCE.getColumnName())))
            );

            assertEquals(
                    "Local provider authority is not correct",
                    /* expected */ LOCAL_PROVIDER,
                    availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .AUTHORITY.getColumnName()))
            );

            assertEquals(
                    "Local provider UID is not correct",
                    /* expected */ Process.myUid(),
                    availableProviders.getInt(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .UID.getColumnName()))
            );

            assertEquals(
                    "Local provider's label is not correct",
                    /* expected */ "LOCAL_PROVIDER",
                    availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .DISPLAY_NAME.getColumnName()))
            );
        }
    }

    @Test
    public void testAvailableProvidersWithCloudProvider() throws
            PackageManager.NameNotFoundException {
        final int cloudUID = Integer.MAX_VALUE;
        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.packageName = CLOUD_PROVIDER;
        providerInfo.name = "PROVIDER";
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.nonLocalizedLabel = providerInfo.name;
        providerInfo.applicationInfo = applicationInfo;

        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        doReturn(cloudUID)
                .when(mMockPackageManager)
                .getPackageUid(any(), anyInt());
        doReturn(providerInfo)
                .when(mMockPackageManager)
                .resolveContentProvider(any(), anyInt());

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor availableProviders = PickerDataLayerV2.queryAvailableProviders(mMockContext)) {
            availableProviders.moveToFirst();

            assertEquals(
                    "Both local and cloud providers should be available",
                    /* expected */ 2,
                    availableProviders.getCount()
            );

            assertEquals(
                    "Available provider should serve local media",
                    /* expected */ MediaSource.LOCAL,
                    MediaSource.valueOf(availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .MEDIA_SOURCE.getColumnName())))
            );

            assertEquals(
                    "Local provider authority is not correct",
                    /* expected */ LOCAL_PROVIDER,
                    availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .AUTHORITY.getColumnName()))
            );

            assertEquals(
                    "Local provider UID is not correct",
                    /* expected */ Process.myUid(),
                    availableProviders.getInt(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .UID.getColumnName()))
            );

            assertEquals(
                    "Local provider's label is not correct",
                    /* expected */ "PROVIDER",
                    availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .DISPLAY_NAME.getColumnName()))
            );

            availableProviders.moveToNext();

            assertEquals(
                    "Available provider should serve remote media",
                    /* expected */ MediaSource.REMOTE,
                    MediaSource.valueOf(availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .MEDIA_SOURCE.getColumnName())))
            );

            assertEquals(
                    "Cloud provider authority is not correct",
                    /* expected */ CLOUD_PROVIDER,
                    availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .AUTHORITY.getColumnName()))
            );

            assertEquals(
                    "Cloud provider UID is not correct",
                    /* expected */ cloudUID,
                    availableProviders.getInt(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .UID.getColumnName()))
            );

            assertEquals(
                    "Cloud provider's label is not correct",
                    /* expected */ "PROVIDER",
                    availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .DISPLAY_NAME.getColumnName()))
            );
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_CMP_IMPROVEMENTS)
    public void testAvailableProvidersWithDisabledCloudProvider() throws
            PackageManager.NameNotFoundException {
        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.packageName = LOCAL_PROVIDER;
        providerInfo.name = "LOCAL_PROVIDER";
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.nonLocalizedLabel = providerInfo.name;
        providerInfo.applicationInfo = applicationInfo;


        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();

        doReturn(providerInfo)
                .when(mMockPackageManager)
                .resolveContentProvider(eq(LOCAL_PROVIDER), anyInt());

        doReturn(null)
                .when(mMockPackageManager)
                .resolveContentProvider(eq(CLOUD_PROVIDER), anyInt());

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor availableProviders = PickerDataLayerV2.queryAvailableProviders(mMockContext)) {
            availableProviders.moveToFirst();

            assertEquals(
                    "Only local provider should be available.",
                    /* expected */ 1,
                    availableProviders.getCount()
            );

            assertEquals(
                    "Available provider should serve local media",
                    /* expected */ MediaSource.LOCAL,
                    MediaSource.valueOf(availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .MEDIA_SOURCE.getColumnName())))
            );

            assertEquals(
                    "Local provider authority is not correct",
                    /* expected */ LOCAL_PROVIDER,
                    availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .AUTHORITY.getColumnName()))
            );

            assertEquals(
                    "Local provider UID is not correct",
                    /* expected */ Process.myUid(),
                    availableProviders.getInt(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .UID.getColumnName()))
            );

            assertEquals(
                    "Local provider's label is not correct",
                    /* expected */ "LOCAL_PROVIDER",
                    availableProviders.getString(
                            availableProviders.getColumnIndexOrThrow(
                                    PickerSQLConstants.AvailableProviderResponse
                                            .DISPLAY_NAME.getColumnName()))
            );
        }
        // Verify that the cloud authority was reset to null
        verify(mMockSyncController, times(1)).setCloudProvider(null);
    }

    @Test
    public void testGetSearchProvidersReturnsProviderAndCachesSearchState() {

        when(mMockSyncController.getSearchState()).thenReturn(mSearchState);
        when(mMockSyncController.getCloudProviderOrDefault(null))
                .thenReturn(CLOUD_PROVIDER);
        when(mUserPrefs.edit()).thenReturn(mEditor);
        // The cloud provider is capable of search
        doReturn(true).when(mSearchState).isCloudSearchEnabled(any());

        Bundle result = PickerDataLayerV2.getSearchProviders(
                mContext,
                MoreExecutors.directExecutor(),
                MoreExecutors.directExecutor()
        );

        assertTrue(result.getStringArrayList(PickerSQLConstants.EXTRA_SEARCH_PROVIDER_AUTHORITIES)
                .contains(CLOUD_PROVIDER));

        // The 'true' result was cached
        verify(mMockSyncController).cacheCloudSearchCapability(true);
    }

    @Test
    public void testGetSearchProvidersReadsFromCacheForSearchState() {
        // Pre-populate the cache with 'true'
        when(mMockSyncController.readLastKnownSearchCapability()).thenReturn(true);
        when(mUserPrefs.getBoolean(PREFS_KEY_SEARCH_STATE_ENABLED, false))
                .thenReturn(true);
        when(mSearchState.doesPickerSupportSearch(any(), anyString()))
                .thenReturn(true);

        // Since we can't easily timeout CompletableFuture.get(), the key is to
        // verify the fallback logic. We can assume the timeout will trigger the catch block.
        boolean searchCapability = PickerDataLayerV2.readLastKnownSearchCapability(
                mMockSyncController);

        assertTrue(searchCapability);
    }

    @Test
    public void testQueryMediaWithInvalidProviders() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                        new ArrayList<>(Arrays.asList("invalid.provider"))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(0);
        }
    }

    @Test
    public void testQueryMediaWithCloudQueryDisabled() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testItemsPerMonthQueryWithInvalidProviders() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS_1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS_3, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS_2, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);


        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryItemsPerMonth(
                mMockContext, getItemsPerMonthQueryExtras(
                        new ArrayList<>(Arrays.asList("invalid.provider"))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(0);
        }
    }

    @Test
    public void testItemsPerMonthQueryWithCloudQueryDisabled() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS_1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS_3, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS_2, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);


        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        List<Long> dateTakenMsList = Arrays.asList(DATE_TAKEN_MS_1, DATE_TAKEN_MS_3);
        //Triple<Integer, Integer, Integer> represents  Triple<Year , Month, ItemsCount>
        List<Triple<Integer, Integer, Integer>> expectedItemsPerMonthList =
                getExpectedItemsPerMonth(dateTakenMsList);

        try (Cursor cr = PickerDataLayerV2.queryItemsPerMonth(
                mMockContext, getItemsPerMonthQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in items per month query result")
                    .that(cr.getCount()).isEqualTo(expectedItemsPerMonthList.size());

            int currIndexInExpectedList = 0;
            cr.moveToFirst();
            while (currIndexInExpectedList < expectedItemsPerMonthList.size()) {
                Triple<Integer, Integer, Integer> currItemsPerMonth =
                        expectedItemsPerMonthList.get(currIndexInExpectedList);
                assertItemsPerMonthCursor(cr, currItemsPerMonth.getFirst(),
                        currItemsPerMonth.getSecond(), currItemsPerMonth.getThird());
                cr.moveToNext();
                currIndexInExpectedList++;
            }
        }
    }

    @Test
    public void testItemsPerMonthQueryWithCloudQueryEnabled() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS_1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS_3, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS_2, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);


        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);


        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        List<Long> dateTakenMsList = Arrays.asList(
                DATE_TAKEN_MS_1, DATE_TAKEN_MS_2, DATE_TAKEN_MS_3);
        //Triple<Integer, Integer, Integer> represents  Triple<Year , Month, ItemsCount>
        List<Triple<Integer, Integer, Integer>> expectedItemsPerMonthList =
                getExpectedItemsPerMonth(dateTakenMsList);

        try (Cursor cr = PickerDataLayerV2.queryItemsPerMonth(
                mMockContext, getItemsPerMonthQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in items per month query result")
                    .that(cr.getCount()).isEqualTo(expectedItemsPerMonthList.size());

            int currIndexInExpectedList = 0;
            cr.moveToFirst();
            while (currIndexInExpectedList < expectedItemsPerMonthList.size()) {
                Triple<Integer, Integer, Integer> currItemsPerMonth =
                        expectedItemsPerMonthList.get(currIndexInExpectedList);
                assertItemsPerMonthCursor(cr, currItemsPerMonth.getFirst(),
                        currItemsPerMonth.getSecond(), currItemsPerMonth.getThird());
                cr.moveToNext();
                currIndexInExpectedList++;
            }
        }
    }

    @Test
    public void testItemsPerMonthQueryWithLocalAndCloudDedupe() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS - 1);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        List<Long> dateTakenMsList = Arrays.asList(DATE_TAKEN_MS + 1, DATE_TAKEN_MS);
        //Triple<Integer, Integer, Integer> represents  Triple<Year , Month, ItemsCount>
        List<Triple<Integer, Integer, Integer>> expectedItemsPerMonthList =
                getExpectedItemsPerMonth(dateTakenMsList);

        try (Cursor cr = PickerDataLayerV2.queryItemsPerMonth(
                mMockContext, getItemsPerMonthQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in items per month query result")
                    .that(cr.getCount()).isEqualTo(expectedItemsPerMonthList.size());

            int currIndexInExpectedList = 0;
            cr.moveToFirst();
            while (currIndexInExpectedList < expectedItemsPerMonthList.size()) {
                Triple<Integer, Integer, Integer> currItemsPerMonth =
                        expectedItemsPerMonthList.get(currIndexInExpectedList);
                assertItemsPerMonthCursor(cr, currItemsPerMonth.getFirst(),
                        currItemsPerMonth.getSecond(), currItemsPerMonth.getThird());
                cr.moveToNext();
                currIndexInExpectedList++;
            }
        }
    }

    @Test
    public void testItemsPerMonthQueryWithAllVideoMimeTypeFilter() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS_1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS_2, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS_3, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        // Only video media's date taken is considered.
        List<Long> dateTakenMsList = Arrays.asList(DATE_TAKEN_MS_1);
        //Triple<Integer, Integer, Integer> represents  Triple<Year , Month, ItemsCount>
        List<Triple<Integer, Integer, Integer>> expectedItemsPerMonthList =
                getExpectedItemsPerMonth(dateTakenMsList);

        try (Cursor cr = PickerDataLayerV2.queryItemsPerMonth(
                mMockContext, getItemsPerMonthQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        new ArrayList<>(Arrays.asList("video/*"))))) {
            assertWithMessage(
                    "Unexpected number of rows in items per month query result")
                    .that(cr.getCount()).isEqualTo(expectedItemsPerMonthList.size());

            int currIndexInExpectedList = 0;
            cr.moveToFirst();
            while (currIndexInExpectedList < expectedItemsPerMonthList.size()) {
                Triple<Integer, Integer, Integer> currItemsPerMonth =
                        expectedItemsPerMonthList.get(currIndexInExpectedList);
                assertItemsPerMonthCursor(cr, currItemsPerMonth.getFirst(),
                        currItemsPerMonth.getSecond(), currItemsPerMonth.getThird());
                cr.moveToNext();
                currIndexInExpectedList++;
            }
        }
    }

    @Test
    public void testItemsPerMonthQueryWithAllImageMimeTypeFilter() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS_1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS_2, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS_3, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        // Only image media's date taken are considered.
        List<Long> dateTakenMsList = Arrays.asList(DATE_TAKEN_MS, DATE_TAKEN_MS_2, DATE_TAKEN_MS_3);
        //Triple<Integer, Integer, Integer> represents  Triple<Year , Month, ItemsCount>
        List<Triple<Integer, Integer, Integer>> expectedItemsPerMonthList =
                getExpectedItemsPerMonth(dateTakenMsList);

        try (Cursor cr = PickerDataLayerV2.queryItemsPerMonth(
                mMockContext, getItemsPerMonthQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        new ArrayList<>(Arrays.asList("image/*"))))) {
            assertWithMessage(
                    "Unexpected number of rows in items per month query result")
                    .that(cr.getCount()).isEqualTo(expectedItemsPerMonthList.size());

            int currIndexInExpectedList = 0;
            cr.moveToFirst();
            while (currIndexInExpectedList < expectedItemsPerMonthList.size()) {
                Triple<Integer, Integer, Integer> currItemsPerMonth =
                        expectedItemsPerMonthList.get(currIndexInExpectedList);
                assertItemsPerMonthCursor(cr, currItemsPerMonth.getFirst(),
                        currItemsPerMonth.getSecond(), currItemsPerMonth.getThird());
                cr.moveToNext();
                currIndexInExpectedList++;
            }
        }
    }

    /**
     * Calculates the count of items per year and month from a list of dateTakenMs values,
     * considering the device's local time zone, and returns the results in descending
     * order of year and month.
     *
     * @param dateTakenMsList A list of timestamps (milliseconds since epoch).
     * @return A list of {@literal Triple<Integer, Integer, Integer>} representing the year, month,
     * and count of items for each year-month combination, sorted in descending
     * order of year and then month.
     */
    public static List<Triple<Integer, Integer, Integer>> getExpectedItemsPerMonth(
            List<Long> dateTakenMsList) {
        Map<Pair<Integer, Integer>, Integer> yearMonthCounts = new HashMap<>();

        for (long dateTakenMs : dateTakenMsList) {
            Pair<Integer, Integer> yearMonth = getYearAndMonth(dateTakenMs);
            yearMonthCounts.put(yearMonth, yearMonthCounts.getOrDefault(yearMonth, 0) + 1);
        }
        List<Triple<Integer, Integer, Integer>> result = new ArrayList<>();
        for (Map.Entry<Pair<Integer, Integer>, Integer> entry : yearMonthCounts.entrySet()) {
            result.add(new Triple<>(entry.getKey().first, entry.getKey().second, entry.getValue()));
        }

        result.sort((a, b) -> {
            int yearComparison = Integer.compare(b.getFirst(), a.getFirst()); // Descending year
            if (yearComparison != 0) return yearComparison;
            return Integer.compare(b.getSecond(), a.getSecond()); // Descending month
        });
        return result;
    }

    /**
     * Extracts the year and month from a given timestamp (milliseconds since epoch),
     * considering the device's local time zone.
     *
     * @param dateTaken The timestamp in milliseconds since the Unix epoch.
     * @return A Pair containing the year (as an Integer) and the month (as an Integer),
     * or null if an error occurs during date/time conversion.
     */
    private static Pair<Integer, Integer> getYearAndMonth(long dateTaken) {
        long dateTakenSeconds = dateTaken / 1000;

        // Get the device's local time zone offset at the given timestamp.
        ZoneOffset localOffset = ZoneOffset.systemDefault().getRules().getOffset(
                Instant.ofEpochSecond(dateTakenSeconds));

        LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(
                dateTakenSeconds,
                0,
                localOffset
        );
        int year = localDateTime.getYear();
        int month = localDateTime.getMonthValue();
        return new Pair<>(year, month);
    }

    @Test
    public void testMediaPageKeyQueryWithInvalidProviders() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);


        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKey(
                mMockContext, getMediaPageKeyQueryExtras(
                        new ArrayList<>(Arrays.asList("invalid.provider")), 1))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key query result")
                    .that(cr.getCount()).isEqualTo(0);
        }
    }

    @Test
    public void testMediaPageKeyQueryWithCloudQueryDisabled() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS + 2, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS + 3, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); // picker_id = 1
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1); // picker_id = 2
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1); // picker_id = 3


        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKey(
                mMockContext, getMediaPageKeyQueryExtras(
                        // Item position is based on zero indexed
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 1))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key query result")
                    .that(cr.getCount()).isEqualTo(1);
            cr.moveToFirst();

            // Item position is based on zero indexed , so for item position = 1 means we want 2nd
            // item in the media table
            // Items order in media table LOCAL_ID_2, LOCAL_ID_1
            assertMediaPageKeyCursor(cr, 1L, DATE_TAKEN_MS + 1);
        }
    }

    @Test
    public void testMediaPageKeyQueryWithCloudQueryEnabled() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS - 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); // picker_id = 1
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1); // picker_id = 2
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1); // picker_id = 3

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKey(
                mMockContext, getMediaPageKeyQueryExtras(
                        // Item position is based on zero indexed
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 1))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key query result")
                    .that(cr.getCount()).isEqualTo(1);
            cr.moveToFirst();

            // Item position is based on zero indexed , so for item position = 1 means we want 2nd
            // item in the media table
            // Items order in data tables are:-
            // LOCAL_ID_1 (latest item added), CLOUD_ID_1, LOCAL_ID_2,
            assertMediaPageKeyCursor(cr, 2L, DATE_TAKEN_MS);
        }
    }

    @Test
    public void testMediaPageKeyQuery_ItemPositionIsGreaterThanNumberOfItems() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); // picker_id = 1
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1); // picker_id = 2
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1); // picker_id = 3

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKey(
                mMockContext, getMediaPageKeyQueryExtras(
                        // Item position is based on zero indexed , so for item position = 3 means
                        // we want 4th item in the media table but only 3 items are available
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 3))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key query result")
                    .that(cr.getCount()).isEqualTo(0);
        }
    }

    @Test
    public void testMediaPageKeyQueryDedupe() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS - 1);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1); // picker_id = 1
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1); // picker_id = 2
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1); // picker_id = 3

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKey(
                mMockContext, getMediaPageKeyQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 1))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            // Item position is based on zero indexed , so for item position = 1 means we want 2nd
            // item in the media table
            // Items in data tables are:-  LOCAL_ID_1 (latest item added) , CLOUD_ID_2
            assertMediaPageKeyCursor(cr, 3L, DATE_TAKEN_MS);
        }
    }

    @Test
    public void testMediaPageKeyQueryAllVideoMimeTypeFilter() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); //picker_id = 1
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1); //picker_id = 2
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1); //picker_id = 3
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1); //picker_id = 4

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKey(
                mMockContext, getMediaPageKeyQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 0,
                        new ArrayList<>(Arrays.asList("video/*"))))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            // Item position is based on zero indexed , so for item position = 0 means we want 1st
            // item in the media table
            // Items order in media tables are:-  LOCAL_ID_4 (latest item added) , LOCAL_ID_2
            assertMediaPageKeyCursor(cr, 4L, DATE_TAKEN_MS + 1);
        }
    }

    @Test
    public void testMediaPageKeyQueryAllImageMimeTypeFilter() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS + 2, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); //picker_id = 1
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1); //picker_id = 2
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1); //picker_id = 3
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1); //picker_id = 4

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKey(
                mMockContext, getMediaPageKeyQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 2,
                        new ArrayList<>(Arrays.asList("image/*"))))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            // Item position is based on zero indexed , so for item position = 2 means we want 3rd
            // item in the media table
            // Items order in data tables are:-
            // LOCAL_ID_4 (latest item added), LOCAL_ID_1, LOCAL_ID_3
            assertMediaPageKeyCursor(cr, 3L, DATE_TAKEN_MS);
        }
    }

    @Test
    public void testMediaPageKeyListQueryWithInvalidProviders() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);


        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKeyList(
                mMockContext, getMediaPageKeyListQueryExtras(
                        new ArrayList<>(Arrays.asList("invalid.provider")), 2))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key list query result")
                    .that(cr.getCount()).isEqualTo(0);
        }
    }

    @Test
    public void testMediaPageKeyListQueryWithCloudQueryDisabled() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS + 2, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS + 3, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); // picker_id = 1
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1); // picker_id = 2
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1); // picker_id = 3


        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKeyList(
                mMockContext, getMediaPageKeyListQueryExtras(
                        // Item position is based on zero indexed
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 2))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key list query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            assertMediaPageKeyListCursor(cr, 3L, DATE_TAKEN_MS + 3);
        }
    }

    @Test
    public void testMediaPageKeyListQueryWithCloudQueryEnabled() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS - 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); // picker_id = 1
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1); // picker_id = 2
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1); // picker_id = 3

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKeyList(
                mMockContext, getMediaPageKeyListQueryExtras(
                        // Item position is based on zero indexed
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 2))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key list query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaPageKeyListCursor(cr, 1L, DATE_TAKEN_MS + 1);

            cr.moveToNext();
            assertMediaPageKeyListCursor(cr, 3L, DATE_TAKEN_MS - 1);
        }
    }

    @Test
    public void testMediaPageKeyListQueryWithLargeInterval() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS - 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); // picker_id = 1
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1); // picker_id = 2
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1); // picker_id = 3

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKeyList(
                mMockContext, getMediaPageKeyListQueryExtras(
                        // Item position is based on zero indexed
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 5))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key list query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            assertMediaPageKeyListCursor(cr, 1L, DATE_TAKEN_MS + 1);
        }
    }

    @Test
    public void testMediaPageKeyListQueryDedupe() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS - 1);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1); // picker_id = 1
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1); // picker_id = 2
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1); // picker_id = 3

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKeyList(
                mMockContext, getMediaPageKeyListQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 1))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key list query result")
                    .that(cr.getCount()).isEqualTo(2);

            // Items in data tables are:-  LOCAL_ID_1 (latest item added) , CLOUD_ID_2

            cr.moveToFirst();
            assertMediaPageKeyListCursor(cr, 2L, DATE_TAKEN_MS + 1);

            cr.moveToNext();
            assertMediaPageKeyListCursor(cr, 3L, DATE_TAKEN_MS);
        }
    }

    @Test
    public void testMediaPageKeyListQueryAllVideoMimeTypeFilter() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); //picker_id = 1
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1); //picker_id = 2
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1); //picker_id = 3
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1); //picker_id = 4

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKeyList(
                mMockContext, getMediaPageKeyListQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 2,
                        new ArrayList<>(Arrays.asList("video/*"))))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key list query result")
                    .that(cr.getCount()).isEqualTo(1);

            // Items order in media tables are:-  LOCAL_ID_4 (latest item added) , LOCAL_ID_2
            cr.moveToFirst();
            assertMediaPageKeyListCursor(cr, 4L, DATE_TAKEN_MS + 1);
        }
    }

    @Test
    public void testMediaPageKeyListQueryAllImageMimeTypeFilter() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS + 2, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1); //picker_id = 1
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1); //picker_id = 2
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1); //picker_id = 3
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1); //picker_id = 4

        try (Cursor cr = PickerDataLayerV2.queryMediaPageKeyList(
                mMockContext, getMediaPageKeyListQueryExtras(
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)), 2,
                        new ArrayList<>(Arrays.asList("image/*"))))) {
            assertWithMessage(
                    "Unexpected number of rows in media page key list query result")
                    .that(cr.getCount()).isEqualTo(2);

            // Items order in data tables are:-
            // LOCAL_ID_4 (latest item added), LOCAL_ID_1, LOCAL_ID_3

            cr.moveToFirst();
            assertMediaPageKeyListCursor(cr, 4L, DATE_TAKEN_MS + 2);

            cr.moveToNext();
            assertMediaPageKeyListCursor(cr, 3L, DATE_TAKEN_MS);
        }
    }

    @Test
    public void testQueryLocalMediaSortOrder() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 3,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            // LOCAL_ID_1 has the most recent date taken.
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            // LOCAL_ID_2 and LOCAL_ID_3 have the same date taken but the Picker ID of LOCAL_ID_3
            // should be greater.
            assertMediaCursor(cr, LOCAL_ID_3, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testQueryLocalMediaWithGrants() {
        Cursor cursorForMediaWithoutGrants = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorForMediaWithGrants = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS,
                GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorForMediaWithoutGrants,
                /* writeCount */1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorForMediaWithGrants,
                /* writeCount */1);

        int testUid = setupAndGetTestUid();

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 3,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        new ArrayList<>(Arrays.asList("video/*")),
                        MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                        testUid))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            // verify item with isPreGranted as false.
            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE, MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                    /* isPreGranted */ false);

            // verify item with isPreGranted as true.
            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE,
                    MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                    /* isPreGranted */ true);
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_REVOKE_ACCESS_OWNED_PHOTOS)
    @EnableCompatChanges({MediaProvider.ENABLE_OWNED_PHOTOS})
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    public void testPreGrantsForOwnedPhotos() {
        assumeTrue(MediaProvider.isOwnedPhotosEnabled(Process.myUid()));

        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        String[] packageNames = new String[]{TEST_PACKAGE_NAME};
        doReturn(packageNames).when(mMockPackageManager).getPackagesForUid(Process.myUid());
        Map<String, Integer> idVsExpectedPreGrantedValue = populateMediaAndMediaGrantsTable();
        int totalCount = idVsExpectedPreGrantedValue.size();

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(List.of(LOCAL_PROVIDER)),
                        new ArrayList<>(List.of("image/*")),
                        MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                        /*callingUid*/ Process.myUid()))) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(totalCount);

            cr.moveToFirst();
            for (int i = 0; i < totalCount; i++) {
                int id = cr.getInt(cr.getColumnIndexOrThrow("id"));
                int isPreGranted = cr.getInt(cr.getColumnIndexOrThrow("is_pre_granted"));
                assertEquals(idVsExpectedPreGrantedValue.get(String.valueOf(id)).intValue(),
                        isPreGranted);
                cr.moveToNext();
            }
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_REVOKE_ACCESS_OWNED_PHOTOS)
    @EnableCompatChanges({MediaProvider.ENABLE_OWNED_PHOTOS})
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    public void testPreGrantedCountForOwnedPhotos() {
        assumeTrue(MediaProvider.isOwnedPhotosEnabled(Process.myUid()));

        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        String[] packageNames = new String[]{TEST_PACKAGE_NAME};
        doReturn(packageNames).when(mMockPackageManager).getPackagesForUid(Process.myUid());
        Map<String, Integer> idVsPreGranted = populateMediaAndMediaGrantsTable();
        int totalPreGranted = (int) idVsPreGranted.values().stream()
                .filter(preGranted -> Integer.valueOf(1).equals(preGranted))
                .count();

        Bundle queryArgs = new Bundle();
        queryArgs.putInt(Intent.EXTRA_UID, Process.myUid());
        try (Cursor cr = PickerDataLayerV2.fetchCountForPreGrantedItems(mMockContext, queryArgs)) {
            cr.moveToFirst();
            assertEquals(totalPreGranted, cr.getInt(cr.getColumnIndexOrThrow(COLUMN_GRANTS_COUNT)));
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_REVOKE_ACCESS_OWNED_PHOTOS)
    @EnableCompatChanges({MediaProvider.ENABLE_OWNED_PHOTOS})
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    public void testPreviewForOwnedPhotos() {
        assumeTrue(MediaProvider.isOwnedPhotosEnabled(Process.myUid()));

        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        String[] packageNames = new String[]{TEST_PACKAGE_NAME};
        doReturn(packageNames).when(mMockPackageManager).getPackagesForUid(Process.myUid());

        Map<String, Integer> idVsPreGranted = populateMediaAndMediaGrantsTable();
        int totalPreGranted = (int) idVsPreGranted.values().stream()
                .filter(preGranted -> Integer.valueOf(1).equals(preGranted))
                .count();

        Bundle queryArgs = getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                new ArrayList<>(List.of(LOCAL_PROVIDER)),
                new ArrayList<>(List.of("image/*")),
                MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                Process.myUid());
        queryArgs.putBoolean("is_preview_session", true);

        try (Cursor cr = PickerDataLayerV2.queryPreviewMedia(mMockContext, queryArgs)) {
            assertEquals(totalPreGranted, cr.getCount());
            cr.moveToFirst();
            for (int i = 0; i < totalPreGranted; i++) {
                int id = cr.getInt(cr.getColumnIndexOrThrow("id"));
                int preGranted = cr.getInt(cr.getColumnIndexOrThrow("is_pre_granted"));
                assertEquals(1, (int) idVsPreGranted.get(String.valueOf(id)));
                assertEquals(1, preGranted);
                cr.moveToNext();
            }
        }
    }

    private Map<String, Integer> populateMediaAndMediaGrantsTable() {

        Map<String, Integer> idVsExpectedPreGrantedValue = new HashMap<>();

        // 1. ownerPackageName != TEST_PACKAGE_NAME and no media grants.
        // preGranted should be false
        Cursor cursorWithDifferentOwnerPackageName = getMediaCursorWithOwnerPackageNameAndUserId(
                LOCAL_ID_1, TEST_DIFFERENT_PACKAGE_NAME);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorWithDifferentOwnerPackageName,
                /*writeCount*/ 1);
        idVsExpectedPreGrantedValue.put(LOCAL_ID_1, 0);

        // 2. ownerPackageName == TEST_PACKAGE_NAME and no media grants.
        // preGranted should be true
        Cursor cursorWithCorrectOwnerPackageName = getMediaCursorWithOwnerPackageNameAndUserId(
                LOCAL_ID_2, TEST_PACKAGE_NAME);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorWithCorrectOwnerPackageName,
                /*writeCount*/ 1);
        idVsExpectedPreGrantedValue.put(LOCAL_ID_2, 1);

        // 3. ownerPackageName != TEST_PACKAGE_NAME
        // and media_grants with packageName == TEST_PACKAGE_NAME.
        // preGranted should be true
        Cursor cursorWithCorrectMediaGrants = getMediaCursorWithOwnerPackageNameAndUserId(
                LOCAL_ID_3, TEST_DIFFERENT_PACKAGE_NAME);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorWithCorrectMediaGrants,
                /*writeCount*/ 1);
        assertInsertGrantsOperation(mFacade, getMediaGrantsCursor(LOCAL_ID_3, TEST_PACKAGE_NAME,
                UserHandle.myUserId()), /*writeCount*/ 1);
        idVsExpectedPreGrantedValue.put(LOCAL_ID_3, 1);

        // 4. ownerPackageName != TEST_PACKAGE_NAME
        // and media_grants with packageName != TEST_PACKAGE_NAME.
        // preGranted should be false
        Cursor cursorWithDifferentMediaGrants = getMediaCursorWithOwnerPackageNameAndUserId(
                LOCAL_ID_4, TEST_DIFFERENT_PACKAGE_NAME);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorWithDifferentMediaGrants,
                /*writeCount*/ 1);
        assertInsertGrantsOperation(mFacade, getMediaGrantsCursor(LOCAL_ID_4,
                TEST_DIFFERENT_PACKAGE_NAME, UserHandle.myUserId()), /*writeCount*/ 1);
        idVsExpectedPreGrantedValue.put(LOCAL_ID_4, 0);

        return idVsExpectedPreGrantedValue;
    }

    private Cursor getMediaCursorWithOwnerPackageNameAndUserId(String id, String ownerPackageName) {
        String[] projectionKey = new String[]{
                CloudMediaProviderContract.MediaColumns.ID,
                CloudMediaProviderContract.MediaColumns.MEDIA_STORE_URI,
                CloudMediaProviderContract.MediaColumns.DATE_TAKEN_MILLIS,
                CloudMediaProviderContract.MediaColumns.SYNC_GENERATION,
                CloudMediaProviderContract.MediaColumns.SIZE_BYTES,
                CloudMediaProviderContract.MediaColumns.MIME_TYPE,
                CloudMediaProviderContract.MediaColumns.STANDARD_MIME_TYPE_EXTENSION,
                CloudMediaProviderContract.MediaColumns.DURATION_MILLIS,
                CloudMediaProviderContract.MediaColumns.IS_FAVORITE,
                CloudMediaProviderContract.MediaColumns.HEIGHT,
                CloudMediaProviderContract.MediaColumns.WIDTH,
                CloudMediaProviderContract.MediaColumns.ORIENTATION,
                CloudMediaProviderContract.MediaColumns.OWNER_PACKAGE_NAME,
                CloudMediaProviderContract.MediaColumns.USER_ID
        };

        String[] projectionValue = new String[]{
                id,
                null,
                String.valueOf(DATE_TAKEN_MS),
                String.valueOf(GENERATION_MODIFIED),
                String.valueOf(1),
                JPEG_IMAGE_MIME_TYPE,
                String.valueOf(STANDARD_MIME_TYPE_EXTENSION),
                String.valueOf(DURATION_MS),
                String.valueOf(0),
                String.valueOf(HEIGHT),
                String.valueOf(WIDTH),
                String.valueOf(ORIENTATION),
                ownerPackageName,
                String.valueOf(UserHandle.myUserId())
        };

        MatrixCursor c = new MatrixCursor(projectionKey);
        c.addRow(projectionValue);
        return c;
    }

    @Test
    public void testQueryLocalMediaForPreview() {
        Cursor cursorForMediaWithoutGrants = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorForMediaWithGrants = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS,
                GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorForMediaWithGrantsButDeSelected = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS,
                GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorForMediaWithoutGrants,
                /* writeCount */1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorForMediaWithGrants,
                /* writeCount */1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorForMediaWithGrantsButDeSelected,
                /* writeCount */1);

        int testUid = setupAndGetTestUid();

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        Bundle extras = getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 3,
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                new ArrayList<>(Arrays.asList("video/*")),
                MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                testUid);

        extras.putBoolean("is_preview_session", true);
        extras.putBoolean("is_first_page", true);
        extras.putStringArrayList("current_de_selection", new ArrayList<>(List.of(LOCAL_ID_3)));
        extras.putStringArrayList("current_selection", new ArrayList<>(List.of(LOCAL_ID_1)));

        // Expected result:
        // 1. one item with LOCAL_ID_1 that has been added as current selection.
        // 2. one item with LOCAL_ID_2 which is a pre-granted item.
        // 3. item with LOCAL_ID_3 should not be included in the cursor because it is de-selected.

        try (Cursor cr = PickerDataLayerV2.queryPreviewMedia(
                mMockContext, extras)) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            // verify item with isPreGranted as false.
            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE, MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                    /* isPreGranted */ false);

            // verify item with isPreGranted as true.
            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE,
                    MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                    /* isPreGranted */ true);
        }
    }

    @Test
    public void testQueryMediaSets()
            throws RequestObsoleteException, PackageManager.NameNotFoundException {
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add("image/*");
        String mediaSetId1 = CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_APP_FOLDERS + ":"
                + PACKAGE_NAME1;
        String mediaSetId2 = "mediaSetId2";
        String displayName1 = "displayName1";
        String displayName2 = "displayName2";
        String coverId1 = "56";
        String coverId2 = "76";
        String categoryId = "id";

        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.icon = RES_ID1;
        doReturn(applicationInfo).when(mMockPackageManager).getApplicationInfo(PACKAGE_NAME1, 0);


        String[] columns = new String[]{
                CloudMediaProviderContract.MediaSetColumns.ID,
                CloudMediaProviderContract.MediaSetColumns.DISPLAY_NAME,
                CloudMediaProviderContract.MediaSetColumns.MEDIA_COVER_ID
        };

        // Prep the media sets table
        MatrixCursor cursor = new MatrixCursor(columns);
        cursor.addRow(new Object[] { mediaSetId1, displayName1, coverId1 });
        cursor.addRow(new Object[] { mediaSetId2, displayName2, coverId2  });

        MediaSetsDatabaseUtil.cacheMediaSets(
                mFacade.getDatabase(), cursor, categoryId,
                SearchProvider.AUTHORITY, mimeTypes);

        Bundle extras = new Bundle();
        extras.putString(
                MediaSetsSyncRequestParams.KEY_PARENT_CATEGORY_AUTHORITY,
                SearchProvider.AUTHORITY);
        extras.putStringArrayList(
                MediaSetsSyncRequestParams.KEY_MIME_TYPES,
                new ArrayList<>(List.of("image/*")));
        extras.putString(MediaSetsSyncRequestParams.KEY_PARENT_CATEGORY_ID, categoryId);
        extras.putStringArrayList("providers", new ArrayList<>(List.of(SearchProvider.AUTHORITY)));

        try (Cursor mediaSets = PickerDataLayerV2.queryMediaSets(mMockContext, extras)) {
            assertNotNull(mediaSets);
            assertEquals(2, mediaSets.getCount());

            if (mediaSets.moveToFirst()) {
                String retrievedMediaSetId1 = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName()));
                assertEquals(mediaSetId1, retrievedMediaSetId1);
                String retrievedDisplayName1 = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaGroupResponseColumns.DISPLAY_NAME.getColumnName()));
                assertEquals(displayName1, retrievedDisplayName1);
                String retrievedUri1 = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaGroupResponseColumns.UNWRAPPED_COVER_URI
                                .getColumnName()
                ));
                assertTrue(retrievedUri1.contains(coverId1));
                String retrievedBadgeUri1 = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaGroupResponseColumns.BADGE_ICON_URI
                                .getColumnName()
                ));
                String expectedBadgeUri = String.format(
                        Locale.ROOT,
                        "android.resource://%s@%s/%s",
                        MY_USER_ID, PACKAGE_NAME1, RES_ID1);
                assertEquals(expectedBadgeUri, retrievedBadgeUri1);

                mediaSets.moveToNext();
                String retrievedMediaSetId2 = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName()));
                assertEquals(mediaSetId2, retrievedMediaSetId2);
                String retrievedDisplayName2 = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaGroupResponseColumns.DISPLAY_NAME.getColumnName()));
                assertEquals(displayName2, retrievedDisplayName2);
                String retrievedUri2 = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaGroupResponseColumns.UNWRAPPED_COVER_URI
                                .getColumnName()
                ));
                assertTrue(retrievedUri2.contains(coverId2));
                String retrievedBadgeUri2 = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaGroupResponseColumns.BADGE_ICON_URI
                                .getColumnName()
                ));
                assertNull(retrievedBadgeUri2);
            }
        }
    }

    @Test
    public void testQueryMediaSets_returnsMediaSetsFromSingleAuthority()
            throws RequestObsoleteException, PackageManager.NameNotFoundException {
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add("image/*");
        String localMediaSetId = "localMediaSetId";
        String cloudMediaSetId = "cloudMediaSetId";
        String displayNameLocal = "Local Album";
        String displayNameCloud = "Cloud Album";
        String coverId = "123";
        String categoryId = "id";

        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.icon = RES_ID1;
        doReturn(applicationInfo).when(mMockPackageManager).getApplicationInfo(anyString(),
                anyInt());

        String[] columns = new String[]{
                CloudMediaProviderContract.MediaSetColumns.ID,
                CloudMediaProviderContract.MediaSetColumns.DISPLAY_NAME,
                CloudMediaProviderContract.MediaSetColumns.MEDIA_COVER_ID
        };

        // Cache local media set
        MatrixCursor localCursor = new MatrixCursor(columns);
        localCursor.addRow(new Object[] { localMediaSetId, displayNameLocal, coverId });
        MediaSetsDatabaseUtil.cacheMediaSets(
                mFacade.getDatabase(), localCursor, categoryId,
                LOCAL_PROVIDER, mimeTypes);

        // Cache cloud media set
        MatrixCursor cloudCursor = new MatrixCursor(columns);
        cloudCursor.addRow(new Object[] { cloudMediaSetId, displayNameCloud, coverId });
        MediaSetsDatabaseUtil.cacheMediaSets(
                mFacade.getDatabase(), cloudCursor, categoryId,
                CLOUD_PROVIDER, mimeTypes);

        // Query for media set from LOCAL_PROVIDER
        Bundle extras = new Bundle();
        extras.putString(
                MediaSetsSyncRequestParams.KEY_PARENT_CATEGORY_AUTHORITY,
                LOCAL_PROVIDER);
        extras.putStringArrayList(
                MediaSetsSyncRequestParams.KEY_MIME_TYPES,
                new ArrayList<>(List.of("image/*")));
        extras.putString(MediaSetsSyncRequestParams.KEY_PARENT_CATEGORY_ID, categoryId);
        // Both providers are present as available providers list
        extras.putStringArrayList("providers",
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));

        try (Cursor mediaSets = PickerDataLayerV2.queryMediaSets(mMockContext, extras)) {
            assertNotNull(mediaSets);
            assertEquals(1, mediaSets.getCount());

            mediaSets.moveToFirst();
            String retrievedMediaSetId = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                    PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName()));
            assertEquals(localMediaSetId, retrievedMediaSetId);
            String retrievedDisplayName = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                    PickerSQLConstants.MediaGroupResponseColumns.DISPLAY_NAME.getColumnName()));
            assertEquals(displayNameLocal, retrievedDisplayName);
            String retrievedAuthority = mediaSets.getString(mediaSets.getColumnIndexOrThrow(
                    PickerSQLConstants.MediaGroupResponseColumns.AUTHORITY.getColumnName()));
            assertEquals(LOCAL_PROVIDER, retrievedAuthority);
        }
    }

    @Test
    public void queryMediaOnlyLocalWithPreSelection() {
        Cursor cursorLocal1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorLocal2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorCloud1 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorCloud2 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursorCloud1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursorCloud2, 1);

        Bundle queryArgs = getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));

        queryArgs.putInt(Intent.EXTRA_UID, Process.myUid());
        // add uris for selection
        String uriPlaceHolder = "content://media/picker/"
                + UserHandle.myUserId() + "/%s/media/%s";
        queryArgs.putStringArrayList("pre_selection_uris", new ArrayList<>(Arrays.asList(
                String.format(uriPlaceHolder, LOCAL_PROVIDER, LOCAL_ID_1) // valid local uri
        )));


        try (Cursor cr = PickerDataLayerV2.queryMediaForPreSelection(
                mMockContext, queryArgs)) {
            // only the 1 local item in the input uris should be returned.
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(1);
            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void queryMediaCloudOnlyWithPreSelection() {
        Cursor cursorLocal1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorLocal2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorCloud1 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorCloud2 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursorCloud1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursorCloud2, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());


        Bundle queryArgs = getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));

        queryArgs.putInt(Intent.EXTRA_UID, Process.myUid());
        // add uris for selection
        String uriPlaceHolder = "content://media/picker/"
                + UserHandle.myUserId() + "/%s/media/%s";
        queryArgs.putStringArrayList("pre_selection_uris", new ArrayList<>(Arrays.asList(
                String.format(uriPlaceHolder, CLOUD_PROVIDER, CLOUD_ID_2) // valid cloud uri
        )));


        try (Cursor cr = PickerDataLayerV2.queryMediaForPreSelection(
                mMockContext, queryArgs)) {
            // only the 1 cloud items in the input uris should be returned.
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            assertMediaCursor(cr, CLOUD_ID_2, CLOUD_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

   @Test
    public void testQueryMediaForPreSelection_ignoresCrossUserUris() {
        Cursor cursorLocal = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal, 1);

        Bundle queryArgs = getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        queryArgs.putInt(Intent.EXTRA_UID, Process.myUid());

        // Construct a Uri that points to a different user: currentUser + 1,
        // but has the same media ID (LOCAL_ID_1).
        int differentUserId = UserHandle.myUserId() + 1;
        String crossUserUri =
                String.format(
                        "content://media/picker/%d/%s/media/%s",
                        differentUserId, LOCAL_PROVIDER, LOCAL_ID_1);

        queryArgs.putStringArrayList(
                "pre_selection_uris",
                new ArrayList<>(Arrays.asList(crossUserUri))
        );

        try (Cursor cr = PickerDataLayerV2.queryMediaForPreSelection(mMockContext, queryArgs)) {
            // Verify that the media item is not returned in the preselection.
            assertWithMessage("Cross-user preselection URI should be ignored")
                    .that(cr.getCount())
                    .isEqualTo(0);
        }
    }

    @Test
    public void queryMediaWithCloudQueryEnabledWithPreSelection() {
        Cursor cursorLocal1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorLocal2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorCloud1 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursorCloud2 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursorCloud1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursorCloud2, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());


        Bundle queryArgs = getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));

        queryArgs.putInt(Intent.EXTRA_UID, Process.myUid());
        // add uris for selection
        String uriPlaceHolder = "content://media/picker/"
                + UserHandle.myUserId() + "/%s/media/%s";
        queryArgs.putStringArrayList("pre_selection_uris", new ArrayList<>(Arrays.asList(
                String.format(uriPlaceHolder, LOCAL_PROVIDER, LOCAL_ID_1), // valid local uri
                String.format(uriPlaceHolder, CLOUD_PROVIDER, CLOUD_ID_2), // valid cloud uri
                // uri for invalid media as LOCAL_ID_3 this has not been inserted,
                String.format(uriPlaceHolder, LOCAL_PROVIDER, LOCAL_ID_3),
                // uri with invalid cloud provider
                String.format(uriPlaceHolder, "cloud.provider.invalid", CLOUD_ID_2)
                )));


        try (Cursor cr = PickerDataLayerV2.queryMediaForPreSelection(
                mMockContext, queryArgs)) {
            // only the 2 items in the input uris should be returned.
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, CLOUD_ID_2, CLOUD_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testQueryMediaForPreSelection_ignoresMalformedUris() {
        // Insert a valid local media item first
        Cursor cursorLocal = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal, 1);

        Bundle queryArgs = getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        queryArgs.putInt(Intent.EXTRA_UID, Process.myUid());

        // Prepare various malformed URIs
        // 1. Too few segments
        String malformedUri1 = "content://media/picker";
        // 2. Non-numeric user ID
        String malformedUri2 = String.format(
                "content://media/picker/abc/%s/media/%s", LOCAL_PROVIDER, LOCAL_ID_1);
        // 3. Wrong segment count (4 segments)
        String malformedUri3 = "content://media/picker/0/local/media";

        // A valid URI pointing to the inserted local media
        String acceptablePickerUri = "content://media/picker/"
                + UserHandle.myUserId() + "/" + LOCAL_PROVIDER + "/media/" + LOCAL_ID_1;

        queryArgs.putStringArrayList(
                "pre_selection_uris",
                new ArrayList<>(Arrays.asList(
                        malformedUri1, malformedUri2, malformedUri3, acceptablePickerUri))
        );

        try (Cursor cr = PickerDataLayerV2.queryMediaForPreSelection(mMockContext, queryArgs)) {
            assertWithMessage("Malformed preselection URIs should be ignored, "
                    + "but valid URIs should be returned")
                    .that(cr.getCount())
                    .isEqualTo(1);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testQueryMediaForPreSelection_ignoresFakeAuthorityUris() {
        // Insert a valid local media item first
        Cursor cursorLocal = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal, 1);

        Bundle queryArgs = getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        queryArgs.putInt(Intent.EXTRA_UID, Process.myUid());

        // Prepare fake authority URIs
        // Fake authority "fake.auth"
        String fakeUri1 = "content://fake.auth/picker/" + UserHandle.myUserId() + "/"
                + LOCAL_PROVIDER + "/media/" + LOCAL_ID_1;
        // Fake authority "media.fake"
        String fakeUri2 = "content://media.fake/picker/" + UserHandle.myUserId() + "/"
                + LOCAL_PROVIDER + "/media/" + LOCAL_ID_1;

        // A valid URI pointing to the inserted local media
        String acceptablePickerUri = "content://media/picker/"
                + UserHandle.myUserId() + "/" + LOCAL_PROVIDER + "/media/" + LOCAL_ID_1;

        queryArgs.putStringArrayList(
                "pre_selection_uris",
                new ArrayList<>(Arrays.asList(fakeUri1, fakeUri2, acceptablePickerUri))
        );

        try (Cursor cr = PickerDataLayerV2.queryMediaForPreSelection(mMockContext, queryArgs)) {
            assertWithMessage("URIs with fake authorities should be ignored, "
                    + "but valid URIs should be returned")
                    .that(cr.getCount())
                    .isEqualTo(1);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testQueryMediaForPreSelection_ignoresInvalidPickerSegments() {
        // Insert a valid local media item first
        Cursor cursorLocal = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursorLocal, 1);

        Bundle queryArgs = getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        queryArgs.putInt(Intent.EXTRA_UID, Process.myUid());

        // Prepare URIs with invalid picker segments
        // Invalid first segment "invalid_segment"
        String invalidUri1 = "content://media/invalid_segment/"
                + UserHandle.myUserId() + "/" + LOCAL_PROVIDER + "/media/" + LOCAL_ID_1;
        // Invalid first segment "picker_internal"
        String invalidUri2 = "content://media/picker_internal/"
                + UserHandle.myUserId() + "/" + LOCAL_PROVIDER + "/media/" + LOCAL_ID_1;
        // Invalid fourth segment "album" instead of "media"
        String invalidUri3 = "content://media/picker/"
                + UserHandle.myUserId() + "/" + LOCAL_PROVIDER + "/album/" + LOCAL_ID_1;
        // Invalid fourth segment "invalid" instead of "media"
        String invalidUri4 = "content://media/picker/"
                + UserHandle.myUserId() + "/" + LOCAL_PROVIDER + "/invalid/" + LOCAL_ID_1;

        // A valid URI pointing to the inserted local media
        String acceptablePickerUri = "content://media/picker/"
                + UserHandle.myUserId() + "/" + LOCAL_PROVIDER + "/media/" + LOCAL_ID_1;

        queryArgs.putStringArrayList(
                "pre_selection_uris",
                new ArrayList<>(Arrays.asList(
                        invalidUri1, invalidUri2, invalidUri3, invalidUri4, acceptablePickerUri))
        );

        try (Cursor cr = PickerDataLayerV2.queryMediaForPreSelection(mMockContext, queryArgs)) {
            assertWithMessage("URIs with invalid picker segments should be ignored, "
                    + "but valid URIs should be returned")
                    .that(cr.getCount())
                    .isEqualTo(1);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testQueryMediaInMediaSet() throws RequestObsoleteException {
        final Cursor cursor1 = getLocalMediaCursor(LOCAL_ID_1, 0);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        final Cursor cursor2 = getLocalMediaCursor(LOCAL_ID_2, 0);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        final Cursor cursor3 = getCloudMediaCursor(CLOUD_ID_2, LOCAL_ID_2, 0);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);

        Long mediaSetPickerId = 1L;

        int cloudRowsInserted = MediaInMediaSetsDatabaseUtil.cacheMediaOfMediaSet(
                mFacade.getDatabase(), List.of(
                        getContentValues(LOCAL_ID_2, CLOUD_ID_2, mediaSetPickerId)
                ), CLOUD_PROVIDER
        );
        assertEquals(
                "Number of rows inserted should be equal to the number of items in the cursor,",
                /*expected*/cloudRowsInserted,
                /*actual*/1);

        int localRowsInserted = MediaInMediaSetsDatabaseUtil.cacheMediaOfMediaSet(
                mFacade.getDatabase(), List.of(
                        getContentValues(LOCAL_ID_1, null, mediaSetPickerId)
                ), LOCAL_PROVIDER
        );
        assertEquals(
                "Number of rows inserted is incorrect",
                localRowsInserted,
                1);

        Bundle extras = new Bundle();
        extras.putInt("current_page_size", 100);
        extras.putInt("next_page_size", 100);
        extras.putStringArrayList("providers",
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        extras.putString("intent_action", MediaStore.ACTION_PICK_IMAGES);
        extras.putLong(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_PICKER_ID,
                mediaSetPickerId);
        extras.putString(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_AUTHORITY,
                LOCAL_PROVIDER);

        try (Cursor cursor =
                     PickerDataLayerV2.queryMediaInMediaSet(mMockContext, extras)) {
            assertWithMessage("Cursor should not be null")
                    .that(cursor)
                    .isNotNull();

            assertWithMessage("Cursor count is not as expected")
                    .that(cursor.getCount())
                    .isEqualTo(2);

            cursor.moveToFirst();
            assertWithMessage("Media ID is not as expected in the search results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.MEDIA_ID.getProjectedName())))
                    .isEqualTo(LOCAL_ID_2);

            cursor.moveToNext();
            assertWithMessage("Media ID is not as expected in the search results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.MEDIA_ID.getProjectedName())))
                    .isEqualTo(LOCAL_ID_1);
        }
    }

    @Test
    public void testQueryMediaInMediaSetWithGrants() {
        // Media item without grants
        final Cursor cursor1 = getLocalMediaCursor(LOCAL_ID_1, 0);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        // Media item with grants
        final Cursor cursor2 = getLocalMediaCursor(LOCAL_ID_2, 0);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        final Cursor cursor3 = getCloudMediaCursor(CLOUD_ID_2, LOCAL_ID_2, 0);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);

        Long mediaSetPickerId = 1L;

        int cloudRowsInserted = MediaInMediaSetsDatabaseUtil.cacheMediaOfMediaSet(
                mFacade.getDatabase(), List.of(
                        getContentValues(LOCAL_ID_2, CLOUD_ID_2, mediaSetPickerId)
                ), CLOUD_PROVIDER
        );
        assertEquals(
                "Unexpected number of rows inserted for cloud media",
                /*expected*/1,
                /*actual*/cloudRowsInserted);

        int localRowsInserted = MediaInMediaSetsDatabaseUtil.cacheMediaOfMediaSet(
                mFacade.getDatabase(), List.of(
                        getContentValues(LOCAL_ID_1, null, mediaSetPickerId)
                ), LOCAL_PROVIDER
        );
        assertEquals(
                "Unexpected number of rows inserted for local media",
                /*expected*/1,
                /*actual*/localRowsInserted);

        int testUid = setupAndGetTestUid();

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        Bundle extras = new Bundle();
        extras.putInt("current_page_size", 100);
        extras.putInt("next_page_size", 100);
        extras.putStringArrayList("providers",
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        extras.putString("intent_action", MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP);
        extras.putInt(Intent.EXTRA_UID, testUid);
        extras.putLong(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_PICKER_ID,
                mediaSetPickerId);
        extras.putString(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_AUTHORITY,
                LOCAL_PROVIDER);


        try (Cursor cursor =
                     PickerDataLayerV2.queryMediaInMediaSet(mMockContext, extras)) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cursor.getCount()).isEqualTo(2);

            // verify item with isPreGranted as true.
            cursor.moveToFirst();
            assertMediaCursor(cursor, LOCAL_ID_2, LOCAL_PROVIDER, 0L,
                    MP4_VIDEO_MIME_TYPE, MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                    /* isPreGranted */ true);

            // verify item with isPreGranted as false.
            cursor.moveToNext();
            assertMediaCursor(cursor, LOCAL_ID_1, LOCAL_PROVIDER, 0L, MP4_VIDEO_MIME_TYPE,
                    MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP,
                    /* isPreGranted */ false);
        }
    }

    @Test
    public void testQueryMediaInMediaSetWithGrantsForInvalidUid() {
        Long mediaSetPickerId = 1L;

        int testUid = -1;

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());

        Bundle extras = new Bundle();
        extras.putInt("current_page_size", 100);
        extras.putInt("next_page_size", 100);
        extras.putStringArrayList("providers",
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        extras.putString("intent_action", MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP);
        extras.putInt(Intent.EXTRA_UID, testUid);
        extras.putLong(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_PICKER_ID,
                mediaSetPickerId);
        extras.putString(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_AUTHORITY,
                LOCAL_PROVIDER);


        try (Cursor cursor =
                     PickerDataLayerV2.queryMediaInMediaSet(mMockContext, extras)) {
            fail("Query should throw runtime exception for invalid uid.");
        } catch (Exception exception) {
            assertWithMessage("UnexpectedException thrown for invalid uid.")
                    .that(exception).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_REVOKE_ACCESS_OWNED_PHOTOS)
    @EnableCompatChanges({MediaProvider.ENABLE_OWNED_PHOTOS})
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    public void testQueryMediaInMediaSetWithPreGrantsForOwnedPhotos() {
        assumeTrue(MediaProvider.isOwnedPhotosEnabled(Process.myUid()));

        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        String[] packageNames = new String[]{TEST_PACKAGE_NAME};
        doReturn(packageNames).when(mMockPackageManager).getPackagesForUid(Process.myUid());
        Map<String, Integer> idVsExpectedPreGrantedValue = populateMediaAndMediaGrantsTable();
        int totalCount = idVsExpectedPreGrantedValue.size();

        Long mediaSetPickerId = 1L;
        int localRowsInserted = MediaInMediaSetsDatabaseUtil.cacheMediaOfMediaSet(
                mFacade.getDatabase(), List.of(
                        getContentValues(LOCAL_ID_1, null, mediaSetPickerId),
                        getContentValues(LOCAL_ID_2, null, mediaSetPickerId),
                        getContentValues(LOCAL_ID_3, null, mediaSetPickerId),
                        getContentValues(LOCAL_ID_4, null, mediaSetPickerId)
                ), LOCAL_PROVIDER
        );

        assertEquals(
                "Unexpected number of rows inserted for local media",
                localRowsInserted,
                totalCount);

        Bundle extras = new Bundle();
        extras.putInt("current_page_size", 100);
        extras.putInt("next_page_size", 100);
        extras.putStringArrayList("providers",
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        extras.putString("intent_action", MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP);
        extras.putInt(Intent.EXTRA_UID, Process.myUid());
        extras.putLong(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_PICKER_ID,
                mediaSetPickerId);
        extras.putString(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_AUTHORITY,
                LOCAL_PROVIDER);

        try (Cursor cursor = PickerDataLayerV2.queryMediaInMediaSet(
                mMockContext, extras)) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cursor.getCount()).isEqualTo(totalCount);

            cursor.moveToFirst();
            for (int i = 0; i < totalCount; i++) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int isPreGranted = cursor.getInt(cursor.getColumnIndexOrThrow("is_pre_granted"));
                assertEquals(idVsExpectedPreGrantedValue.get(String.valueOf(id)).intValue(),
                        isPreGranted);
                cursor.moveToNext();
            }
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_REVOKE_ACCESS_OWNED_PHOTOS)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void testFetchMediaGrantsCount() {
        int testUid = 123;
        int userId = PickerSyncController.uidToUserId(testUid);
        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        String[] packageNames = new String[]{TEST_PACKAGE_NAME};
        doReturn(packageNames).when(mMockPackageManager).getPackagesForUid(testUid);


        // insert 2 grants corresponding to testUid.
        assertInsertGrantsOperation(mFacade,
                getMediaGrantsCursor(LOCAL_ID_1, TEST_PACKAGE_NAME, userId), /* writeCount */1);
        assertInsertGrantsOperation(mFacade,
                getMediaGrantsCursor(LOCAL_ID_2, TEST_PACKAGE_NAME, userId), /* writeCount */1);

        // insert grants with different packageName or userIds.
        String TEST_PACKAGE_NAME_2 = "package.name.two";
        int TEST_USER_ID_2 = 10;

        // same id but different packageName
        assertInsertGrantsOperation(mFacade, getMediaGrantsCursor(LOCAL_ID_2, TEST_PACKAGE_NAME_2,
                UserHandle.myUserId()), /* writeCount */1);
        // same id but different userId
        assertInsertGrantsOperation(mFacade, getMediaGrantsCursor(LOCAL_ID_2, TEST_PACKAGE_NAME,
                TEST_USER_ID_2), /* writeCount */1);
        // both packageName and userId different
        assertInsertGrantsOperation(mFacade,
                getMediaGrantsCursor(LOCAL_ID_2, TEST_PACKAGE_NAME_2, TEST_USER_ID_2), 1);
        // every aspect different
        assertInsertGrantsOperation(mFacade,
                getMediaGrantsCursor(LOCAL_ID_3, TEST_PACKAGE_NAME_2, TEST_USER_ID_2), 1);

        Bundle input = new Bundle();
        input.putInt(Intent.EXTRA_UID, testUid);

        try (Cursor cr = PickerDataLayerV2.fetchCountForPreGrantedItems(
                mMockContext, input)) {

            // cursor should only contain 1 row that represents the count.
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(1);

            // verify that the cursor contains the count. Ensure that only 2 grants are considered
            // even when there were total 4 grants inserted. This ensures that the grants were
            // filtered properly based on the packageName and UserId.
            cr.moveToFirst();
            int columnIndexForCount = cr.getColumnIndex(COLUMN_GRANTS_COUNT);
            assertWithMessage(
                    "column index should not be -1.")
                    .that(columnIndexForCount).isNotEqualTo(-1);
            assertWithMessage(
                    "Unexpected number grants count, expected to be 2.")
                    .that(cr.getInt(columnIndexForCount)).isEqualTo(2);
        }
    }

    @Test
    public void queryMediaWithCloudQueryEnabled() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, DATE_TAKEN_MS, /* pageSize */ 2,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, CLOUD_ID, CLOUD_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testQueryCloudMediaSortOrder() {
        Cursor cursor1 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS + 1,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 3,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            // CLOUD_ID_1 has the most recent date taken.
            assertMediaCursor(cr, CLOUD_ID_1, CLOUD_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            // CLOUD_ID_2 and CLOUD_ID_3 have the same date taken but the Picker ID of CLOUD_ID_3
            // should be greater.
            assertMediaCursor(cr, CLOUD_ID_3, CLOUD_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, CLOUD_ID_2, CLOUD_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testQueryLocalAndCloudMediaSortOrder() {
        Cursor cursor1 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 3,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            // CLOUD_ID_1 has the most recent date taken.
            assertMediaCursor(cr, CLOUD_ID_1, CLOUD_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            // LOCAL_ID_1 and CLOUD_ID_3 have the same date taken but the Picker ID of LOCAL_ID_1
            // should be greater.
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, CLOUD_ID_2, CLOUD_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testQueryMediaActionGetContent() {
        Cursor cursor1 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        final Bundle mediaQueryExtras = getMediaQueryExtras(Long.MAX_VALUE,
                Long.MAX_VALUE, /* pageSize */ 3,
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        mediaQueryExtras.putString("intent_action", Intent.ACTION_GET_CONTENT);
        try (Cursor cr = PickerDataLayerV2.queryMedia(mMockContext, mediaQueryExtras)) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            // CLOUD_ID_1 has the most recent date taken.
            assertMediaCursor(cr, CLOUD_ID_1, CLOUD_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE, Intent.ACTION_GET_CONTENT);

            cr.moveToNext();
            // LOCAL_ID_1 and CLOUD_ID_3 have the same date taken but the Picker ID of LOCAL_ID_1
            // should be greater.
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE,
                    Intent.ACTION_GET_CONTENT);

            cr.moveToNext();
            assertMediaCursor(cr, CLOUD_ID_2, CLOUD_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE,
                    Intent.ACTION_GET_CONTENT);
        }
    }

    @Test
    public void testLocalAndCloudQueryDedupe() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS - 1);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 3,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, CLOUD_ID_2, CLOUD_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testAllVideoMimeTypeFilter() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        new ArrayList<>(Arrays.asList("video/*"))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testAllImageMimeTypeFilter() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        new ArrayList<>(Arrays.asList("image/*"))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_4, LOCAL_PROVIDER, DATE_TAKEN_MS, GIF_IMAGE_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_3, LOCAL_PROVIDER, DATE_TAKEN_MS,
                    PNG_IMAGE_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, JPEG_IMAGE_MIME_TYPE);
        }
    }

    @Test
    public void testSpecificMimeTypeFilter() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        new ArrayList<>(
                                Arrays.asList(GIF_IMAGE_MIME_TYPE, JPEG_IMAGE_MIME_TYPE))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_4, LOCAL_PROVIDER, DATE_TAKEN_MS, GIF_IMAGE_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, JPEG_IMAGE_MIME_TYPE);
        }
    }

    @Test
    public void testMixOfGeneralAndSpecificMimeTypeFilters() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        new ArrayList<String>(
                                Arrays.asList(GIF_IMAGE_MIME_TYPE, "video/*"))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_4, LOCAL_PROVIDER, DATE_TAKEN_MS,
                    GIF_IMAGE_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }


    @Test
    public void testDefaultAlbumsWithCloudQueriesDisabled() {
        Cursor cursor1 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(CLOUD_ID_4, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor4, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            // Favorites album will be displayed by default
            cr.moveToFirst();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            // Camera album will be displayed by default
            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);
        }
    }

    @Test
    public void testFavoritesAlbumMediaWithCloudDisabled() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS,
                GIF_IMAGE_MIME_TYPE, /* isFavorite */ true);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                GIF_IMAGE_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS - 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                GIF_IMAGE_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        LOCAL_PROVIDER),
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES)) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS, GIF_IMAGE_MIME_TYPE);
        }
    }

    @Test
    public void testFavoritesAlbumMediaWithLocalAndCloudItems() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS,
                GIF_IMAGE_MIME_TYPE, /* isFavorite */ true);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                GIF_IMAGE_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS - 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                GIF_IMAGE_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        LOCAL_PROVIDER),
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES)) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS, GIF_IMAGE_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, GIF_IMAGE_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, CLOUD_ID_2, CLOUD_PROVIDER, DATE_TAKEN_MS - 1,
                    GIF_IMAGE_MIME_TYPE);
        }
    }

    @Test
    public void testVideosMergedAlbumMediaWithCloudDisabled() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS,
                MP4_VIDEO_MIME_TYPE, /* isFavorite */ true);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS - 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                GIF_IMAGE_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        LOCAL_PROVIDER),
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS)) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(1);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testVideosMergedAlbumMedia() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS,
                MP4_VIDEO_MIME_TYPE, /* isFavorite */ true);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS - 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                GIF_IMAGE_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        LOCAL_PROVIDER),
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS)) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, CLOUD_ID_2, CLOUD_PROVIDER, DATE_TAKEN_MS - 1,
                    MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testLocalAlbumMediaQuery() {
        Cursor cursor1 = getAlbumMediaCursor(LOCAL_ID_1, /* cloudId */ null, DATE_TAKEN_MS + 1);
        Cursor cursor2 = getAlbumMediaCursor(LOCAL_ID_2, /* cloudId */ null, DATE_TAKEN_MS);
        Cursor cursor3 = getAlbumMediaCursor(/* localId */ null, CLOUD_ID_1, DATE_TAKEN_MS);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER,
                getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1), 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER,
                getLocalMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS), 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER,
                getCloudMediaCursor(CLOUD_ID_1, null, DATE_TAKEN_MS), 1);

        assertAddAlbumMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1, ALBUM_ID);
        assertAddAlbumMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1, ALBUM_ID);
        assertAddAlbumMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1, ALBUM_ID);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        LOCAL_PROVIDER),
                ALBUM_ID)) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS,
                    MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testCloudAlbumMediaQuery() {
        Cursor cursor1 = getAlbumMediaCursor(LOCAL_ID_1, /* cloudId */ null, DATE_TAKEN_MS);
        Cursor cursor2 = getAlbumMediaCursor(/* localId */ null, CLOUD_ID_1, DATE_TAKEN_MS);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER,
                getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS), 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER,
                getCloudMediaCursor(CLOUD_ID_1, null, DATE_TAKEN_MS), 1);

        assertAddAlbumMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1, ALBUM_ID);
        assertAddAlbumMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1, ALBUM_ID);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        CLOUD_PROVIDER),
                ALBUM_ID)) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, CLOUD_ID_1, CLOUD_PROVIDER, DATE_TAKEN_MS,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS,
                    MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testCloudAlbumMediaQueryWithLocalCopyOfCloudItem() {
        // Item 1: present both locally and on cloud (deduped)
        Cursor cursor1 = getAlbumMediaCursor(LOCAL_ID_1, CLOUD_ID_1, DATE_TAKEN_MS);
        // Item 2: purely cloud
        Cursor cursor2 = getAlbumMediaCursor(/* localId */ null, CLOUD_ID_2, DATE_TAKEN_MS - 1);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER,
                getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS), 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER,
                getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS), 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER,
                getCloudMediaCursor(CLOUD_ID_2, null, DATE_TAKEN_MS - 1), 1);

        assertAddAlbumMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1, ALBUM_ID);
        assertAddAlbumMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1, ALBUM_ID);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)),
                        CLOUD_PROVIDER),
                ALBUM_ID)) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            // Deduplicated item should return local info
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            // Pure cloud item should return cloud info
            assertMediaCursor(cr, CLOUD_ID_2, CLOUD_PROVIDER, DATE_TAKEN_MS - 1,
                    MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testCloudAlbumMediaQueryWhenCloudIsDisabled() {
        Cursor cursor1 = getAlbumMediaCursor(LOCAL_ID_1, /* cloudId */ null, DATE_TAKEN_MS + 1);
        Cursor cursor2 = getAlbumMediaCursor(LOCAL_ID_2, /* cloudId */ null, DATE_TAKEN_MS);
        Cursor cursor3 = getAlbumMediaCursor(/* localId */ null, CLOUD_ID_1, DATE_TAKEN_MS);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER,
                getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1), 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER,
                getLocalMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS), 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER,
                getCloudMediaCursor(CLOUD_ID_1, null, DATE_TAKEN_MS), 1);

        assertAddAlbumMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1, ALBUM_ID);
        assertAddAlbumMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1, ALBUM_ID);
        assertAddAlbumMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1, ALBUM_ID);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER)),
                        CLOUD_PROVIDER),
                ALBUM_ID)) {

            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS,
                    MP4_VIDEO_MIME_TYPE);
        }
    }

    @Test
    public void testLocalVideosAlbum() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            // Favorites album will be displayed by default
            cr.moveToFirst();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            // Camera album will be displayed by default
            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_2);
        }
    }

    @Test
    public void testCloudVideosAlbum() {
        Cursor cursor1 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor2 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(CLOUD_ID_4, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor4, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            // Favorites albums will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID, MediaSource.LOCAL);

            // Camera album will be displayed by default
            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ CLOUD_ID_2,
                    MediaSource.REMOTE);
        }
    }

    @Test
    public void testMergedLocalAndCloudVideosAlbum() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                MP4_VIDEO_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS - 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            // Favorites albums will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            // Camera album will be displayed by default
            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_1);
        }
    }

    @Test
    public void testLocalFavoritesAlbum() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);
        Cursor cursor3 = getMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor4, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            // Favorites albums will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_2);

            // Camera album will be displayed by default
            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);
        }
    }

    @Test
    public void testCloudFavoritesAlbum() {
        Cursor cursor1 = getMediaCursor(CLOUD_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);
        Cursor cursor2 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_3, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor4 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, PNG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ CLOUD_ID_1,
                    MediaSource.REMOTE);

            // Camera album will be displayed by default
            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            cr.moveToNext();
            // Videos album will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID,
                    MediaSource.LOCAL);
        }
    }

    @Test
    public void testMergedLocalAndCloudFavoritesAlbum() {
        Cursor cursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS,
                GIF_IMAGE_MIME_TYPE, /* isFavorite */ true);
        Cursor cursor2 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS,
                GENERATION_MODIFIED, /* mediaStoreUri */ null, /* sizeBytes */ 1,
                GIF_IMAGE_MIME_TYPE, STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        Cursor cursor3 = getMediaCursor(CLOUD_ID_2, DATE_TAKEN_MS - 1, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 2, GIF_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ true);

        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor3, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_1);

            cr.moveToNext();
            // Camera album will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            cr.moveToNext();
            // Videos album will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);
        }
    }

    @Test
    public void testLocalAlbums() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        Cursor cursor2 = getAlbumCursor(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                DATE_TAKEN_MS, LOCAL_ID_2, LOCAL_PROVIDER);
        mLocalProvider.setQueryResult(cursor2);

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            // Favorites album will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            cr.moveToNext();
            // Camera album will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_2);

            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_1);
        }
    }

    @Test
    public void testCloudAlbums() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        Cursor cursor2 = getAlbumCursor("CloudAlbum", DATE_TAKEN_MS, CLOUD_ID_1, CLOUD_PROVIDER);
        mCloudProvider.setQueryResult(cursor2);

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(4);

            cr.moveToFirst();
            // Favorites albums will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            cr.moveToNext();
            // Camera album will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_1);

            cr.moveToNext();
            assertAlbumCursor(cr, /* albumId */ "CloudAlbum", CLOUD_PROVIDER,
                    /* dateTaken */ DATE_TAKEN_MS, /* coverMediaId */ CLOUD_ID_1);
        }
    }

    @Test
    public void testCloudAndLocalAlbums() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        List<Cursor> localAlbumCursors = new ArrayList<>();
        localAlbumCursors.add(getAlbumCursor(
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_DOWNLOADS,
                DATE_TAKEN_MS, LOCAL_ID_2, LOCAL_PROVIDER));
        localAlbumCursors.add(getAlbumCursor(
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_SCREENSHOTS,
                DATE_TAKEN_MS, LOCAL_ID_2, LOCAL_PROVIDER));
        localAlbumCursors.add(getAlbumCursor(
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                DATE_TAKEN_MS, LOCAL_ID_2, LOCAL_PROVIDER));
        MergeCursor allLocalAlbumsCursor =
                new MergeCursor(localAlbumCursors.toArray(new Cursor[0]));
        mLocalProvider.setQueryResult(allLocalAlbumsCursor);

        Cursor cursor5 = getAlbumCursor("CloudAlbum", DATE_TAKEN_MS, CLOUD_ID_1, CLOUD_PROVIDER);
        mCloudProvider.setQueryResult(cursor5);

        try (Cursor cr = PickerDataLayerV2.queryAlbums(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(6);

            cr.moveToFirst();
            // Favorites albums will be displayed by default
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE,
                    /* coverMediaId */ EMPTY_MEDIA_ID);

            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_2);

            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_1);

            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_SCREENSHOTS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_2);

            cr.moveToNext();
            assertAlbumCursor(cr,
                    /* albumId */ CloudMediaProviderContract.AlbumColumns.ALBUM_ID_DOWNLOADS,
                    LOCAL_PROVIDER, /* dateTaken */ Long.MAX_VALUE, /* coverMediaId */ LOCAL_ID_2);


            cr.moveToNext();
            assertAlbumCursor(cr, /* albumId */ "CloudAlbum", CLOUD_PROVIDER,
                    /* dateTaken */ DATE_TAKEN_MS, /* coverMediaId */ CLOUD_ID_1);
        }
    }

    @Test
    public void testVideoAlbumsWithImageMime() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, JPEG_IMAGE_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        List<Cursor> localAlbumCursors = new ArrayList<>();
        localAlbumCursors.add(getAlbumCursor(
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                DATE_TAKEN_MS, LOCAL_ID_2, LOCAL_PROVIDER));

        MergeCursor allLocalAlbumsCursor =
                new MergeCursor(localAlbumCursors.toArray(new Cursor[0]));
        mLocalProvider.setQueryResult(allLocalAlbumsCursor);

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER)),
                        LOCAL_PROVIDER),
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS)) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(0);
        }
    }

    @Test
    public void testVideoAlbumsWithVideoMime() {
        Cursor cursor1 = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        List<Cursor> localAlbumCursors = new ArrayList<>();
        localAlbumCursors.add(getAlbumCursor(
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS,
                DATE_TAKEN_MS, LOCAL_ID_2, LOCAL_PROVIDER));

        MergeCursor allLocalAlbumsCursor =
                new MergeCursor(localAlbumCursors.toArray(new Cursor[0]));
        mLocalProvider.setQueryResult(allLocalAlbumsCursor);

        try (Cursor cr = PickerDataLayerV2.queryAlbumMedia(
                mMockContext, getAlbumMediaQueryExtras(
                        Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 10,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER)),
                        LOCAL_PROVIDER),
                CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS)) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(1);
        }
    }

    @Test
    public void testPaginationFirstPage() {
        Cursor cursor1 = getLocalMediaCursor(LOCAL_ID, DATE_TAKEN_MS);
        Cursor cursor2 = getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1);
        Cursor cursor3 = getLocalMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS + 2);
        Cursor cursor4 = getLocalMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS + 3);
        Cursor cursor5 = getLocalMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS + 4);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor5, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, /* pageSize */ 3,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_4, LOCAL_PROVIDER, DATE_TAKEN_MS + 4,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_3, LOCAL_PROVIDER, DATE_TAKEN_MS + 3,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS + 2,
                    MP4_VIDEO_MIME_TYPE);

            assertWithMessage("Unexpected value of previous date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(Long.MIN_VALUE);

            assertWithMessage("Unexpected value of previous picker id in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_ID.getKey(), Long.MIN_VALUE))
                    .isEqualTo(Long.MIN_VALUE);

            assertWithMessage("Unexpected value of next date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .NEXT_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(DATE_TAKEN_MS + 1);

            assertWithMessage("Unexpected value of next picker id in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .NEXT_PAGE_ID.getKey(), Long.MIN_VALUE))
                    .isEqualTo(2);

            assertWithMessage("Unexpected value of items before count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_BEFORE_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(0);

            assertWithMessage("Unexpected value of items after count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_AFTER_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(2);
        }
    }

    @Test
    public void testPaginationLastPage() {
        Cursor cursor1 = getLocalMediaCursor(LOCAL_ID, DATE_TAKEN_MS);
        Cursor cursor2 = getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1);
        Cursor cursor3 = getLocalMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS + 2);
        Cursor cursor4 = getLocalMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS + 3);
        Cursor cursor5 = getLocalMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS + 4);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor5, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE,
                        DATE_TAKEN_MS + 1,
                        /* pageSize */ 2,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);

            assertWithMessage("Unexpected value of previous date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(DATE_TAKEN_MS + 3);

            assertWithMessage("Unexpected value of previous picker id in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_ID.getKey(), Long.MIN_VALUE))
                    .isEqualTo(4);

            assertWithMessage("Unexpected value of next date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .NEXT_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(Long.MIN_VALUE);

            assertWithMessage("Unexpected value of next picker id in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .NEXT_PAGE_ID.getKey(), Long.MIN_VALUE))
                    .isEqualTo(Long.MIN_VALUE);

            assertWithMessage("Unexpected value of items before count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_BEFORE_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(3);
            assertWithMessage("Unexpected value of items before count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_AFTER_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(0);
        }
    }

    @Test
    public void testPaginationLastPagePartialPreviousPage() {
        Cursor cursor1 = getLocalMediaCursor(LOCAL_ID, DATE_TAKEN_MS);
        Cursor cursor2 = getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1);
        Cursor cursor3 = getLocalMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS + 2);
        Cursor cursor4 = getLocalMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS + 3);
        Cursor cursor5 = getLocalMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS + 4);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor5, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE,
                        DATE_TAKEN_MS + 2,
                        /* pageSize */ 3,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(3);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS + 2,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);

            assertWithMessage("Unexpected value of previous date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(DATE_TAKEN_MS + 4);

            assertWithMessage("Unexpected value of previous picker id in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_ID.getKey(), Long.MIN_VALUE))
                    .isEqualTo(5);

            assertWithMessage("Unexpected value of next date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .NEXT_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(Long.MIN_VALUE);

            assertWithMessage("Unexpected value of next picker id in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .NEXT_PAGE_ID.getKey(), Long.MIN_VALUE))
                    .isEqualTo(Long.MIN_VALUE);

            assertWithMessage("Unexpected value of items before count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_BEFORE_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(2);

            assertWithMessage("Unexpected value of items after count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_AFTER_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(0);
        }
    }

    @Test
    public void testPaginationMiddlePage() {
        Cursor cursor1 = getLocalMediaCursor(LOCAL_ID, DATE_TAKEN_MS);
        Cursor cursor2 = getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1);
        Cursor cursor3 = getLocalMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS + 2);
        Cursor cursor4 = getLocalMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS + 3);
        Cursor cursor5 = getLocalMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS + 4);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor5, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE,
                        DATE_TAKEN_MS + 2,
                        /* pageSize */ 2,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_2, LOCAL_PROVIDER, DATE_TAKEN_MS + 2,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            assertWithMessage("Unexpected value of previous date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(DATE_TAKEN_MS + 4);

            assertWithMessage("Unexpected value of previous picker id in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_ID.getKey(), Long.MIN_VALUE))
                    .isEqualTo(5);

            assertWithMessage("Unexpected value of next date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .NEXT_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(DATE_TAKEN_MS);

            assertWithMessage("Unexpected value of next picker id in the media cursor.")
                    .that(cr.getExtras().getLong("next_page_picker_id", Long.MIN_VALUE))
                    .isEqualTo(1);

            assertWithMessage("Unexpected value of items before count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_BEFORE_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(2);
            assertWithMessage("Unexpected value of items after count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_AFTER_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(1);
        }
    }

    @Test
    public void testPaginationLastPageWhenLastPageItemsLessThanPageSize() {
        Cursor cursor1 = getLocalMediaCursor(LOCAL_ID, DATE_TAKEN_MS);
        Cursor cursor2 = getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS + 1);
        Cursor cursor3 = getLocalMediaCursor(LOCAL_ID_2, DATE_TAKEN_MS + 2);
        Cursor cursor4 = getLocalMediaCursor(LOCAL_ID_3, DATE_TAKEN_MS + 3);
        Cursor cursor5 = getLocalMediaCursor(LOCAL_ID_4, DATE_TAKEN_MS + 4);

        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor2, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor3, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor4, 1);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor5, 1);

        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        try (Cursor cr = PickerDataLayerV2.queryMedia(
                mMockContext, getMediaQueryExtras(Long.MAX_VALUE,
                        DATE_TAKEN_MS + 1,
                        /* pageSize */ 3,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER))))) {
            assertWithMessage(
                    "Unexpected number of rows in media query result")
                    .that(cr.getCount()).isEqualTo(2);

            cr.moveToFirst();
            assertMediaCursor(cr, LOCAL_ID_1, LOCAL_PROVIDER, DATE_TAKEN_MS + 1,
                    MP4_VIDEO_MIME_TYPE);

            cr.moveToNext();
            assertMediaCursor(cr, LOCAL_ID, LOCAL_PROVIDER, DATE_TAKEN_MS, MP4_VIDEO_MIME_TYPE);

            assertWithMessage("Unexpected value of previous date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(DATE_TAKEN_MS + 4);

            assertWithMessage("Unexpected value of previous picker id in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .PREV_PAGE_ID.getKey(), Long.MIN_VALUE))
                    .isEqualTo(5);

            assertWithMessage("Unexpected value of next date taken in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .NEXT_PAGE_DATE_TAKEN.getKey(), Long.MIN_VALUE))
                    .isEqualTo(Long.MIN_VALUE);

            assertWithMessage("Unexpected value of next picker id in the media cursor.")
                    .that(cr.getExtras().getLong(PickerSQLConstants.MediaResponseExtras
                            .NEXT_PAGE_ID.getKey(), Long.MIN_VALUE))
                    .isEqualTo(Long.MIN_VALUE);

            assertWithMessage("Unexpected value of items before count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_BEFORE_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(3);
            assertWithMessage("Unexpected value of items before count in the media cursor.")
                    .that(cr.getExtras().getInt(PickerSQLConstants.MediaResponseExtras
                            .ITEMS_AFTER_COUNT.getKey(), Integer.MIN_VALUE))
                    .isEqualTo(0);
        }
    }

    @Test
    public void testQuerySearchSuggestionsZeroState() {
        doReturn(SearchProvider.AUTHORITY).when(mMockSyncController).getCloudProvider();
        doReturn(SearchProvider.AUTHORITY).when(mMockSyncController)
                .getCloudProviderOrDefault(any());
        doReturn(mSearchState).when(mMockSyncController).getSearchState();
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());
        doReturn(true).when(mSearchState).isCloudSearchEnabled(any());

        final Bundle bundle = new Bundle();
        bundle.putString("prefix", "");
        bundle.putStringArrayList("providers", new ArrayList<>(List.of(SearchProvider.AUTHORITY)));
        final SearchSuggestionsQuery query = new SearchSuggestionsQuery(bundle);

        // Async tasks are run synchronously during tests to make tests deterministic and prevent
        // flaky test results.
        final Executor currentThreadExecutor = Runnable::run;

        try (Cursor cursor = PickerDataLayerV2.querySearchSuggestions(
                mContext, bundle, currentThreadExecutor, null)) {
            assertWithMessage("Cursor should not be null")
                    .that(cursor)
                    .isNotNull();

            assertWithMessage("Cursor count is not as expected")
                    .that(cursor.getCount())
                    .isEqualTo(SearchProvider.DEFAULT_SUGGESTION_RESULTS.getCount());

            final String projection = PickerSQLConstants.SearchSuggestionsResponseColumns
                            .MEDIA_SET_ID.getProjection();
            if (cursor.moveToFirst() && SearchProvider.DEFAULT_SUGGESTION_RESULTS.moveToFirst()) {
                do {
                    assertWithMessage("Media ID is not as expected")
                            .that(cursor.getString(cursor.getColumnIndexOrThrow(projection)))
                            .isEqualTo(SearchProvider.DEFAULT_SUGGESTION_RESULTS.getString(
                                    SearchProvider.DEFAULT_SUGGESTION_RESULTS
                                            .getColumnIndexOrThrow(projection)));
                } while (cursor.moveToNext()
                        && SearchProvider.DEFAULT_SUGGESTION_RESULTS.moveToNext());
            }
        }

        final List<SearchSuggestion> searchSuggestions = SearchSuggestionsDatabaseUtils
                .getCachedSuggestions(mFacade.getDatabase(), query);

        assertWithMessage("Suggestions should not be null")
                .that(searchSuggestions)
                .isNotNull();

        assertWithMessage("Suggestions size is not as expected")
                .that(searchSuggestions.size())
                .isEqualTo(SearchProvider.DEFAULT_SUGGESTION_RESULTS.getCount());
    }

    @Test
    public void testQuerySearchSuggestionsNonZeroState() {
        doReturn(SearchProvider.AUTHORITY).when(mMockSyncController).getCloudProvider();
        doReturn(SearchProvider.AUTHORITY).when(mMockSyncController)
                .getCloudProviderOrDefault(any());
        doReturn(mSearchState).when(mMockSyncController).getSearchState();
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());
        doReturn(true).when(mSearchState).isCloudSearchEnabled(any());

        final Bundle bundle = new Bundle();
        bundle.putString("prefix", "x");
        bundle.putStringArrayList("providers", new ArrayList<>(List.of(SearchProvider.AUTHORITY)));
        final SearchSuggestionsQuery query = new SearchSuggestionsQuery(bundle);

        // Async tasks are run synchronously during tests to make tests deterministic and prevent
        // flaky test results.
        final Executor currentThreadExecutor = Runnable::run;

        try (Cursor cursor = PickerDataLayerV2.querySearchSuggestions(
                mContext, bundle, currentThreadExecutor, null)) {
            assertWithMessage("Cursor should not be null")
                    .that(cursor)
                    .isNotNull();

            assertWithMessage("Cursor count is not as expected")
                    .that(cursor.getCount())
                    .isEqualTo(SearchProvider.DEFAULT_SUGGESTION_RESULTS.getCount());

            final String projection = PickerSQLConstants.SearchSuggestionsResponseColumns
                    .MEDIA_SET_ID.getProjection();
            if (cursor.moveToFirst() && SearchProvider.DEFAULT_SUGGESTION_RESULTS.moveToFirst()) {
                do {
                    assertWithMessage("Media ID is not as expected")
                            .that(cursor.getString(cursor.getColumnIndexOrThrow(projection)))
                            .isEqualTo(SearchProvider.DEFAULT_SUGGESTION_RESULTS.getString(
                                    SearchProvider.DEFAULT_SUGGESTION_RESULTS
                                            .getColumnIndexOrThrow(projection)));
                } while (cursor.moveToNext()
                        && SearchProvider.DEFAULT_SUGGESTION_RESULTS.moveToNext());
            }
        }

        final List<SearchSuggestion> searchSuggestions = SearchSuggestionsDatabaseUtils
                .getCachedSuggestions(mFacade.getDatabase(), query);

        assertWithMessage("Suggestions should not be null")
                .that(searchSuggestions)
                .isNotNull();

        assertWithMessage("Suggestions size is not as expected")
                .that(searchSuggestions.size())
                .isEqualTo(0);
    }

    @Test
    public void testQuerySearchSuggestionsWithHistory() {
        doReturn(SearchProvider.AUTHORITY).when(mMockSyncController).getCloudProvider();
        doReturn(SearchProvider.AUTHORITY).when(mMockSyncController)
                .getCloudProviderOrDefault(any());
        doReturn(mSearchState).when(mMockSyncController).getSearchState();
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());
        doReturn(true).when(mSearchState).isCloudSearchEnabled(any());

        final Bundle bundle = new Bundle();
        bundle.putString("prefix", "");
        bundle.putStringArrayList("providers", new ArrayList<>(List.of(SearchProvider.AUTHORITY)));
        final SearchSuggestionsQuery query = new SearchSuggestionsQuery(bundle);

        // Async tasks are run synchronously during tests to make tests deterministic and prevent
        // flaky test results.
        final Executor currentThreadExecutor = Runnable::run;

        SearchSuggestionsDatabaseUtils.saveSearchHistory(
                mFacade.getDatabase(),
                new SearchTextRequest(null, "mountains"));

        try (Cursor cursor = PickerDataLayerV2.querySearchSuggestions(
                mContext, bundle, currentThreadExecutor, null)) {
            assertWithMessage("Cursor should not be null")
                    .that(cursor)
                    .isNotNull();

            assertWithMessage("Cursor count is not as expected")
                    .that(cursor.getCount())
                    .isEqualTo(SearchProvider.DEFAULT_SUGGESTION_RESULTS.getCount() + 1);

            final String projection = PickerSQLConstants.SearchSuggestionsResponseColumns
                    .MEDIA_SET_ID.getProjection();

            cursor.moveToFirst();
            assertWithMessage("Media ID is not as expected")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(projection)))
                    .isNull();

            if (cursor.moveToNext() && SearchProvider.DEFAULT_SUGGESTION_RESULTS.moveToFirst()) {
                do {
                    assertWithMessage("Media ID is not as expected")
                            .that(cursor.getString(cursor.getColumnIndexOrThrow(projection)))
                            .isEqualTo(SearchProvider.DEFAULT_SUGGESTION_RESULTS.getString(
                                    SearchProvider.DEFAULT_SUGGESTION_RESULTS
                                            .getColumnIndexOrThrow(projection)));
                } while (cursor.moveToNext()
                        && SearchProvider.DEFAULT_SUGGESTION_RESULTS.moveToNext());
            }
        }
    }

    @Test
    public void testHandleNewSearchRequest() {
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaForSearch(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaForSearch(any(), any());
        doReturn(mMockOperation).when(mMockWorkManager)
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));
        doReturn(mMockFuture).when(mMockOperation).getResult();

        final String searchText = "volcano";
        final Bundle extras = getCreateSearchRequestExtras(new SearchTextRequest(null, searchText));
        final Executor currentThreadExecutor = Runnable::run;

        final Bundle result = PickerDataLayerV2.handleNewSearchRequest(
                mMockContext, extras, currentThreadExecutor, mMockWorkManager);

        // Assert that a new search request was created
        assertThat(result).isNotNull();
        assertThat(result.getInt("search_request_id")).isEqualTo(1);

        // Assert that local sync, cloud sync and cache clearing work was scheduled
        verify(mMockWorkManager, times(3))
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));

        // Assert that search request was saved as search history in database
        final List<SearchSuggestion> suggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("", new ArrayList<>()));
        assertThat(suggestions.size()).isEqualTo(1);
        assertThat(suggestions.get(0).getSearchText()).isEqualTo(searchText);
    }

    @Test
    public void testDeleteSearchHistorySuggestionSearchText() {
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaForSearch(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaForSearch(any(), any());
        doReturn(mMockOperation).when(mMockWorkManager)
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));
        doReturn(mMockFuture).when(mMockOperation).getResult();

        final String searchText = "volcano";
        final Bundle extras = getCreateSearchRequestExtras(new SearchTextRequest(null, searchText));
        final Executor currentThreadExecutor = Runnable::run;

        final Bundle result = PickerDataLayerV2.handleNewSearchRequest(
                mMockContext, extras, currentThreadExecutor, mMockWorkManager);

        // Assert that a new search request was created
        assertThat(result).isNotNull();
        assertThat(result.getInt("search_request_id")).isEqualTo(1);

        // Assert that local sync, cloud sync and cache clearing work was scheduled
        verify(mMockWorkManager, times(3))
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));

        // Assert that search request was saved as search history in database
        final List<SearchSuggestion> suggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("", new ArrayList<>()));
        assertThat(suggestions.size()).isEqualTo(1);
        assertThat(suggestions.get(0).getSearchText()).isEqualTo(searchText);

        final Bundle bundle = new Bundle();
        bundle.putString("display_text", searchText);

        final int deletedRows = PickerDataLayerV2.deleteSearchHistorySuggestion(bundle);
        assertThat(deletedRows).isEqualTo(1);

        final List<SearchSuggestion> newSuggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("", new ArrayList<>()));
        assertThat(newSuggestions.size()).isEqualTo(0);
    }


    @Test
    public void testDeleteSearchHistorySuggestionSearchText_noMatch() {
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaForSearch(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaForSearch(any(), any());
        doReturn(mMockOperation).when(mMockWorkManager)
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));
        doReturn(mMockFuture).when(mMockOperation).getResult();

        final String searchText = "volcano";
        final Bundle extras = getCreateSearchRequestExtras(new SearchTextRequest(null, searchText));
        final Executor currentThreadExecutor = Runnable::run;

        final Bundle result = PickerDataLayerV2.handleNewSearchRequest(
                mMockContext, extras, currentThreadExecutor, mMockWorkManager);

        // Assert that a new search request was created
        assertThat(result).isNotNull();
        assertThat(result.getInt("search_request_id")).isEqualTo(1);

        // Assert that local sync, cloud sync and cache clearing work was scheduled
        verify(mMockWorkManager, times(3))
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));

        // Assert that search request was saved as search history in database
        final List<SearchSuggestion> suggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("", new ArrayList<>()));
        assertThat(suggestions.size()).isEqualTo(1);
        assertThat(suggestions.get(0).getSearchText()).isEqualTo(searchText);

        final Bundle bundle = new Bundle();
        bundle.putString("display_text", "different_text");

        final int deletedRows = PickerDataLayerV2.deleteSearchHistorySuggestion(bundle);
        assertThat(deletedRows).isEqualTo(0);

        final List<SearchSuggestion> newSuggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("", new ArrayList<>()));
        assertThat(newSuggestions.size()).isEqualTo(1);
    }

    @Test
    public void testDeleteSearchHistorySuggestionWithMediaSetId() {
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaForSearch(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaForSearch(any(), any());
        doReturn(mMockOperation).when(mMockWorkManager)
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));
        doReturn(mMockFuture).when(mMockOperation).getResult();

        final String searchText = "volcano";
        final String searchMediaSetId = "testMediaSetId";
        Bundle extras = getCreateSearchSuggestionRequestExtras(
                new SearchSuggestionRequest(null, searchText, searchMediaSetId,
                        null, CloudMediaProviderContract.SEARCH_SUGGESTION_HISTORY));
        final Executor currentThreadExecutor = Runnable::run;

        final Bundle result = PickerDataLayerV2.handleNewSearchRequest(
                mMockContext, extras, currentThreadExecutor, mMockWorkManager);

        // Assert that a new search request was created
        assertThat(result).isNotNull();
        assertThat(result.getInt("search_request_id")).isEqualTo(1);

        // Assert that local sync, cloud sync and cache clearing work was scheduled
        verify(mMockWorkManager, times(3))
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));

        // Assert that search request was saved as search history in database
        final List<SearchSuggestion> suggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("", new ArrayList<>()));
        assertThat(suggestions.size()).isEqualTo(1);
        assertThat(suggestions.get(0).getSearchText()).isEqualTo(searchText);
        assertThat(suggestions.get(0).getMediaSetId()).isEqualTo(searchMediaSetId);

        final Bundle bundle = new Bundle();
        bundle.putString("display_text", searchText);
        bundle.putString("media_set_id", searchMediaSetId);
        bundle.putString("authority", null);

        final int deletedRows = PickerDataLayerV2.deleteSearchHistorySuggestion(bundle);
        assertThat(deletedRows).isEqualTo(1);

        final List<SearchSuggestion> newSuggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("", new ArrayList<>()));
        assertThat(newSuggestions.size()).isEqualTo(0);
    }


    @Test
    public void testDeleteSearchHistorySuggestionWithMediaSetId_noMatch() {
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaForSearch(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaForSearch(any(), any());
        doReturn(mMockOperation).when(mMockWorkManager)
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));
        doReturn(mMockFuture).when(mMockOperation).getResult();

        final String searchText = "volcano";
        final String searchMediaSetId = "testMediaSetId";
        final String authority = null;
        Bundle extras = getCreateSearchSuggestionRequestExtras(
                new SearchSuggestionRequest(null, searchText, searchMediaSetId,
                        authority, CloudMediaProviderContract.SEARCH_SUGGESTION_HISTORY));
        final Executor currentThreadExecutor = Runnable::run;

        final Bundle result = PickerDataLayerV2.handleNewSearchRequest(
                mMockContext, extras, currentThreadExecutor, mMockWorkManager);

        // Assert that a new search request was created
        assertThat(result).isNotNull();
        assertThat(result.getInt("search_request_id")).isEqualTo(1);

        // Assert that local sync, cloud sync and cache clearing work was scheduled
        verify(mMockWorkManager, times(3))
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));

        // Assert that search request was saved as search history in database
        final List<SearchSuggestion> suggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("", new ArrayList<>()));
        assertThat(suggestions.size()).isEqualTo(1);
        assertThat(suggestions.get(0).getSearchText()).isEqualTo(searchText);
        assertThat(suggestions.get(0).getMediaSetId()).isEqualTo(searchMediaSetId);
        assertThat(suggestions.get(0).getAuthority()).isEqualTo(authority);

        final Bundle bundle = new Bundle();
        bundle.putString("display_text", searchText);
        bundle.putString("media_set_id", "differentMediaSetId");
        bundle.putString("authority", null);

        final int deletedRows = PickerDataLayerV2.deleteSearchHistorySuggestion(bundle);
        assertThat(deletedRows).isEqualTo(0);

        final List<SearchSuggestion> newSuggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("", new ArrayList<>()));
        assertThat(newSuggestions.size()).isEqualTo(1);
    }

    @Test
    public void testDeleteSearchHistorySuggestionWithMediaSetIdAndAuthority_noMatch() {
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaForSearch(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaForSearch(any(), any());
        doReturn(mMockOperation).when(mMockWorkManager)
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));
        doReturn(mMockFuture).when(mMockOperation).getResult();

        final String searchText = "volcano";
        final String searchMediaSetId = "testMediaSetId";
        final String authority = CloudProviderPrimary.AUTHORITY;
        Bundle extras = getCreateSearchSuggestionRequestExtras(
                new SearchSuggestionRequest(null, searchText, searchMediaSetId,
                        authority, CloudMediaProviderContract.SEARCH_SUGGESTION_HISTORY));
        final Executor currentThreadExecutor = Runnable::run;

        final Bundle result = PickerDataLayerV2.handleNewSearchRequest(
                mMockContext, extras, currentThreadExecutor, mMockWorkManager);

        // Assert that a new search request was created
        assertThat(result).isNotNull();
        assertThat(result.getInt("search_request_id")).isEqualTo(1);

        // Assert that local sync, cloud sync and cache clearing work was scheduled
        verify(mMockWorkManager, times(3))
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));

        // Assert that search request was saved as search history in database
        final List<SearchSuggestion> suggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("",
                                new ArrayList<>(List.of(CloudProviderPrimary.AUTHORITY))));
        assertThat(suggestions.size()).isEqualTo(1);
        assertThat(suggestions.get(0).getSearchText()).isEqualTo(searchText);
        assertThat(suggestions.get(0).getMediaSetId()).isEqualTo(searchMediaSetId);
        assertThat(suggestions.get(0).getAuthority()).isEqualTo(authority);

        final Bundle bundle = new Bundle();
        bundle.putString("display_text", searchText);
        bundle.putString("media_set_id", searchMediaSetId);
        bundle.putString("authority", "differentAuthority");

        final int deletedRows = PickerDataLayerV2.deleteSearchHistorySuggestion(bundle);
        assertThat(deletedRows).isEqualTo(0);

        final List<SearchSuggestion> newSuggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("",
                                new ArrayList<>(List.of(CloudProviderPrimary.AUTHORITY))));
        assertThat(newSuggestions.size()).isEqualTo(1);
    }

    @Test
    public void testDeleteSearchHistorySuggestionWithMediaSetIdAndAuthority() {
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaForSearch(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaForSearch(any(), any());
        doReturn(mMockOperation).when(mMockWorkManager)
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));
        doReturn(mMockFuture).when(mMockOperation).getResult();

        final String searchText = "volcano";
        final String searchMediaSetId = "testMediaSetId";
        final String cloudAuthority = CloudProviderPrimary.AUTHORITY;
        Bundle extras = getCreateSearchSuggestionRequestExtras(
                new SearchSuggestionRequest(null, searchText, searchMediaSetId,
                        cloudAuthority, CloudMediaProviderContract.SEARCH_SUGGESTION_HISTORY));
        final Executor currentThreadExecutor = Runnable::run;

        final Bundle result = PickerDataLayerV2.handleNewSearchRequest(
                mMockContext, extras, currentThreadExecutor, mMockWorkManager);

        // Assert that a new search request was created
        assertThat(result).isNotNull();
        assertThat(result.getInt("search_request_id")).isEqualTo(1);

        final String searchAuthority = SearchProvider.AUTHORITY;
        final String searchMediaSetId1 = "testMediaSetId1";
        Bundle extras1 = getCreateSearchSuggestionRequestExtras(
                new SearchSuggestionRequest(null, searchText, searchMediaSetId1,
                        searchAuthority, CloudMediaProviderContract.SEARCH_SUGGESTION_HISTORY));

        final Bundle result1 = PickerDataLayerV2.handleNewSearchRequest(
                mMockContext, extras1, currentThreadExecutor, mMockWorkManager);

        // Assert that a new search request was created
        assertThat(result1).isNotNull();
        assertThat(result1.getInt("search_request_id")).isEqualTo(2);

        final String searchMediaSetId2 = "testMediaSetId2";
        Bundle extras2 = getCreateSearchSuggestionRequestExtras(
                new SearchSuggestionRequest(null, searchText, searchMediaSetId2,
                        cloudAuthority, CloudMediaProviderContract.SEARCH_SUGGESTION_HISTORY));

        final Bundle result2 = PickerDataLayerV2.handleNewSearchRequest(
                mMockContext, extras2, currentThreadExecutor, mMockWorkManager);

        // Assert that a new search request was created
        assertThat(result2).isNotNull();
        assertThat(result2.getInt("search_request_id")).isEqualTo(3);

        // Assert that local sync, cloud sync and cache clearing work was scheduled
        verify(mMockWorkManager, times(9))
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));

        // Assert that search request was saved as search history in database
        final List<SearchSuggestion> suggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("",
                                new ArrayList<>(List.of(cloudAuthority, searchAuthority))));
        assertThat(suggestions.size()).isEqualTo(3);

        final Bundle bundle = new Bundle();
        bundle.putString("display_text", searchText);
        bundle.putString("media_set_id", searchMediaSetId);
        bundle.putString("authority", cloudAuthority);

        final int deletedRows = PickerDataLayerV2.deleteSearchHistorySuggestion(bundle);
        assertThat(deletedRows).isEqualTo(1);

        final List<SearchSuggestion> newSuggestions =
                SearchSuggestionsDatabaseUtils.getHistorySuggestions(
                        mFacade.getDatabase(),
                        new SearchSuggestionsQuery("",
                                new ArrayList<>(List.of(cloudAuthority, searchAuthority))));
        assertThat(newSuggestions.size()).isEqualTo(2);
    }

    @Test
    public void testTriggerMediaSetsSyncRequest() {
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaSets(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaSets(any(), any());
        doReturn(mMockWorkContinuation)
                .when(mMockWorkManager)
                .beginUniqueWork(
                        anyString(), any(ExistingWorkPolicy.class), any(List.class));
        // Handle .then chaining
        doReturn(mMockWorkContinuation)
                .when(mMockWorkContinuation)
                .then(any(List.class));
        doReturn(mMockOperation).when(mMockWorkContinuation).enqueue();
        doReturn(mMockFuture).when(mMockOperation).getResult();

        Bundle extras = new Bundle();
        extras.putString(
                MediaSetsSyncRequestParams.KEY_PARENT_CATEGORY_AUTHORITY,
                SearchProvider.AUTHORITY);
        extras.putStringArray(
                MediaSetsSyncRequestParams.KEY_MIME_TYPES,
                new String[] { "image/*" });
        extras.putString(MediaSetsSyncRequestParams.KEY_PARENT_CATEGORY_ID, "id");
        extras.putStringArrayList("providers", new ArrayList<>(List.of(SearchProvider.AUTHORITY)));

        PickerDataLayerV2.triggerMediaSetsSync(extras, mContext, mMockWorkManager);

        // Assert that both local and cloud syncs were scheduled
        verify(mMockWorkManager, times(1)).beginUniqueWork(
                anyString(), any(ExistingWorkPolicy.class), any(List.class));
        verify(mMockWorkContinuation, times(1)).then(any(List.class));
        verify(mMockWorkContinuation, times(1)).enqueue();
    }

    @Test
    public void testTriggerMediaInMediaSetSyncRequest() {
        doReturn(true).when(mMockSyncController).shouldQueryLocalMediaSets(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaSets(any(), any());
        doReturn(mMockOperation).when(mMockWorkManager)
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));
        doReturn(mMockFuture).when(mMockOperation).getResult();

        Bundle extras = new Bundle();
        extras.putString(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_AUTHORITY,
                SearchProvider.AUTHORITY);
        extras.putLong(MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_PICKER_ID, 1);
        extras.putStringArrayList("providers", new ArrayList<>(List.of(SearchProvider.AUTHORITY)));

        PickerDataLayerV2.triggerMediaSyncForMediaSet(extras, mContext, mMockWorkManager);

        // Assert that both local and cloud syncs were scheduled
        verify(mMockWorkManager, times(1))
                .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class),
                        any(OneTimeWorkRequest.class));
    }

    @Test
    public void testQueryCategoriesAndAlbums() {
        doReturn(SearchProvider.AUTHORITY).when(mMockSyncController).getCloudProvider();
        doReturn(SearchProvider.AUTHORITY).when(mMockSyncController)
                .getCloudProviderOrDefault(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());
        doReturn(true).when(mCategoriesState).areCategoriesEnabled(any(), any());

        final Cursor cursor1 = getLocalMediaCursor(LOCAL_ID_1, 0);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        final Cursor cursor2 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, 0);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);

        try (Cursor cursor = PickerDataLayerV2.queryCategoriesAndAlbums(
                mContext,
                getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, 100,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, SearchProvider.AUTHORITY))),
                /* cancellationSignal */ null,
                mTestConfigStore)) {
            assertWithMessage("Unexpected count of albums and categories")
                    .that(cursor.getCount())
                    .isEqualTo(5);

            cursor.moveToFirst();
            // Assert for Favorites album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(0L);

            cursor.moveToNext();
            // Assert for Camera album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(1L);

            cursor.moveToNext();
            // Assert that the next media group is people and pets category
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.CATEGORY);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(2L);

            cursor.moveToNext();
            // Assert for Video merged album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(3L);

            cursor.moveToNext();
            // Assert that the next media group is a cloud album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            final Uri coverUri = Uri.parse(
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .UNWRAPPED_COVER_URI.getColumnName())));
            assertWithMessage("Unexpected cover uri")
                    .that(coverUri.getLastPathSegment())
                    .isEqualTo(LOCAL_ID_1);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(4L);
        }
    }

    @Test
    public void testQueryCategoriesWithCloudAlbumsAsCategories() {
        doReturn(CloudProviderPrimary.AUTHORITY).when(mMockSyncController).getCloudProvider();
        doReturn(CloudProviderPrimary.AUTHORITY).when(mMockSyncController)
                .getCloudProviderOrDefault(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());
        doReturn(true).when(mCategoriesState).areCategoriesEnabled(any(), any());
        doReturn(true).when(mCategoriesState)
                .isCloudAlbumsAsCategoryEnabled(any(), any());

        final Cursor cursor1 = getLocalMediaCursor(LOCAL_ID_1, 0);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, cursor1, 1);
        final Cursor cursor2 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, 0);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cursor2, 1);

        try (Cursor cursor = PickerDataLayerV2.queryCategoriesAndAlbums(
                mContext,
                getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, 100,
                        new ArrayList<>(Arrays.asList(
                                LOCAL_PROVIDER,
                                CloudProviderPrimary.AUTHORITY))),
                /* cancellationSignal */ null,
                mTestConfigStore)) {
            assertWithMessage("Unexpected count of albums and categories")
                    .that(cursor.getCount())
                    .isEqualTo(5);

            cursor.moveToFirst();
            // Assert for Favorites album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(0L);

            cursor.moveToNext();
            // Assert for Camera album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(1L);

            cursor.moveToNext();
            // Assert that the next media group is people and pets category
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.CATEGORY);
            assertWithMessage("Unexpected group id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_PEOPLE_AND_PETS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(2L);

            cursor.moveToNext();
            // Assert that the next media group is user albums
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.CATEGORY);
            assertWithMessage("Unexpected group id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_USER_ALBUMS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(3L);

            cursor.moveToNext();
            // Assert for Video merged album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(4L);
        }
    }

    @Test
    public void testQueryCategoriesWithLocalCategories() {
        Cursor videoCursor = getMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS, GENERATION_MODIFIED,
                /* mediaStoreUri */ null, /* sizeBytes */ 1, MP4_VIDEO_MIME_TYPE,
                STANDARD_MIME_TYPE_EXTENSION, /* isFavorite */ false);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, videoCursor, 1);

        doReturn(true).when(mCategoriesState).areCategoriesEnabled(any(), any());
        final Cursor cursor1 = getMediaCategoriesCursor(
                CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_DEVICE_FOLDERS);
        final Cursor cursor2 = getMediaCategoriesCursor(
                CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_APP_FOLDERS);
        final Cursor cursor3 = getMediaCategoriesCursor(
                CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_SD_CARD);
        final Cursor localCategoryCursor = new MergeCursor(new Cursor[]{cursor1, cursor2, cursor3});
        mLocalProvider.setQueryResult(localCategoryCursor);

        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());
        final Cursor cursor4 = getAlbumCursor("CloudAlbum", DATE_TAKEN_MS, CLOUD_ID_1,
                CLOUD_PROVIDER);
        mCloudProvider.setQueryResult(cursor4);

        mTestConfigStore.setSdCardCategoryInPhotoPickerEnabled(true);
        try (Cursor cursor = PickerDataLayerV2.queryCategoriesAndAlbums(
                mMockContext,
                getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, 100,
                        new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, SearchProvider.AUTHORITY))),
                /* cancellationSignal */ null,
                mTestConfigStore)) {
            assertWithMessage("Unexpected count of albums and categories")
                    .that(cursor.getCount())
                    .isEqualTo(7);

            cursor.moveToFirst();
            // Assert for Favorites album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(0L);

            cursor.moveToNext();
            // Assert for Camera album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(1L);

            cursor.moveToNext();
            // Assert that the next media group is "from this device" category
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.CATEGORY);
            assertWithMessage("Unexpected group id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_DEVICE_FOLDERS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(2L);

            cursor.moveToNext();
            // Assert that the next media group is "from your apps" category
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.CATEGORY);
            assertWithMessage("Unexpected group id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_APP_FOLDERS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(3L);

            cursor.moveToNext();
            // Assert for Video merged album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(4L);

            cursor.moveToNext();
            // Assert that the next media group is "Sd card" category
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.CATEGORY);
            assertWithMessage("Unexpected group id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_SD_CARD);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(5L);

            cursor.moveToNext();
            // Assert that the next media group is a cloud album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            final Uri coverUri = Uri.parse(
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .UNWRAPPED_COVER_URI.getColumnName())));
            assertWithMessage("Unexpected cover id")
                    .that(coverUri.getLastPathSegment())
                    .isEqualTo(CLOUD_ID_1);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(6L);
        }

    }

    @Test
    public void testQueryCategories_invalidCategoryType_categoryCursorSkipped() {
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());
        doReturn(true).when(mCategoriesState).areCategoriesEnabled(any(), any());
        doReturn(true).when(mCategoriesState)
                .isCloudAlbumsAsCategoryEnabled(any(), any());
        // Cloud media category cursor with invalid media category type
        Cursor cloudCursor1 = PickerDbTestUtils.getMediaCategoriesCursor(
                CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_APP_FOLDERS);
        // Cloud media category cursor with valid media category type
        Cursor cloudCursor2 = PickerDbTestUtils.getMediaCategoriesCursor(
                CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_USER_ALBUMS);
        Cursor cloudCategoryCursor = new MergeCursor(new Cursor[]{cloudCursor1, cloudCursor2});
        mCloudProvider.setQueryResult(cloudCategoryCursor);

        // Local media category cursor with invalid media category type
        Cursor localCursor1 = getMediaCategoriesCursor(
                CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_PEOPLE_AND_PETS);
        // Local media category cursor with valid media category type
        Cursor localCursor2 = getMediaCategoriesCursor(
                CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_DEVICE_FOLDERS);
        Cursor localCategoryCursor = new MergeCursor(new Cursor[]{localCursor1, localCursor2});
        mLocalProvider.setQueryResult(localCategoryCursor);

        try (Cursor cursor = PickerDataLayerV2.queryCategoriesAndAlbums(
                mMockContext,
                getMediaQueryExtras(Long.MAX_VALUE, Long.MAX_VALUE, 100,
                        new ArrayList<>(Arrays.asList(
                                LOCAL_PROVIDER,
                                CloudProviderPrimary.AUTHORITY))),
                /* cancellationSignal */ null,
                mTestConfigStore)) {
            assertWithMessage("Unexpected count of albums and categories")
                    .that(cursor.getCount())
                    .isEqualTo(5);

            cursor.moveToFirst();
            // Assert for Favorites album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_FAVORITES);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(0L);

            cursor.moveToNext();
            // Assert for Camera album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_CAMERA);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(1L);

            cursor.moveToNext();
            // Assert that the next media group is device folders category
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.CATEGORY);
            assertWithMessage("Unexpected group id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_DEVICE_FOLDERS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(2L);

            cursor.moveToNext();
            // Assert that the next media group is user albums category
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.CATEGORY);
            assertWithMessage("Unexpected group id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.MEDIA_CATEGORY_TYPE_USER_ALBUMS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(3L);

            cursor.moveToNext();
            // Assert for Video merged album
            assertWithMessage("Unexpected media group")
                    .that(MediaGroup.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                    PickerSQLConstants.MediaGroupResponseColumns
                                            .MEDIA_GROUP.getColumnName()))))
                    .isEqualTo(MediaGroup.ALBUM);
            assertWithMessage("Unexpected album id")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns.GROUP_ID.getColumnName())))
                    .isEqualTo(CloudMediaProviderContract.AlbumColumns.ALBUM_ID_VIDEOS);
            assertWithMessage("Unexpected picker id")
                    .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaGroupResponseColumns
                                    .PICKER_ID.getColumnName())))
                    .isEqualTo(4L);
        }
    }

    @Test
    public void testQueryMediaInMediaSet_withCloudProvider_localMediaisDisplayedInCloudMediaSet() {
        doReturn(true).when(mMockSyncController).shouldQueryCloudMediaSets(any(), any());

        // Add local media item.
        final Cursor localMediaCursor = getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, localMediaCursor, 1);
        // Add the cloud copy of the same item.
        final Cursor cloudMediaCursor = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cloudMediaCursor, 1);

        // Create a media set for the cloud provider and add the media item to it.
        Long mediaSetPickerId = 1L;

        int cloudRowsInserted = MediaInMediaSetsDatabaseUtil.cacheMediaOfMediaSet(
                mFacade.getDatabase(), List.of(
                        getContentValues(LOCAL_ID_1, CLOUD_ID_1, mediaSetPickerId)
                ), CLOUD_PROVIDER
        );
        assertEquals(
                "Number of rows inserted should be equal to the number of items in the cursor,",
                /*expected*/1,
                /*actual*/cloudRowsInserted);

        // Query for the media set with the cloud provider authority.
        Bundle extras = new Bundle();
        extras.putInt("current_page_size", 100);
        extras.putInt("next_page_size", 100);
        extras.putStringArrayList("providers",
                new ArrayList<>(Arrays.asList(CLOUD_PROVIDER)));
        extras.putString("intent_action", MediaStore.ACTION_PICK_IMAGES);
        extras.putLong(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_PICKER_ID,
                mediaSetPickerId);
        extras.putString(
                MediaInMediaSetSyncRequestParams.KEY_PARENT_MEDIA_SET_AUTHORITY,
                CLOUD_PROVIDER);

        try (Cursor cursor =
                     PickerDataLayerV2.queryMediaInMediaSet(mMockContext, extras)) {
            // Assertion: Verify the results.
            assertWithMessage("Cursor should not be null")
                    .that(cursor)
                    .isNotNull();

            assertWithMessage("Cursor count is not as expected")
                    .that(cursor.getCount())
                    .isEqualTo(1);

            cursor.moveToFirst();
            assertWithMessage("Media ID is not as expected in the media set results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.MEDIA_ID.getProjectedName())))
                    .isEqualTo(LOCAL_ID_1);

            assertWithMessage("Authority is not as expected in the media set results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.AUTHORITY.getProjectedName())))
                    .isEqualTo(LOCAL_PROVIDER);
        }
    }

    @Test
    public void testQuerySearchMedia_withBothProviders_localCopyOfCloudMediaIsDisplayed() {
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        // Add local media item.
        final Cursor localMediaCursor = getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, localMediaCursor, 1);
        // Add the cloud copy of the same item.
        final Cursor cloudMediaCursor = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cloudMediaCursor, 1);

        final int searchRequestId = 1;

        // Cache search results from cloud provider only, pointing to the cloud ID.
        SearchResultsDatabaseUtil.cacheSearchResults(
                mFacade.getDatabase(), CLOUD_PROVIDER, List.of(
                        getSearchContentValues(LOCAL_ID_1, CLOUD_ID_1, searchRequestId)
                ), /* cancellationSignal */ null);

        // Query for the search results.
        Bundle extras = new Bundle();
        extras.putInt("current_page_size", 100);
        extras.putInt("next_page_size", 100);
        extras.putStringArrayList("providers",
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER, CLOUD_PROVIDER)));
        extras.putString("intent_action", MediaStore.ACTION_PICK_IMAGES);

        try (Cursor cursor =
                     PickerDataLayerV2.querySearchMedia(mMockContext, extras, searchRequestId)) {

            assertWithMessage("Cursor should not be null")
                    .that(cursor)
                    .isNotNull();

            assertWithMessage("Cursor count is not as expected")
                    .that(cursor.getCount())
                    .isEqualTo(1);

            cursor.moveToFirst();
            assertWithMessage("Media ID is not as expected in the search results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.MEDIA_ID.getProjectedName())))
                    .isEqualTo(LOCAL_ID_1);

            assertWithMessage("Authority is not as expected in the search results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.AUTHORITY.getProjectedName())))
                    .isEqualTo(LOCAL_PROVIDER);
        }
    }

    @Test
    public void testQuerySearchMedia_withCloudProvider_cloudOnlyMediaIsDisplayed() {
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(true).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        // Add local media item.
        final Cursor localMediaCursor = getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, localMediaCursor, 1);
        // Add the cloud copy of the same item.
        final Cursor cloudMediaCursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cloudMediaCursor1, 1);
        // Add cloud media item.
        final Cursor cloudMediaCursor2 = getCloudMediaCursor(CLOUD_ID_2, null, DATE_TAKEN_MS_1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cloudMediaCursor2, 1);

        final int searchRequestId = 1;

        // Cache search results from cloud provider only.
        SearchResultsDatabaseUtil.cacheSearchResults(
                mFacade.getDatabase(), CLOUD_PROVIDER, List.of(
                        getSearchContentValues(LOCAL_ID_1, CLOUD_ID_1, searchRequestId),
                        getSearchContentValues(null, CLOUD_ID_2, searchRequestId)
                ), /* cancellationSignal */ null);

        // Query for the search results.
        Bundle extras = new Bundle();
        extras.putInt("current_page_size", 100);
        extras.putInt("next_page_size", 100);
        // Add only the cloud provider in the list of available provider
        extras.putStringArrayList("providers",
                new ArrayList<>(Arrays.asList(CLOUD_PROVIDER)));
        extras.putString("intent_action", MediaStore.ACTION_PICK_IMAGES);

        try (Cursor cursor =
                     PickerDataLayerV2.querySearchMedia(mMockContext, extras, searchRequestId)) {

            assertWithMessage("Cursor should not be null")
                    .that(cursor)
                    .isNotNull();

            assertWithMessage("Cursor count is not as expected")
                    .that(cursor.getCount())
                    .isEqualTo(1);

            cursor.moveToFirst();
            assertWithMessage("Media ID is not as expected in the search results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.MEDIA_ID.getProjectedName())))
                    .isEqualTo(CLOUD_ID_2);

            assertWithMessage("Authority is not as expected in the search results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.AUTHORITY.getProjectedName())))
                    .isEqualTo(CLOUD_PROVIDER);
        }
    }

    @Test
    public void testQuerySearchMedia_withLocalProvider_localOnlyMediaIsDisplayed() {
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any());
        doReturn(false).when(mMockSyncController).shouldQueryCloudMedia(any(), any());

        // Add local media item.
        final Cursor localMediaCursor = getLocalMediaCursor(LOCAL_ID_1, DATE_TAKEN_MS);
        assertAddMediaOperation(mFacade, LOCAL_PROVIDER, localMediaCursor, 1);
        // Add the cloud copy of the same item.
        final Cursor cloudMediaCursor1 = getCloudMediaCursor(CLOUD_ID_1, LOCAL_ID_1, DATE_TAKEN_MS);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cloudMediaCursor1, 1);
        // Add cloud media item.
        final Cursor cloudMediaCursor2 = getCloudMediaCursor(CLOUD_ID_2, null, DATE_TAKEN_MS_1);
        assertAddMediaOperation(mFacade, CLOUD_PROVIDER, cloudMediaCursor2, 1);

        final int searchRequestId = 1;

        // Cache all search results.
        SearchResultsDatabaseUtil.cacheSearchResults(
                mFacade.getDatabase(), CLOUD_PROVIDER, List.of(
                        getSearchContentValues(LOCAL_ID_1, null, searchRequestId),
                        getSearchContentValues(LOCAL_ID_1, CLOUD_ID_1, searchRequestId),
                        getSearchContentValues(null, CLOUD_ID_2, searchRequestId)
                ), /* cancellationSignal */ null);

        // Query for the search results.
        Bundle extras = new Bundle();
        extras.putInt("current_page_size", 100);
        extras.putInt("next_page_size", 100);
        // Add only the local provider in the list of available provider
        extras.putStringArrayList("providers",
                new ArrayList<>(Arrays.asList(LOCAL_PROVIDER)));
        extras.putString("intent_action", MediaStore.ACTION_PICK_IMAGES);

        try (Cursor cursor =
                     PickerDataLayerV2.querySearchMedia(mMockContext, extras, searchRequestId)) {

            assertWithMessage("Cursor should not be null")
                    .that(cursor)
                    .isNotNull();

            assertWithMessage("Cursor count is not as expected")
                    .that(cursor.getCount())
                    .isEqualTo(1);

            cursor.moveToFirst();
            assertWithMessage("Media ID is not as expected in the search results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.MEDIA_ID.getProjectedName())))
                    .isEqualTo(LOCAL_ID_1);

            assertWithMessage("Authority is not as expected in the search results")
                    .that(cursor.getString(cursor.getColumnIndexOrThrow(
                            PickerSQLConstants.MediaResponse.AUTHORITY.getProjectedName())))
                    .isEqualTo(LOCAL_PROVIDER);
        }
    }

    private static Bundle getCreateSearchRequestExtras(SearchTextRequest searchTextRequest) {
        final Bundle bundle = new Bundle();
        bundle.putString("search_text", searchTextRequest.getSearchText());
        bundle.putStringArrayList("providers", new ArrayList<>(List.of(SearchProvider.AUTHORITY)));
        return bundle;
    }

    private static Bundle getCreateSearchSuggestionRequestExtras(
            SearchSuggestionRequest searchTextRequest) {
        final Bundle bundle = new Bundle();
        bundle.putString("search_text", searchTextRequest.getSearchSuggestion().getSearchText());
        bundle.putString("media_set_id", searchTextRequest.getSearchSuggestion().getMediaSetId());
        bundle.putStringArrayList("providers", new ArrayList<>(List.of(SearchProvider.AUTHORITY)));
        bundle.putString("authority", searchTextRequest.getSearchSuggestion().getAuthority());
        bundle.putString("search_suggestion_type",
                searchTextRequest.getSearchSuggestion().getSearchSuggestionType());
        return bundle;
    }

    private static void assertMediaCursor(Cursor cursor, String id, String authority,
            Long dateTaken, String mimeType) {
        assertMediaCursor(cursor, id, authority, dateTaken, mimeType,
                MediaStore.ACTION_PICK_IMAGES, /* isPreGranted */ false);
    }
    private static void assertMediaCursor(Cursor cursor, String id, String authority,
            Long dateTaken, String mimeType, String intent) {
        assertMediaCursor(cursor, id, authority, dateTaken, mimeType,
                intent, /* isPreGranted */ false);
    }

    private static void assertMediaCursor(Cursor cursor, String id, String authority,
            Long dateTaken, String mimeType, String intent, boolean isPreGranted) {
        assertWithMessage("Unexpected value of id in the media cursor.")
                .that(cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.MEDIA_ID.getProjectedName())))
                .isEqualTo(id);

        assertWithMessage("Unexpected value of authority in the media cursor.")
                .that(cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.AUTHORITY.getProjectedName())))
                .isEqualTo(authority);

        assertWithMessage("Unexpected value of date taken in the media cursor.")
                .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.DATE_TAKEN_MS.getProjectedName())))
                .isEqualTo(dateTaken);

        assertWithMessage("Unexpected value of mime type in the media cursor.")
                .that(cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.MIME_TYPE.getProjectedName())))
                .isEqualTo(mimeType);

        final Uri expectedUri = getMediaUri(id, authority, intent);

        assertWithMessage("Unexpected value of uri in the media cursor.")
                .that(cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.WRAPPED_URI.getProjectedName())))
                .isEqualTo(expectedUri.toString());

        assertWithMessage("Unexpected value of grants in the media cursor.")
                .that(cursor.getInt(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.IS_PRE_GRANTED.getProjectedName())))
                .isEqualTo(isPreGranted ? 1 : 0);
    }

    private static void assertItemsPerMonthCursor(
            Cursor cursor, int year, int month, int itemsCount) {
        assertWithMessage("Unexpected year value in the Items per month cursor.")
                .that(Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.ItemsPerMonthResponse.YEAR_TAKEN.getProjectedName()))))
                .isEqualTo(year);
        assertWithMessage("Unexpected month value in the Items per month cursor.")
                .that(Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.ItemsPerMonthResponse.MONTH_TAKEN.getProjectedName()))))
                .isEqualTo(month);
        assertWithMessage("Unexpected value of items count in the Items per month cursor.")
                .that(cursor.getInt(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.ItemsPerMonthResponse.ITEM_COUNT.getProjectedName())))
                .isEqualTo(itemsCount);
    }

    private static void assertMediaPageKeyCursor(Cursor cursor, Long pickerId, Long dateTaken) {
        assertWithMessage("Unexpected value of id in the media page key cursor.")
                .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.PICKER_ID.getProjectedName())))
                .isEqualTo(pickerId);

        assertWithMessage("Unexpected value of date taken in the media page key cursor.")
                .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.DATE_TAKEN_MS.getProjectedName())))
                .isEqualTo(dateTaken);
    }

    private static void assertMediaPageKeyListCursor(Cursor cursor, Long pickerId, Long dateTaken) {
        assertWithMessage("Unexpected value of id in the media page key cursor.")
                .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.PICKER_ID.getProjectedName())))
                .isEqualTo(pickerId);

        assertWithMessage("Unexpected value of date taken in the media page key cursor.")
                .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.MediaResponse.DATE_TAKEN_MS.getProjectedName())))
                .isEqualTo(dateTaken);
    }

    private static void assertAlbumCursor(Cursor cursor, String albumId, String authority,
            Long dateTaken, String coverMediaId) {
        final MediaSource mediaSource = LOCAL_PROVIDER.equals(authority)
                ? MediaSource.LOCAL
                : MediaSource.REMOTE;
        assertAlbumCursor(cursor, albumId, authority, dateTaken, coverMediaId, mediaSource);
    }

    private static void assertAlbumCursor(Cursor cursor, String albumId, String authority,
            Long dateTaken, String coverMediaId, MediaSource mediaSource) {
        assertWithMessage("Unexpected value of id in the media cursor.")
                .that(cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.AlbumResponse.ALBUM_ID.getColumnName())))
                .isEqualTo(albumId);

        assertWithMessage("Unexpected value of authority in the media cursor.")
                .that(cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.AlbumResponse.AUTHORITY.getColumnName())))
                .isEqualTo(authority);

        assertWithMessage("Unexpected value of date taken in the media cursor.")
                .that(cursor.getLong(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.AlbumResponse.DATE_TAKEN.getColumnName())))
                .isEqualTo(dateTaken);

        final Uri coverUri = Uri.parse(
                cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.AlbumResponse.UNWRAPPED_COVER_URI.getColumnName()))
        );

        if (EMPTY_MEDIA_ID.equals(coverMediaId)) {
            assertWithMessage("Unexpected value of cover uri.")
                    .that(coverUri)
                    .isEqualTo(Uri.EMPTY);
        } else {
            assertWithMessage("Unexpected value of cover media id in the media cursor.")
                    .that(coverUri.getLastPathSegment())
                    .isEqualTo(coverMediaId);
        }

        assertWithMessage("Unexpected value of media source in the media cursor.")
                .that(MediaSource.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(
                        PickerSQLConstants.AlbumResponse.COVER_MEDIA_SOURCE.getColumnName()))))
                .isEqualTo(mediaSource);
    }

    private static Uri getMediaUri(String id, String authority, String intent) {
        return PickerUriResolver.wrapProviderUri(
                ItemsProvider.getItemsUri(id, authority, UserId.CURRENT_USER),
                intent,
                MediaStore.MY_USER_ID
        );
    }

    private Bundle getMediaQueryExtras(Long pickerId, Long dateTakenMillis, int pageSize,
            List<String> providers) {
        Bundle extras = new Bundle();
        extras.putLong("picker_id", pickerId);
        extras.putLong("date_taken_millis", dateTakenMillis);
        extras.putInt("current_page_size", pageSize);
        extras.putInt("next_page_size", pageSize);
        extras.putStringArrayList("providers", new ArrayList<>(providers));
        extras.putString("intent_action", MediaStore.ACTION_PICK_IMAGES);
        extras.putBoolean("enable_items_before_count", true);
        extras.putBoolean("enable_items_after_count", true);
        return extras;
    }

    private Bundle getItemsPerMonthQueryExtras(List<String> providers) {
        Bundle extras = new Bundle();
        extras.putStringArrayList("providers", new ArrayList<>(providers));
        extras.putString("intent_action", MediaStore.ACTION_PICK_IMAGES);
        return extras;
    }

    private Bundle getMediaPageKeyQueryExtras(List<String> providers, int itemPosition) {
        Bundle extras = new Bundle();
        extras.putStringArrayList("providers", new ArrayList<>(providers));
        extras.putString("intent_action", MediaStore.ACTION_PICK_IMAGES);
        extras.putInt("item_position", itemPosition);
        return extras;
    }

    private Bundle getMediaPageKeyQueryExtras(
            List<String> providers, int itemPosition, List<String> mimeTypes) {
        Bundle extras = getMediaPageKeyQueryExtras(providers, itemPosition);
        extras.putStringArrayList("mime_types", new ArrayList<>(mimeTypes));
        return extras;
    }

    private Bundle getMediaPageKeyListQueryExtras(List<String> providers, int itemIndexInterval) {
        Bundle extras = new Bundle();
        extras.putStringArrayList("providers", new ArrayList<>(providers));
        extras.putString("intent_action", MediaStore.ACTION_PICK_IMAGES);
        extras.putInt("item_index_interval", itemIndexInterval);
        return extras;
    }

    private Bundle getMediaPageKeyListQueryExtras(
            List<String> providers, int itemIndexInterval, List<String> mimeTypes) {
        Bundle extras = getMediaPageKeyListQueryExtras(providers, itemIndexInterval);
        extras.putStringArrayList("mime_types", new ArrayList<>(mimeTypes));
        return extras;
    }

    private Bundle getItemsPerMonthQueryExtras(List<String> providers, List<String> mimeTypes) {
        Bundle extras = getItemsPerMonthQueryExtras(providers);
        extras.putStringArrayList("mime_types", new ArrayList<>(mimeTypes));
        return extras;
    }

    private Bundle getMediaQueryExtras(Long pickerId, Long dateTakenMillis, int pageSize,
            List<String> providers, List<String> mimeTypes) {
        Bundle extras = getMediaQueryExtras(
                pickerId,
                dateTakenMillis,
                pageSize,
                providers
        );
        extras.putStringArrayList("mime_types", new ArrayList<>(mimeTypes));
        return extras;
    }

    private Bundle getMediaQueryExtras(
            Long pickerId, Long dateTakenMillis, int pageSize,
            List<String> providers, List<String> mimeTypes,
            String intentAction, int callingUid) {
        Bundle extras = getMediaQueryExtras(
                pickerId,
                dateTakenMillis,
                pageSize,
                providers,
                mimeTypes
        );
        extras.putInt(Intent.EXTRA_UID, callingUid);
        extras.putString("intent_action", intentAction);
        return extras;
    }

    private Bundle getAlbumMediaQueryExtras(Long pickerId, Long dateTakenMillis, int pageSize,
            List<String> providers, String albumAuthority) {
        Bundle extras = getMediaQueryExtras(
                pickerId,
                dateTakenMillis,
                pageSize,
                providers
        );
        extras.putString("album_authority", albumAuthority);
        return extras;
    }

    private ContentValues getContentValues(
            String localId, String cloudId, Long mediaSetPickerId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(
                PickerSQLConstants.MediaInMediaSetsTableColumns.CLOUD_ID.getColumnName(), cloudId);
        contentValues.put(
                PickerSQLConstants.MediaInMediaSetsTableColumns.LOCAL_ID.getColumnName(), localId);
        contentValues.put(
                PickerSQLConstants.MediaInMediaSetsTableColumns.MEDIA_SETS_PICKER_ID
                        .getColumnName(),
                mediaSetPickerId);
        return contentValues;
    }

    private ContentValues getSearchContentValues(String localId, String cloudId,
            int searchRequestId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(
                PickerSQLConstants.SearchResultMediaTableColumns.CLOUD_ID.getColumnName(), cloudId);
        contentValues.put(
                PickerSQLConstants.SearchResultMediaTableColumns.LOCAL_ID.getColumnName(), localId);
        contentValues.put(
                PickerSQLConstants.SearchResultMediaTableColumns.SEARCH_REQUEST_ID.getColumnName(),
                searchRequestId);
        return contentValues;
    }

    private int setupAndGetTestUid() {
        // testUid should be selected such that the userId computed from this uid later in the code
        // flow matches the current userId. UserId is computed using
        // PickerSyncController#uidToUser() where the userId = uid / PER_USER_RANGE.
        // So testUid is =
        // (a random number smaller than PER_USER_RANGE) + (PER_USER_RANGE * UserHandle.myUserId())
        int testUid = 11 + (PER_USER_RANGE * UserHandle.myUserId());
        doReturn(mMockPackageManager)
                .when(mMockContext).getPackageManager();
        String[] packageNames = new String[]{TEST_PACKAGE_NAME};
        doReturn(packageNames).when(mMockPackageManager).getPackagesForUid(testUid);
        // insert a grant for the second item inserted in media.
        assertInsertGrantsOperation(mFacade, getMediaGrantsCursor(LOCAL_ID_2), /* writeCount */1);
        return testUid;
    }
}
