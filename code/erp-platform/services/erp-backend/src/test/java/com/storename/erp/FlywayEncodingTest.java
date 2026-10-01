package com.storename.erp;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mọi file migration Flyway phải là UTF-8 không BOM.
 *
 * <p>Lịch sử: một số file từng bị lưu UTF-16 hoặc UTF-8 có BOM trên Windows, làm Flyway đọc sai
 * và đổi checksum. Test quét toàn bộ {@code db/migration*} thay vì liệt kê tên file cố định.</p>
 */
public class FlywayEncodingTest {

    @Test
    public void allMigrationFilesAreUtf8WithoutBom() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration*/*.sql");
        assertTrue(resources.length > 0, "Không tìm thấy file migration nào trong db/migration*");

        List<String> problems = new ArrayList<>();
        for (Resource resource : resources) {
            byte[] bytes;
            try (InputStream is = resource.getInputStream()) {
                bytes = is.readAllBytes();
            }
            String name = resource.getFilename();
            if (bytes.length >= 2 && ((bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)
                    || (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF))) {
                problems.add(name + ": UTF-16 BOM");
            }
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                problems.add(name + ": UTF-8 BOM");
            }
            int nullBytes = 0;
            for (byte b : bytes) {
                if (b == 0) nullBytes++;
            }
            if (nullBytes > 0) {
                problems.add(name + ": chứa " + nullBytes + " byte NUL (có thể là UTF-16 không BOM)");
            }
        }
        assertFalse(!problems.isEmpty(), "Migration sai encoding: " + problems);
    }
}
