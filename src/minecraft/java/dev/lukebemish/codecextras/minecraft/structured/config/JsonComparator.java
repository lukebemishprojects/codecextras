package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

class JsonComparator implements Comparator<JsonElement> {
    public static final JsonComparator INSTANCE = new JsonComparator();

    private JsonComparator() {}

    @Override
    public int compare(JsonElement o1, JsonElement o2) {
        if (o1.isJsonPrimitive() && o2.isJsonPrimitive()) {
            var p1 = o1.getAsJsonPrimitive();
            var p2 = o2.getAsJsonPrimitive();
            if (p1.isString() || p2.isString()) {
                return p1.getAsString().compareTo(p2.getAsString());
            } else if (p1.isNumber() || p2.isNumber()) {
                BigDecimal p1d = p1.isNumber() ? p1.getAsBigDecimal() : p1.getAsBoolean() ? BigDecimal.ONE : BigDecimal.ZERO;
                BigDecimal p2d = p2.isNumber() ? p2.getAsBigDecimal() : p2.getAsBoolean() ? BigDecimal.ONE : BigDecimal.ZERO;
                return p1d.compareTo(p2d);
            } else {
                return Boolean.compare(p1.getAsBoolean(), p2.getAsBoolean());
            }
        } else if (o1.isJsonArray()) {
            if (!o2.isJsonArray()) {
                return 1;
            }
            var a1 = o1.getAsJsonArray();
            var a2 = o2.getAsJsonArray();
            int size = Math.min(a1.size(), a2.size());
            for (int i = 0; i < size; i++) {
                int cmp = compare(a1.get(i), a2.get(i));
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(a1.size(), a2.size());
        } else if (o1.isJsonObject()) {
            if (!o2.isJsonObject()) {
                return 1;
            }
            var o1e = o1.getAsJsonObject().entrySet().stream().sorted(Map.Entry.comparingByKey()).iterator();
            var o2e = o2.getAsJsonObject().entrySet().stream().sorted(Map.Entry.comparingByKey()).iterator();
            while (o1e.hasNext() && o2e.hasNext()) {
                var e1 = o1e.next();
                var e2 = o2e.next();
                int cmp = e1.getKey().compareTo(e2.getKey());
                if (cmp != 0) {
                    return cmp;
                }
                cmp = compare(e1.getValue(), e2.getValue());
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Boolean.compare(o1e.hasNext(), o2e.hasNext());
        } else {
            return 0;
        }
    }
}
