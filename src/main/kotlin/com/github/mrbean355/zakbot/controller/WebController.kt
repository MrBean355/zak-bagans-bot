package com.github.mrbean355.zakbot.controller

import com.github.mrbean355.zakbot.db.PhraseType
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class WebController(
    private val phraseRepository: PhraseRepository
) {

    @GetMapping("/")
    fun index(): String {
        return buildString {
            appendLine(
                """
                <html>
                    <title>Zak Bot Responses</title>
                    <head>
                      <link href="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.css" rel="stylesheet">
                      <script src="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.js"></script>
                    </head>
                    <body>
                """.trimIndent()
            )

            appendResponses(PhraseType.Aaron, "Aaron", "10% chance to reply when \"Aaron\" is mentioned.")
            appendResponses(PhraseType.Answers, "Answers", "25% chance to reply when \"we want answers\" is mentioned.")
            appendResponses(PhraseType.Feeling, "Feeling", "25% chance to reply when \"I feel\" or \"I'm feeling\" is mentioned.")
            appendResponses(PhraseType.Generic, "Generic", "15% chance to reply when \"Zak\" or \"Bagans\" is mentioned.")
            appendResponses(PhraseType.Mercury, "Mercury", "25% chance to reply when \"mercury\" is mentioned.")
            appendResponses(PhraseType.Situation, "Situation", "20% chance to reply when \"situation\" is mentioned.")
            appendResponses(PhraseType.Trinity, "Trinity", "10% chance to reply when \"3\" or \"three\" is mentioned.")
            appendResponses(PhraseType.Understand, "Understand", "20% chance to reply when \"understand\" is mentioned.")
            appendResponses(PhraseType.Zozo, "Zozo", "25% chance to reply when \"Zozo\" is mentioned.")

            appendLine(
                """
                    </body>
                </html>
                """.trimIndent()
            )
        }
    }

    private fun StringBuilder.appendResponses(type: Int, title: String, description: String) {
        appendLine("\t<p id=\"${title.lowercase()}\" class=\"mdc-typography--headline4\">${title}</p>")
        appendLine("\t<p class=\"mdc-typography--subtitle1\">${description}</p>")
        appendLine("\t<ul class=\"mdc-list\">")
        phraseRepository.findByType(type).sortedBy { it.content }.forEach { phrase ->
            append("\t\t<li class=\"mdc-list-item\">").append(phrase.content)
            if (phrase.source != null) {
                append(" (quote from: \"${phrase.source}\")")
            }
            appendLine("</li>")
        }
        appendLine("\t</ul>")
    }
}