package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.TelegramNotifier
import com.github.mrbean355.zakbot.db.PhraseType
import com.github.mrbean355.zakbot.db.entity.PhraseEntity
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import com.github.mrbean355.zakbot.db.type
import com.github.mrbean355.zakbot.phrases.Phrase
import com.github.mrbean355.zakbot.util.getString
import net.dean.jraw.models.Comment
import net.dean.jraw.models.Submission
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

private val UrlRegex = """[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)""".toRegex()

/**
 * Checks the contents of submissions and comments to see if the bot should send a reply.
 *
 * A reply is returned if a suitable phrase type is found (e.g. "Aaron" is mentioned), and the dice roll succeeds.
 * The returned reply will be a random one with the lowest usage count, from the phrase type.
 * URLs are excluded from the checks, as it could be unclear why a reply was sent.
 */
@Service
class PhraseService(
    private val phraseRepository: PhraseRepository,
    phrases: List<Phrase>,
    private val telegramNotifier: TelegramNotifier,
) {

    private val phrases = phrases.sortedByDescending { it.priority }

    @Transactional(readOnly = true)
    fun getAllPhrases(): List<PhraseEntity> {
        return phraseRepository.findAll().toList()
    }

    @Transactional
    fun addPhrase(content: String, type: Int, source: String?): PhraseEntity {
        val minUsages = phraseRepository.findMinUsagesByType(type) ?: 0
        return phraseRepository.save(PhraseEntity(0, content, minUsages, type, source)).also {
            telegramNotifier.sendMessage(
                getString(
                    "telegram.new_quote",
                    content,
                    PhraseType.name(type),
                    source.orEmpty().ifBlank { "None" }
                )
            )
        }
    }

    @Transactional
    fun findPhrase(submission: Submission): String? {
        return findPhrase(submission.title, submission.selfText)
    }

    @Transactional
    fun findPhrase(comment: Comment): String? {
        return findPhrase(comment.body)
    }

    private fun findPhrase(vararg inputs: String?): String? {
        inputs.filterNotNull().forEach { input ->
            val text = input.lowercase().replace(UrlRegex, "")
            val phrase = phrases.find { Random.nextFloat() <= it.getReplyChance(text) }

            if (phrase != null) {
                val choices = phraseRepository.findByType(phrase.type())
                val lowestUsage = choices.minOf { it.usages }

                return choices.filter { it.usages == lowestUsage }
                    .random()
                    .let { entity ->
                        phraseRepository.save(entity.copy(usages = entity.usages + 1))
                        val source = entity.source
                        if (source != null) {
                            getString("reddit.quote_source_prefix", entity.content, source.escapeParentheses())
                        } else {
                            entity.content
                        }
                    }
            }
        }
        return null
    }

    // Prevent the closing parenthesis in the quote's source being interpreted as closing the superscript block.
    // This caused the comment to render strangely on Reddit.
    private fun String.escapeParentheses(): String {
        return replace("(", "\\(")
            .replace(")", "\\)")
    }
}