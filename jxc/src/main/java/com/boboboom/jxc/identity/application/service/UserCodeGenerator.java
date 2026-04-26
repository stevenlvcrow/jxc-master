package com.boboboom.jxc.identity.application.service;

import java.util.Locale;

import org.springframework.stereotype.Component;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/** 身份与权限生成器，负责用户编码生成器生成。 */
@Component
public class UserCodeGenerator {

    private static final int PHONE_SUFFIX_LENGTH = 4;
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = buildFormat();

    /** 生成业务编码。 */
    public String generate(String realName, String phone) {
        String initials = buildInitials(realName);
        String suffix = buildPhoneSuffix(phone);
        return (initials + suffix).toLowerCase(Locale.ROOT);
    }

    private String buildInitials(String realName) {
        String normalized = realName == null ? "" : realName.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if (isAsciiLetterOrDigit(ch)) {
                builder.append(Character.toLowerCase(ch));
                continue;
            }
            String initial = resolveChineseInitial(ch);
            if (!initial.isEmpty()) {
                builder.append(initial);
            }
        }
        return builder.toString();
    }

    private String resolveChineseInitial(char ch) {
        try {
            String[] values = PinyinHelper.toHanyuPinyinStringArray(ch, PINYIN_FORMAT);
            if (values == null || values.length == 0 || values[0] == null || values[0].isBlank()) {
                return "";
            }
            return values[0].substring(0, 1).toLowerCase(Locale.ROOT);
        } catch (BadHanyuPinyinOutputFormatCombination ex) {
            return "";
        }
    }

    private String buildPhoneSuffix(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        if (normalized.length() <= PHONE_SUFFIX_LENGTH) {
            return normalized;
        }
        return normalized.substring(normalized.length() - PHONE_SUFFIX_LENGTH);
    }

    private boolean isAsciiLetterOrDigit(char ch) {
        return (ch >= '0' && ch <= '9')
                || (ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z');
    }

    private static HanyuPinyinOutputFormat buildFormat() {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        return format;
    }
}
