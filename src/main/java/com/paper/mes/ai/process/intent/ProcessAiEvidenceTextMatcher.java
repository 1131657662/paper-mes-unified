package com.paper.mes.ai.process.intent;

import java.util.regex.Pattern;

/** Matches evidence fragments without letting one numeric value match part of another. */
public final class ProcessAiEvidenceTextMatcher {

    private static final Pattern NUMERIC = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?");

    private ProcessAiEvidenceTextMatcher() {
    }

    public static boolean contains(String text, String fragment) {
        if (text == null || fragment == null || fragment.isBlank()) return false;
        if (!NUMERIC.matcher(fragment).matches()) return text.contains(fragment);
        Pattern token = Pattern.compile("(?<![0-9.])" + Pattern.quote(fragment)
                + "(?![0-9.])");
        return token.matcher(text).find();
    }
}
