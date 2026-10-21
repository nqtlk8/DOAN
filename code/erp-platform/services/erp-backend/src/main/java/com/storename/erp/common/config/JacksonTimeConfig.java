package com.storename.erp.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Ghi {@link LocalDateTime} ra JSON kèm độ lệch múi giờ của máy chủ (ví dụ {@code 2026-10-13T09:52:00Z}).
 *
 * <p>Các entity dùng {@code LocalDateTime} (giờ máy chủ, trong container là UTC) còn sổ công nợ dùng
 * {@code ZonedDateTime}. Trước đây {@code LocalDateTime} được ghi không có độ lệch nên trình duyệt hiểu
 * là giờ địa phương: cùng một hóa đơn hiện 09:52 ở danh sách phiếu nhưng 16:52 ở sao kê. Thêm độ lệch
 * giúp mọi màn hình quy đổi về cùng một giờ địa phương.</p>
 */
@Configuration
public class JacksonTimeConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeWithOffset() {
        return builder -> builder.serializerByType(LocalDateTime.class, new LocalDateTimeWithOffsetSerializer());
    }

    static class LocalDateTimeWithOffsetSerializer extends StdSerializer<LocalDateTime> {
        LocalDateTimeWithOffsetSerializer() {
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.atZone(ZoneId.systemDefault()).toOffsetDateTime()
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }
}
