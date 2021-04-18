package com.github.mrbean355.zakbot.substitutions

import io.mockk.every
import io.mockk.mockk
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.references.CommentReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SubstitutionsKtTest {

    @Test
    internal fun testSubstitute() {
        val contribution = mockk<PublicContribution<CommentReference>> {
            every { author } returns "Mr_Bean355"
        }

        val result = "Hello {author}!".substitute(contribution)

        assertEquals("Hello Mr_Bean355!", result)
    }
}