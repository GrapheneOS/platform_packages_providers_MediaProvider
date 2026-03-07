package com.android.providers.media;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.ext.micspoofing.MicSpoofingApi;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class MediaProviderMicSpoofingOpenSourceTest {

    private static final byte[] TEST_AUDIO_BYTES = "test_wav_payload".getBytes(
            StandardCharsets.UTF_8);

    private Context context;
    private String packageName;
    private int userId;
    @Nullable
    private File sourceFile;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        packageName = context.getPackageName();
        userId = UserHandle.myUserId();
        clearMicSpoofingState();
    }

    @After
    public void tearDown() throws Exception {
        clearMicSpoofingState();
        if (sourceFile != null) {
            sourceFile.delete();
            sourceFile = null;
        }
    }

    @Test
    public void openSource_writeMode_throwsSecurityException() throws Exception {
        sourceFile = stageSourceFile();
        setMicSpoofingState(sourceFile.getAbsolutePath(), true);

        assertThrows(SecurityException.class, () -> openSource("rw"));
    }

    @Test
    public void openSource_spoofingDisabled_throwsFileNotFoundException() throws Exception {
        sourceFile = stageSourceFile();
        setMicSpoofingState(sourceFile.getAbsolutePath(), false);

        assertThrows(FileNotFoundException.class, () -> openSource("r"));
    }

    @Test
    public void openSource_missingFile_throwsFileNotFoundException() throws Exception {
        File missing = new File(
                context.getExternalFilesDir(null),
                "missing_mic_spoofing_source_" + System.nanoTime() + ".wav"
        );
        setMicSpoofingState(missing.getAbsolutePath(), true);

        assertThrows(FileNotFoundException.class, () -> openSource("r"));
    }

    @Test
    public void openSource_validCustomPath_returnsReadableFd() throws Exception {
        sourceFile = stageSourceFile();
        setMicSpoofingState(sourceFile.getAbsolutePath(), true);

        ParcelFileDescriptor parcelFileDescriptor = openSource("r");
        assertThat(parcelFileDescriptor).isNotNull();

        try (ParcelFileDescriptor.AutoCloseInputStream stream =
                     new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor)) {
            assertThat(stream.readAllBytes()).isEqualTo(TEST_AUDIO_BYTES);
        }
    }

    private void clearMicSpoofingState() throws IOException {
        runShellCommand("pm edit-gos-package-state " + packageName + " " + userId
                + " clear-flag MIC_SPOOFING_ENABLED");
        runShellCommand("pm edit-gos-package-state " + packageName + " " + userId
                + " set-mic-spoofing-config null");
    }

    private void setMicSpoofingState(@Nullable String path, boolean enabled) throws IOException {
        String configHex = path == null
                ? "null"
                : bytesToHex(MicSpoofingApi.buildCustomPathConfig(path));

        runShellCommand("pm edit-gos-package-state " + packageName + " " + userId
                + " set-mic-spoofing-config " + configHex);
        runShellCommand("pm edit-gos-package-state " + packageName + " " + userId + " "
                + (enabled ? "add-flag" : "clear-flag") + " MIC_SPOOFING_ENABLED");
    }

    @Nullable
    private ParcelFileDescriptor openSource(@NonNull String mode) throws IOException {
        return context.getContentResolver().openFileDescriptor(
                android.net.Uri.parse("content://media/mic_spoofing_source"),
                mode);
    }

    private File stageSourceFile() throws IOException {
        File parent = context.getExternalFilesDir(null);
        assertThat(parent).isNotNull();

        File file = new File(parent, "mic_spoofing_open_source_test_" + System.nanoTime()
                + ".wav");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(TEST_AUDIO_BYTES);
        }
        return file;
    }

    private static void runShellCommand(@NonNull String command) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .executeShellCommand(command);
        try (ParcelFileDescriptor.AutoCloseInputStream stream =
                     new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor)) {
            stream.readAllBytes();
        }
    }

    private static String bytesToHex(@NonNull byte[] bytes) {
        char[] hex = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            out[i * 2] = hex[value >>> 4];
            out[i * 2 + 1] = hex[value & 0x0F];
        }
        return new String(out);
    }
}
