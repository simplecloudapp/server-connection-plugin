package app.simplecloud.plugin.connection.shared.matcher

object EqualsOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.equals(value, ignoreCase = true)
    }

}