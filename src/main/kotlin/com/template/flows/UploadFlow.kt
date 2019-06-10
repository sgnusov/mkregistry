package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UploadContract
import com.template.states.StateContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object UploadFlows
{
    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class UploadFlow(val attachmentHash: SecureHash) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val nodeName = CordaX500Name("PartyB", "New York", "US")
            val otherParty = serviceHub.networkMapCache.getPeerByLegalName(nodeName)!!

            // Stage 1.
            // Generate an unsigned transaction.
            val iouState = StateContract.UploadState(attachmentHash, ourIdentity, otherParty)

            val txCommand = Command(UploadContract.Create(), iouState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addAttachment(attachmentHash)
                    .addOutputState(iouState, "com.template.contracts.UploadContract")
                    .addCommand(txCommand)


            // Stage 2.
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            // Send the state to the counterparty, and receive it back with their signature.
            val otherPartyFlow = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow)))

            // Stage 5.
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(UploadFlow::class)
    class UploadFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction." using (output is StateContract.UploadState)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}