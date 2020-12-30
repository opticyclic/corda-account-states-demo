package com.github.opticyclic.corda.demo.flows.accounts

import co.paralleluniverse.fibers.Suspendable
import com.github.opticyclic.corda.demo.accounts.contracts.IOUAccountContract
import com.github.opticyclic.corda.demo.accounts.contracts.IOUAccountContract.Companion.IOU_CONTRACT_ID
import com.github.opticyclic.corda.demo.accounts.states.IOUAccountState
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

/**
 * This flow allows two parties to come to an agreement about the IOU encapsulated
 * within an [IOUAccountState].
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding.
 * In practice we would recommend splitting up the various stages of the flow into sub-routines.
 */
@InitiatingFlow
@StartableByRPC
class IOUAccountFlow(val iouValue: Int, val lender: String, val borrower: String) : FlowLogic<SignedTransaction>() {
    /*
     * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each checkpoint is reached.
     * See the 'progressTracker.currentStep' expressions within the call() function.
     */
    companion object {
        object BUILDING : Step("Building a new transaction.")
        object SIGNING : Step("Signing the transaction with our private key.")
        object COLLECTING : Step("Collecting the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING : Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            BUILDING,
            SIGNING,
            COLLECTING,
            FINALISING
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = BUILDING
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val lenderAccountInfo = accountService.accountInfo(lender).first().state.data
        val borrowerAccountInfo = accountService.accountInfo(borrower).first().state.data

        val lenderKey = subFlow(RequestKeyForAccount(lenderAccountInfo)).owningKey
        val borrowerKey = subFlow(RequestKeyForAccount(borrowerAccountInfo)).owningKey

        progressTracker.currentStep = BUILDING
        //Create the output.
        val outputState = IOUAccountState(iouValue, lenderKey, borrowerKey)

        //Create the command.
        val command = Command(IOUAccountContract.Commands.Create(), outputState.participants.map { it.owningKey })

        //Build the transaction.
        val txBuilder = TransactionBuilder(notary)
            .addOutputState(outputState, IOU_CONTRACT_ID)
            .addCommand(command)

        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        //Sign with our node key AND the private key from the lender+borrower accounts
        val keysToSignWith = mutableListOf(ourIdentity.owningKey, lenderKey, borrowerKey)

        //We would do different things depending on whether the accounts were on this node or other nodes.
        //For this demo they are all on the single node so we can simplify the flow
        val locallySignedTx = serviceHub.signInitialTransaction(txBuilder, keysToSignWith)

        progressTracker.currentStep = FINALISING
        return subFlow(FinalityFlow(locallySignedTx, emptyList()))
    }
}


@InitiatedBy(IOUAccountFlow::class)
class IOUAccountResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction." using (output is IOUAccountState)
                val iou = output as IOUAccountState
                "IOUs with a value over 100 are not accepted." using (iou.value <= 100)
            }
        }
        val txId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}
