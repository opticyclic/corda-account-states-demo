package com.github.opticyclic.corda.demo.accounts.states

import com.github.opticyclic.corda.demo.accounts.contracts.CorpContract
import com.github.opticyclic.corda.demo.accounts.schemas.CorpInfoSchema
import com.github.opticyclic.corda.demo.accounts.schemas.PersistentCorpInfo
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable

/**
 * Custom Account with an extra property
 * @param accountInfo the [account][AccountInfo] linked to this company
 * @param accountType the [type][AccountType] of company
 */
@BelongsToContract(CorpContract::class)
data class CorpInfo(
    val accountInfo: AccountInfo,
    val accountType: AccountType,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    //The public keys of the involved parties.
    override val participants: List<AbstractParty> get() = listOf(accountInfo.host)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        if (schema is CorpInfoSchema) {
            return PersistentCorpInfo(
                linearId.id,
                accountInfo.identifier.id,
                accountInfo.name,
                accountType.name
            )
        } else {
            throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(CorpInfoSchema)
}

@CordaSerializable
enum class AccountType {
    BANK, AGENT
}
