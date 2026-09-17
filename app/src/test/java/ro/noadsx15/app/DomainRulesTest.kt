package ro.noadsx15.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainRulesTest {
    @Test fun parsesAdblock() {
        assertEquals(false to "ads.example.com", DomainRules.parseLine("||ads.example.com^"))
    }

    @Test fun parsesAllow() {
        assertEquals(true to "cdn.example.com", DomainRules.parseLine("@@||cdn.example.com^"))
    }

    @Test fun parsesHosts() {
        assertEquals(false to "ads.example.com", DomainRules.parseLine("0.0.0.0 ads.example.com"))
    }

    @Test fun matchesSubdomain() {
        assertTrue(DomainRules.matches("a.b.ads.example.com", setOf("ads.example.com")))
    }

    @Test fun doesNotMatchSibling() {
        assertFalse(DomainRules.matches("goodexample.com", setOf("example.com")))
    }

    @Test fun ignoresCosmeticRule() {
        assertNull(DomainRules.parseLine("example.com##.banner"))
    }
}
