package com.github.opticyclic.corda.demo.flows.accounts

import co.paralleluniverse.fibers.Suspendable
import com.github.opticyclic.corda.demo.accounts.contracts.CorpContract
import com.github.opticyclic.corda.demo.accounts.contracts.CorpContract.Companion.CORP_CONTRACT_ID
import com.github.opticyclic.corda.demo.accounts.states.AccountType
import com.github.opticyclic.corda.demo.accounts.states.CorpInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * A flow to create a custom Corda account with a property that defines the "type" of company
 * @param companyName the company name
 * @param companyType the [type][AccountType] of company
 * @see CorpInfo
 */
@StartableByService
@StartableByRPC
class CreateCorp(
    private val companyName: String,
    private val companyType: String
) : FlowLogic<StateAndRef<CorpInfo>>() {

    /*
     * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each checkpoint is reached.
     * See the 'progressTracker.currentStep' expressions within the call() function.
     */
    companion object {
        object CREATING : ProgressTracker.Step("Creating a new output state with linked Corda Account.")
        object BUILDING : ProgressTracker.Step("Building a new transaction.")
        object SIGNING : ProgressTracker.Step("Signing the transaction.")
        object FINALISING : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            CREATING,
            BUILDING,
            SIGNING,
            FINALISING
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): StateAndRef<CorpInfo> {
        progressTracker.currentStep = CREATING
        //Create the Account to link to the output state
        val cordaAccount = subFlow(CreateAccount(companyName))
        val cordaAccountInfo = cordaAccount.state.data
        //Create the output.
        val outputState = CorpInfo(cordaAccountInfo, AccountType.valueOf(companyType))

        progressTracker.currentStep = BUILDING
        val accountKey = subFlow(RequestKeyForAccount(outputState.accountInfo)).owningKey
        val keysToSignWith = mutableListOf(ourIdentity.owningKey, accountKey)
        val command = Command(CorpContract.Commands.Create(), keysToSignWith)

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        //Build the transaction.
        val txBuilder = TransactionBuilder(notary)
            .addOutputState(outputState, CORP_CONTRACT_ID)
            .addCommand(command)

        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val signedTransaction = serviceHub.signInitialTransaction(txBuilder, keysToSignWith)

        progressTracker.currentStep = FINALISING
        val finalisedTransaction = subFlow(FinalityFlow(signedTransaction, emptyList()))
        return finalisedTransaction.coreTransaction.outRefsOfType<CorpInfo>().single()
    }
}
