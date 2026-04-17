package com.boboboom.jxc.common;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BusinessCodeGenerator {

    private static final int DEFAULT_SERIAL_LENGTH = 6;

    public CodeAllocator allocator(String prefix, Collection<String> existingCodes) {
        return new CodeAllocator(normalizePrefix(prefix), existingCodes, DEFAULT_SERIAL_LENGTH);
    }

    public String nextCode(String prefix, Collection<String> existingCodes) {
        return allocator(prefix, existingCodes).nextCode();
    }

    public static final class CodeAllocator {
        private final String prefix;
        private final int serialLength;
        private final Set<String> usedCodes;
        private int nextSerial;

        private CodeAllocator(String prefix, Collection<String> existingCodes, int serialLength) {
            this.prefix = prefix;
            this.serialLength = serialLength;
            this.usedCodes = new LinkedHashSet<>();
            int maxSerial = 0;
            Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix) + "(\\d+)$");
            if (existingCodes != null) {
                for (String rawCode : existingCodes) {
                    if (rawCode == null || rawCode.isBlank()) {
                        continue;
                    }
                    String code = rawCode.trim().toUpperCase(Locale.ROOT);
                    usedCodes.add(code);
                    Matcher matcher = pattern.matcher(code);
                    if (!matcher.matches()) {
                        continue;
                    }
                    int serial = parseSerial(matcher.group(1));
                    if (serial > maxSerial) {
                        maxSerial = serial;
                    }
                }
            }
            this.nextSerial = maxSerial + 1;
        }

        public String nextCode() {
            while (true) {
                String candidate = prefix + String.format(Locale.ROOT, "%0" + serialLength + "d", nextSerial++);
                if (usedCodes.add(candidate)) {
                    return candidate;
                }
            }
        }

        private int parseSerial(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("编码前缀不能为空");
        }
        String normalized = prefix.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 4) {
            throw new IllegalArgumentException("编码前缀必须为4位大写字母");
        }
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch < 'A' || ch > 'Z') {
                throw new IllegalArgumentException("编码前缀必须为4位大写字母");
            }
        }
        return normalized;
    }
}
