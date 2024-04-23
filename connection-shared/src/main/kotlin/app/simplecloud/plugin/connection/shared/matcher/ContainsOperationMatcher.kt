package app.simplecloud.plugin.connection.shared.matcher

object ContainsOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.contains(value, ignoreCase = true)
    }

}