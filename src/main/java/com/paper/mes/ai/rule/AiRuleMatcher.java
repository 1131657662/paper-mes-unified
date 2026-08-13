package com.paper.mes.ai.rule;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class AiRuleMatcher {

    public List<AiRule> match(String question, List<AiRule> rules) {
        return match(question, null, rules);
    }

    public List<AiRule> match(String question, String pageTemplate, List<AiRule> rules) {
        String normalized = question.toLowerCase(Locale.ROOT).trim();
        return rules.stream()
                .filter(rule -> "ACTIVE".equals(rule.status()))
                .map(rule -> new ScoredRule(rule, score(rule, normalized, pageTemplate)))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredRule::score).reversed()
                        .thenComparing(item -> item.rule().ruleId()))
                .limit(3)
                .map(ScoredRule::rule)
                .toList();
    }

    private boolean moduleMatches(String module, String pageTemplate) {
        if (module == null || module.isBlank() || "*".equals(module) || pageTemplate == null || pageTemplate.isBlank()) {
            return true;
        }
        String normalizedModule = module.toLowerCase(Locale.ROOT);
        String normalizedPage = pageTemplate.toLowerCase(Locale.ROOT);
        return normalizedModule.equals(normalizedPage)
                || (normalizedModule + "s").equals(normalizedPage)
                || (normalizedPage + "s").equals(normalizedModule);
    }

    private int score(AiRule rule, String question, String pageTemplate) {
        int score = contains(question, rule.ruleId()) ? 12 : 0;
        score += contains(question, rule.title()) ? 8 : 0;
        score += rule.keywords().stream().mapToInt(keyword -> contains(question, keyword) ? 4 : 0).sum();
        if (score == 0) {
            return 0;
        }
        score += moduleMatches(rule.module(), pageTemplate) ? 2 : 0;
        return score;
    }

    private boolean contains(String text, String term) {
        return term != null && !term.isBlank() && text.contains(term.toLowerCase(Locale.ROOT));
    }

    private record ScoredRule(AiRule rule, int score) {
    }
}
