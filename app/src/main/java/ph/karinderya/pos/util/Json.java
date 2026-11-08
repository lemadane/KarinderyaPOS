package ph.karinderya.pos.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared Jackson mapper. Auto-detection is disabled so that only explicitly annotated record
 * components are mapped; this keeps behaviour identical on the JVM and on Android, where records
 * are desugared and are not recognised as {@code java.lang.Record} on older API levels.
 */
public final class Json {

     static final ObjectMapper MAPPER = JsonMapper.builder()
            .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

     Json() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
