package app.simplecloud.plugin.connection.shared.matcher

interface OperationMatcher {

    fun matches(name: String, value: String): Boolean

}