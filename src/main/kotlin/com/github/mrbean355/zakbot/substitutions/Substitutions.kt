package com.github.mrbean355.zakbot.substitutions

import net.dean.jraw.models.PublicContribution

private const val AUTHOR_NAME = "{author}"

fun String.substitute(contribution: PublicContribution<*>): String =
    replace(AUTHOR_NAME, contribution.author)