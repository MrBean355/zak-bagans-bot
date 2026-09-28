package com.github.mrbean355.zakbot.db.entity

import com.github.mrbean355.zakbot.db.PhraseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class EntityEqualsHashCodeTest {

    @Test
    fun testAppUserEntity_EqualsAndHashCode() {
        val user1 = AppUserEntity(id = 1, username = "admin", password = "pw1")
        val user2 = AppUserEntity(id = 1, username = "admin", password = "pw2")
        val user3 = AppUserEntity(id = 2, username = "admin", password = "pw1")
        val unpersisted1 = AppUserEntity(id = 0, username = "guest", password = "pw")
        val unpersisted2 = AppUserEntity(id = 0, username = "guest", password = "pw")

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
        assertNotEquals(user1, user3)
        assertEquals(unpersisted1, unpersisted2)
    }

    @Test
    fun testLastCheckedEntity_EqualsAndHashCode() {
        val now = Instant.now()
        val e1 = LastCheckedEntity("post", now)
        val e2 = LastCheckedEntity("post", now.plusSeconds(60))
        val e3 = LastCheckedEntity("comment", now)

        assertEquals(e1, e2)
        assertEquals(e1.hashCode(), e2.hashCode())
        assertNotEquals(e1, e3)
    }

    @Test
    fun testIgnoredUserEntity_EqualsAndHashCode() {
        val now = Instant.now()
        val u1 = IgnoredUserEntity("u1", now, "src1")
        val u2 = IgnoredUserEntity("u1", now.plusSeconds(10), "src2")
        val u3 = IgnoredUserEntity("u2", now, "src1")

        assertEquals(u1, u2)
        assertEquals(u1.hashCode(), u2.hashCode())
        assertNotEquals(u1, u3)
    }

    @Test
    fun testIgnoredSubmissionEntity_EqualsAndHashCode() {
        val now = Instant.now()
        val s1 = IgnoredSubmissionEntity("sub1", now, "reason1")
        val s2 = IgnoredSubmissionEntity("sub1", now.plusSeconds(10), "reason2")
        val s3 = IgnoredSubmissionEntity("sub2", now, "reason1")

        assertEquals(s1, s2)
        assertEquals(s1.hashCode(), s2.hashCode())
        assertNotEquals(s1, s3)
    }

    @Test
    fun testPhraseEntity_EqualsAndHashCode() {
        val p1 = PhraseEntity(id = 1, content = "Quote", usages = 0, type = PhraseType.Aaron, source = null)
        val p2 = PhraseEntity(id = 1, content = "Quote 2", usages = 5, type = PhraseType.Aaron, source = "s")
        val p3 = PhraseEntity(id = 2, content = "Quote", usages = 0, type = PhraseType.Aaron, source = null)
        val unpersisted1 = PhraseEntity(id = 0, content = "Quote", usages = 0, type = PhraseType.Aaron, source = null)
        val unpersisted2 = PhraseEntity(id = 0, content = "Quote", usages = 0, type = PhraseType.Aaron, source = null)

        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
        assertNotEquals(p1, p3)
        assertEquals(unpersisted1, unpersisted2)
    }
}
