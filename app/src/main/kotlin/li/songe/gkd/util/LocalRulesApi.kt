package li.songe.gkd.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import li.songe.gkd.data.RawSubscription

@Serializable
data class LocalRulesAppendResponse(
    val ok: Boolean = true,
    val action: String = "append",
    val appId: String,
    val appName: String? = null,
    val addedGroups: Int,
    val addedRules: Int,
    val skippedDuplicates: Int,
)

data class LocalRulesAppendRequest(
    val app: RawSubscription.RawApp,
    val dedupe: Boolean = true,
)

data class LocalRulesAppendResult(
    val subscription: RawSubscription,
    val appId: String,
    val appName: String?,
    val addedGroups: Int,
    val addedRules: Int,
    val skippedDuplicates: Int,
) {
    val changed: Boolean
        get() = addedRules > 0

    fun toResponse() = LocalRulesAppendResponse(
        appId = appId,
        appName = appName,
        addedGroups = addedGroups,
        addedRules = addedRules,
        skippedDuplicates = skippedDuplicates,
    )
}

fun parseLocalRulesAppendRequest(source: String): LocalRulesAppendRequest {
    val root = json.parseToJsonElement(source).jsonObject
    val appElement = root["app"]
    val appObject = when (appElement) {
        null, JsonNull -> root
        is JsonObject -> appElement
        else -> error("app must be an object")
    }
    val dedupe = root["dedupe"]?.jsonPrimitive?.booleanOrNull ?: true
    return LocalRulesAppendRequest(
        app = RawSubscription.parseApp(appObject),
        dedupe = dedupe,
    )
}

fun appendLocalRules(
    current: RawSubscription,
    incomingApp: RawSubscription.RawApp,
    dedupe: Boolean = true,
): LocalRulesAppendResult {
    val apps = current.apps.toMutableList()
    val appIndex = apps.indexOfFirst { it.id == incomingApp.id }
    val currentApp = if (appIndex >= 0) {
        apps[appIndex]
    } else {
        RawSubscription.RawApp(
            id = incomingApp.id,
            name = incomingApp.name,
            groups = emptyList(),
        )
    }

    val seenSelectors = currentApp.groups.flatMap { group ->
        group.rules.flatMap { rule -> rule.getAllSelectorStrings() }
    }.toMutableSet()
    var nextGroupKey = (currentApp.groups.maxOfOrNull { it.key } ?: -1) + 1
    var addedGroups = 0
    var addedRules = 0
    var skippedDuplicates = 0
    val appendedGroups = mutableListOf<RawSubscription.RawAppGroup>()

    incomingApp.groups.forEach { sourceGroup ->
        var nextRuleKey = 0
        val nextRules = sourceGroup.rules.mapNotNull { sourceRule ->
            val selectors = sourceRule.getAllSelectorStrings()
            if (dedupe && selectors.isNotEmpty() && selectors.any { seenSelectors.contains(it) }) {
                skippedDuplicates += 1
                null
            } else {
                selectors.forEach { seenSelectors.add(it) }
                addedRules += 1
                sourceRule.copy(key = nextRuleKey++)
            }
        }
        if (nextRules.isNotEmpty()) {
            appendedGroups.add(
                sourceGroup.copy(
                    key = nextGroupKey++,
                    rules = nextRules,
                )
            )
            addedGroups += 1
        }
    }

    if (appendedGroups.isNotEmpty()) {
        val nextApp = currentApp.copy(
            name = currentApp.name ?: incomingApp.name,
            groups = currentApp.groups + appendedGroups,
        )
        if (appIndex >= 0) {
            apps[appIndex] = nextApp
        } else {
            apps.add(nextApp)
        }
    }

    return LocalRulesAppendResult(
        subscription = current.copy(
            id = LOCAL_SUBS_ID,
            name = current.name.ifBlank { "本地订阅" },
            apps = apps,
        ),
        appId = incomingApp.id,
        appName = incomingApp.name,
        addedGroups = addedGroups,
        addedRules = addedRules,
        skippedDuplicates = skippedDuplicates,
    )
}
