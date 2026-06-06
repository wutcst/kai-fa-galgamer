package cn.edu.whut.sept.zuul.save;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveServiceSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void savePathShouldStayInsideBaseDirectoryAfterNormalization() {
        SaveService saveService = new SaveService(tempDir);

        Path path = saveService.resolveSavePath("../../../etc/passwd");

        assertTrue(path.startsWith(tempDir.toAbsolutePath().normalize()));
        assertTrue(path.getFileName().toString().endsWith("etc_passwd.json"));
    }

    @Test
    void profileShouldNotBeDeletedThroughSaveDelete() {
        SaveService saveService = new SaveService(tempDir);

        try {
            saveService.delete("profile");
        } catch (IllegalArgumentException ex) {
            assertEquals("profile.json 不能通过普通存档接口删除。", ex.getMessage());
        }
    }
}
