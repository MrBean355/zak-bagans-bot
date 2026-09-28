package com.github.mrbean355.zakbot.db

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.github.mrbean355.zakbot.phrases.AaronPhrase
import com.github.mrbean355.zakbot.phrases.AnswersPhrase
import com.github.mrbean355.zakbot.phrases.FeelingPhrase
import com.github.mrbean355.zakbot.phrases.GenericPhrase
import com.github.mrbean355.zakbot.phrases.MercuryPhrase
import com.github.mrbean355.zakbot.phrases.Phrase
import com.github.mrbean355.zakbot.phrases.SituationPhrase
import com.github.mrbean355.zakbot.phrases.TrinityPhrase
import com.github.mrbean355.zakbot.phrases.UnderstandPhrase
import com.github.mrbean355.zakbot.phrases.ZozoPhrase

enum class PhraseType(
    @get:JsonValue
    val id: Int
) {
    Aaron(0),
    Answers(1),
    Feeling(2),
    Generic(3),
    Mercury(4),
    Situation(5),
    Trinity(6),
    Understand(7),
    Zozo(8);

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromJson(value: Any): PhraseType {
            return when (value) {
                is Number -> fromId(value.toInt())
                is String -> value.toIntOrNull()?.let(::fromId)
                    ?: entries.find { it.name.equals(value, ignoreCase = true) }
                    ?: throw IllegalArgumentException("Unknown phrase type: $value")

                else -> throw IllegalArgumentException("Invalid phrase type: $value")
            }
        }

        fun fromId(id: Int): PhraseType = fromIdOrNull(id)
            ?: throw IllegalArgumentException("Unknown phrase type ID: $id")

        fun fromIdOrNull(id: Int): PhraseType? = entries.find { it.id == id }

        fun name(type: Int): String = fromIdOrNull(type)?.name ?: "Unknown"
    }
}

fun Phrase.type(): PhraseType = when (this) {
    is AaronPhrase -> PhraseType.Aaron
    is AnswersPhrase -> PhraseType.Answers
    is FeelingPhrase -> PhraseType.Feeling
    is GenericPhrase -> PhraseType.Generic
    is MercuryPhrase -> PhraseType.Mercury
    is SituationPhrase -> PhraseType.Situation
    is TrinityPhrase -> PhraseType.Trinity
    is UnderstandPhrase -> PhraseType.Understand
    is ZozoPhrase -> PhraseType.Zozo
}
