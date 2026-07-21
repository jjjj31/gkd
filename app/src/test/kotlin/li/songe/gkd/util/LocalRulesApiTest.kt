package li.songe.gkd.util

import li.songe.gkd.data.RawSubscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocalRulesApiTest {
    @Test
    fun appendLocalRulesAppendsNewGroupWithFreshKeys() {
        val current = localSubscription(
            app(
                groups = listOf(
                    group(
                        key = 3,
                        name = "开屏广告",
                        rules = listOf(rule(key = 0, matches = listOf("[vid=\"old\"]"))),
                    ),
                ),
            ),
        )
        val incoming = app(
            groups = listOf(
                group(
                    key = 0,
                    name = "开屏广告",
                    rules = listOf(rule(key = 0, matches = listOf("[vid=\"new\"]"))),
                ),
            ),
        )

        val result = appendLocalRules(current, incoming)

        assertEquals(1, result.addedRules)
        assertEquals(0, result.skippedDuplicates)
        assertEquals(1, result.addedGroups)
        val nextApp = result.subscription.apps.single()
        assertEquals(listOf(3, 4), nextApp.groups.map { it.key })
        assertEquals(0, nextApp.groups[1].rules.single().key)
        assertEquals(listOf("[vid=\"new\"]"), nextApp.groups[1].rules.single().matches)
    }

    @Test
    fun appendLocalRulesCreatesMissingAppAndSkipsDuplicateSelectors() {
        val current = localSubscription(
            app(
                id = "com.exists",
                groups = listOf(
                    group(
                        key = 0,
                        rules = listOf(rule(matches = listOf("[vid=\"skip\"]"))),
                    ),
                ),
            ),
        )
        val incoming = app(
            id = "com.demo",
            groups = listOf(
                group(
                    key = 9,
                    rules = listOf(
                        rule(key = 8, matches = listOf("[vid=\"first\"]")),
                        rule(key = 9, matches = listOf("[vid=\"first\"]")),
                    ),
                ),
            ),
        )

        val result = appendLocalRules(current, incoming)

        assertEquals(1, result.addedRules)
        assertEquals(1, result.skippedDuplicates)
        assertEquals(2, result.subscription.apps.size)
        val newApp = result.subscription.apps.last()
        assertEquals("com.demo", newApp.id)
        assertEquals(0, newApp.groups.single().key)
        assertEquals(0, newApp.groups.single().rules.single().key)
        assertEquals(listOf("[vid=\"first\"]"), newApp.groups.single().rules.single().matches)
    }

    @Test
    fun appendLocalRulesDoesNotAddGroupWhenAllRulesAlreadyExist() {
        val current = localSubscription(
            app(
                groups = listOf(
                    group(
                        key = 0,
                        rules = listOf(rule(matches = listOf("[vid=\"skip\"]"))),
                    ),
                ),
            ),
        )
        val incoming = app(
            groups = listOf(
                group(
                    key = 0,
                    rules = listOf(rule(matches = listOf("[vid=\"skip\"]"))),
                ),
            ),
        )

        val result = appendLocalRules(current, incoming)

        assertEquals(0, result.addedRules)
        assertEquals(1, result.skippedDuplicates)
        assertEquals(0, result.addedGroups)
        assertEquals(1, result.subscription.apps.single().groups.size)
        assertFalse(result.changed)
    }
}

private fun localSubscription(vararg apps: RawSubscription.RawApp) = RawSubscription(
    id = LOCAL_SUBS_ID,
    name = "本地订阅",
    version = 0,
    apps = apps.toList(),
)

private fun app(
    id: String = "com.demo",
    name: String = "Demo",
    groups: List<RawSubscription.RawAppGroup>,
) = RawSubscription.RawApp(
    id = id,
    name = name,
    groups = groups,
)

private fun group(
    key: Int,
    name: String = "开屏广告",
    rules: List<RawSubscription.RawAppRule>,
) = RawSubscription.RawAppGroup(
    key = key,
    name = name,
    desc = null,
    enable = null,
    scopeKeys = null,
    actionCdKey = null,
    actionMaximumKey = null,
    actionCd = null,
    actionDelay = null,
    fastQuery = true,
    matchRoot = null,
    actionMaximum = 1,
    priorityTime = null,
    priorityActionMaximum = null,
    order = null,
    forcedTime = null,
    matchDelay = null,
    matchTime = 30000,
    resetMatch = "app",
    snapshotUrls = null,
    excludeSnapshotUrls = null,
    exampleUrls = null,
    activityIds = null,
    excludeActivityIds = null,
    rules = rules,
    versionCode = null,
    versionName = null,
    ignoreGlobalGroupMatch = null,
)

private fun rule(
    key: Int? = null,
    matches: List<String>,
) = RawSubscription.RawAppRule(
    key = key,
    name = "自动跳过",
    preKeys = null,
    action = null,
    position = null,
    swipeArg = null,
    matches = matches,
    excludeMatches = null,
    excludeAllMatches = null,
    anyMatches = null,
    actionCdKey = null,
    actionMaximumKey = null,
    actionCd = null,
    actionDelay = null,
    fastQuery = true,
    matchRoot = null,
    actionMaximum = null,
    priorityTime = null,
    priorityActionMaximum = null,
    order = null,
    forcedTime = null,
    matchDelay = null,
    matchTime = null,
    resetMatch = null,
    snapshotUrls = null,
    excludeSnapshotUrls = null,
    exampleUrls = null,
    activityIds = null,
    excludeActivityIds = null,
    versionCode = null,
    versionName = null,
)
