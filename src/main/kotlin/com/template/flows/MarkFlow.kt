package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.SendContract
import com.template.states.StateContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object MarkFlows
{
    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class MarkFlow(val attachmentHash: SecureHash,
                       val name: String,
                       val mark: Int) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val nodeName = CordaX500Name("PartyF", "New York", "US")
            val otherParty = serviceHub.networkMapCache.getPeerByLegalName(nodeName)!!

            val inputState = serviceHub.vaultService.queryBy(StateContract.SendState::class.java).states
                    .filter { it -> it.state.data.hash == attachmentHash }[0]

            // Generate an unsigned transaction.
            val markState = StateContract.MarkState(attachmentHash, name, mark, ourIdentity, otherParty)

            val txCommand = Command(SendContract.Create(), markState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addAttachment(attachmentHash)
                    .addInputState(inputState)
                    .addOutputState(markState, "com.template.contracts.MarkContract")
                    .addCommand(txCommand)


            txBuilder.verify(serviceHub)
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            val otherPartyFlow = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow)))

            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(MarkFlow::class)
    class MarkFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                }
            }
            return subFlow(signTransactionFlow)
        }
    }
}