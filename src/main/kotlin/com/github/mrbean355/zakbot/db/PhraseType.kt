package com.github.mrbean355.zakbot.db

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

object PhraseType {
    const val Aaron = 0
    const val Answers = 1
    const val Feeling = 2
    const val Generic = 3
    const val Mercury = 4
    const val Situation = 5
    const val Trinity = 6
    const val Understand = 7
    const val Zozo = 8
}

fun Phrase.type(): Int = when (this) {
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
