package com.github.opticyclic.corda.demo.accounts.schemas

import net.corda.core.schemas.MappedSchema

object CorpInfoSchema : MappedSchema(
        version = 1,
        schemaFamily = CorpInfoSchema::class.java,
        mappedTypes = listOf(PersistentCorpInfo::class.java)
)
