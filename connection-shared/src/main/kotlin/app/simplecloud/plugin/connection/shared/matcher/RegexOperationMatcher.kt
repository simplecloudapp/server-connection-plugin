package app.simplecloud.plugin.connection.shared.matcher

object RegexOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.equals(Regex.fromLiteral(value))
    }

}