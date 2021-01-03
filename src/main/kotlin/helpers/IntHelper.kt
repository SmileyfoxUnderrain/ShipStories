package helpers

fun toPreviousCurrentAndNext(number: Int) =
    IntProgression.fromClosedRange(number - 1, number + 1, 1).toList()
