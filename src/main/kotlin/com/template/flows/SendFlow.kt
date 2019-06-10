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

object SendFlow
{
    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class SendInfoFlow(val attachmentHash: SecureHash,
                       val name: String,
                       val description: String) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val markParties = listOf(
                    CordaX500Name("PartyC", "New York", "US"),
                    CordaX500Name("PartyD", "New York", "US"),
                    CordaX500Name("PartyE", "New York", "US")
            ).map { it -> serviceHub.networkMapCache.getPeerByLegalName(it)!! }

            val inputState = serviceHub.vaultService.queryBy(StateContract.UploadState::class.java).states
                    .filter { it -> it.state.data.hash == attachmentHash }[0]


            val outputStates = markParties.map {
                party -> StateContract.SendState(attachmentHash, name, description, ourIdentity, party)
            }

            val txCommand = Command(SendContract.Create(),
                    outputStates[0].participants.map { it.owningKey }.union(
                    outputStates[1].participants.map { it.owningKey }).union(
                    outputStates[2].participants.map { it.owningKey }).toList())

            val txBuilder = TransactionBuilder(notary)
                    .addAttachment(attachmentHash)
                    .addInputState(inputState)
                    .addOutputState(outputStates[0], "com.template.contracts.SendContract")
                    .addOutputState(outputStates[1], "com.template.contracts.SendContract")
                    .addOutputState(outputStates[2], "com.template.contracts.SendContract")
                    .addCommand(txCommand)


            txBuilder.verify(serviceHub)

            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            val otherPartyFlow       = initiateFlow(markParties[0])
            val otherPartySecondFlow = initiateFlow(markParties[1])
            val otherPartyThirdFlow  = initiateFlow(markParties[2])

            //val b = markParties.map { party -> initiateFlow(party) }.toSet()
            val fullySignedTx = subFlow(
                    CollectSignaturesFlow(
                            partSignedTx,
                            setOf(otherPartyFlow, otherPartySecondFlow, otherPartyThirdFlow)
                    )
            )

            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(SendInfoFlow::class)
    class InfoFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
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