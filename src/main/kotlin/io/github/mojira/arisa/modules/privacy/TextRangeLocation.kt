package io.github.mojira.arisa.modules.privacy

data class TextRangeLocation(
    private val completeText: String,
    /** Global start index, beginning at 0 */
    private val startIndex: Int,
    /** Global end index (inclusive), beginning at 0 */
    private val endIndex: Int
) {
    companion object {
        fun fromMatchResult(completeText: String, matchResult: MatchResult): TextRangeLocation {
            val range = matchResult.range
            return TextRangeLocation(completeText, range.first, range.last)
        }
    }

    private fun getIndexBehindNextLineTerminator(string: String, startIndex: Int): Int? {
        val nextLfIndex = string.indexOf('\n', startIndex)
        val nextCrIndex = string.indexOf('\r', startIndex)

        return (
            if (nextLfIndex != -1 && (nextCrIndex == -1 || nextLfIndex < nextCrIndex)) {
                // LF
                nextLfIndex + 1
            } else if (nextCrIndex != -1) {
                if (nextLfIndex == nextCrIndex + 1) {
                    // CR LF
                    nextLfIndex + 1
                } else {
                    // CR
                    nextCrIndex + 1
                }
            } else {
                null
            }
        )
    }

    fun getLocationDescription(): String {
        var currentLineStartIndex = 0
        // Start line numbering at 1
        var lineNumber = 1

        var startLine: Int? = null
        var relativeStartIndex = 0
        var endLine: Int? = null
        var relativeEndIndex = 0

        @Suppress("LoopWithTooManyJumpStatements")
        while (currentLineStartIndex < completeText.length) {
            val previousLineStartIndex = currentLineStartIndex
            currentLineStartIndex = getIndexBehindNextLineTerminator(completeText, previousLineStartIndex)
                ?: break // reached end of last line

            if (startIndex in previousLineStartIndex until currentLineStartIndex) {
                startLine = lineNumber
                relativeStartIndex = startIndex - previousLineStartIndex
            }
            if (endIndex in previousLineStartIndex until currentLineStartIndex) {
                endLine = lineNumber
                relativeEndIndex = endIndex - previousLineStartIndex
            }

            // Found both line numbers, can stop iteration
            if (startLine != null && endLine != null) {
                break
            }

            lineNumber++
        }

        // If startLine or endLine are still null they are in the last line
        if (startLine == null) {
            startLine = lineNumber
            relativeStartIndex = startIndex - currentLineStartIndex
        }
        if (endLine == null) {
            endLine = lineNumber
            relativeEndIndex = endIndex - currentLineStartIndex
        }

        return "$startIndex - $endIndex ($startLine:$relativeStartIndex - $endLine:$relativeEndIndex)"
    }
}
