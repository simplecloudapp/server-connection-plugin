package app.simplecloud.plugin.connection.shared.matcher

object StartsWithOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.startsWith(value, ignoreCase = true)
    }

}