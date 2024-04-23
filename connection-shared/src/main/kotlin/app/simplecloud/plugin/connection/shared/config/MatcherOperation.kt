package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.connection.shared.matcher.*

enum class MatcherOperation(
    val matcher: OperationMatcher
) {

    REGEX(RegexOperationMatcher),
    EQUALS(EqualsOperationMatcher),
    CONTAINS(ContainsOperationMatcher),
    STARTS_WITH(StartsWithOperationMatcher),
    ENDS_WITH(EndsWithOperationMatcher);

    fun matches(name: String, value: String, negate: Boolean): Boolean {
        val matches = matcher.matches(name, value)
        if (negate) {
            return matches.not()
        }

        return matches
    }

    fun anyMatches(names: List<String>, value: String, negate: Boolean): Boolean {
        return names.any {
            matches(it, value, negate)
        }
    }

    fun allMatches(names: List<String>, value: String, negate: Boolean): Boolean {
        return names.all {
            matches(it, value, negate)
        }
    }

}