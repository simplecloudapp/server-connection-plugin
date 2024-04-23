package app.simplecloud.plugin.connection.shared.matcher

object EndsWithOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.endsWith(value, ignoreCase = true)
    }

}