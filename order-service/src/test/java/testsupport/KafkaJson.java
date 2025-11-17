package testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class KafkaJson {
    private static final ObjectMapper OM = new ObjectMapper();
    public static String toJson(Object o) {
        try { return OM.writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}