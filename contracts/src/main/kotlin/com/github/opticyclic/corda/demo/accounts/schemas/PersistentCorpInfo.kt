package com.github.opticyclic.corda.demo.accounts.schemas

import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "custom_accounts")
data class PersistentCorpInfo(
    @Column(unique = true, nullable = false)
    val linearId: UUID,
    @Column(unique = true, nullable = false)
    val accountId: UUID,
    @Column(unique = true, nullable = false)
    val name: String,
    @Column(unique = false, nullable = false)
    val type: String
) : PersistentState()
