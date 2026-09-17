package com.storename.erp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

public class FlywayEncodingTest {

    @Test
    public void testMigrationFilesAreNotUtf16() throws Exception {
        String[] migrationsToCheck = {
            "db/migration/V16__add_branch_id_to_customer.sql",
            "db/migration/V17__backfill_opening_balance.sql"
        };
        
        for (String path : migrationsToCheck) {
            ClassPathResource resource = new ClassPathResource(path);
            Assertions.assertTrue(resource.exists(), "Migration file " + path + " must exist");
            
            try (InputStream is = resource.getInputStream()) {
                byte[] bytes = new byte[2];
                int read = is.read(bytes);
                if (read == 2) {
                    // Check for UTF-16 LE or BE BOM
                    boolean isUtf16LE = (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE);
                    boolean isUtf16BE = (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF);
                    Assertions.assertFalse(isUtf16LE, "File " + path + " must not have UTF-16 LE BOM");
                    Assertions.assertFalse(isUtf16BE, "File " + path + " must not have UTF-16 BE BOM");
                    
                    // Additionally check if every second byte is a null byte (typical for UTF-16 ascii files)
                    byte[] content = is.readAllBytes();
                    int nullByteCount = 0;
                    for (byte b : content) {
                        if (b == 0) nullByteCount++;
                    }
                    // A normal SQL file shouldn't have many null bytes unless it's UTF-16
                    Assertions.assertTrue(nullByteCount < content.length / 4, 
                        "File " + path + " contains too many null bytes, possibly UTF-16 encoded without BOM");
                }
            }
        }
    }
}
