package com.github.eprendre.tingshu.sources

import sources_by_myself.EighteenTS

object SourceEntry : ISourceEntry {
    override fun getSources(): List<TingShu> {
        return listOf(
            EighteenTS
        )
    }
}
