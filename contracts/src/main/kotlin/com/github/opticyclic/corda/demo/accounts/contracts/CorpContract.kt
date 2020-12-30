package com.github.opticyclic.corda.demo.accounts.contracts

import com.github.opticyclic.corda.demo.accounts.states.CorpInfo
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

/**
 * For a new [CorpInfo] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [CorpInfo].
 * - A Create() command
 */
class CorpContract : Contract {
    companion object {
        @JvmStatic
        val CORP_CONTRACT_ID = CorpContract::class.java.name
    }

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val signers = command.signers.toSet()

        when (command.value) {
            is Commands.Create -> verifyCreate(tx, signers)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs should be consumed when creating an Account." using (tx.inputs.isEmpty())
        "Only one output state should be created." using (tx.outputs.size == 1)
        "Output states must be CorpInfo States." using (tx.outputsOfType<CorpInfo>().isNotEmpty())
        val out = tx.outputsOfType<CorpInfo>().single()
        "All of the participants must be signers." using (signers.containsAll(out.participants.map { it.owningKey }))
    }

}
