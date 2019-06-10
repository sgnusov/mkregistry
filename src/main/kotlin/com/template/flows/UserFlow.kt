package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.sun.org.apache.xpath.internal.operations.Bool
import com.template.contracts.UploadContract
import com.template.contracts.UserContract
import com.template.states.StateContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import kotlin.coroutines.experimental.suspendCoroutine

object UserFlows
{
    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class UserCreateFlow(val login: String,
                         val password: String,
                         val adminRight: Boolean) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val otherParty = ourIdentity

            val userState = StateContract.UserState(login, password,
                    adminRight, false, false, false,
                    ourIdentity, otherParty)

            val txCommand = Command(UploadContract.Create(), userState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(userState, "com.template.contracts.UserContract")
                    .addCommand(txCommand)

            txBuilder.verify(serviceHub)

            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            val otherPartyFlow = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow)))

            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(UserCreateFlow::class)
    class UserCreateFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a user transaction." using (output is StateContract.UserState)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }

    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class UserChangeFlow(val login: String,
                         val uploadRight: Boolean,
                         val sendRight: Boolean,
                         val markRight: Boolean) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val otherParty = ourIdentity

            val inputState = serviceHub.vaultService.queryBy(StateContract.UserState::class.java).states
                    .filter { it -> it.state.data.login == login }[0]

            val userState = StateContract.UserState(login, inputState.state.data.password,
                    inputState.state.data.adminRight, uploadRight, markRight, sendRight,
                    ourIdentity, otherParty)

            val txCommand = Command(UserContract.Change(), userState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addOutputState(userState, "com.template.contracts.UserContract")
                    .addCommand(txCommand)


            txBuilder.verify(serviceHub)
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            val otherPartyFlow = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow)))

            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(UserChangeFlow::class)
    class UserChangeFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a user transaction." using (output is StateContract.UserState)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }

    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class UserDeleteFlow(val login: String) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val otherParty = ourIdentity

            val inputState = serviceHub.vaultService.queryBy(StateContract.UserState::class.java).states
                    .filter { it -> it.state.data.login == login }[0]

            val txCommand = Command(UserContract.Delete(), listOf(ourIdentity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addCommand(txCommand)


            txBuilder.verify(serviceHub)

            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            val otherPartyFlow = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow)))

            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(UserDeleteFlow::class)
    class UserDeleteFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a user transaction." using (output is StateContract.UserState)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}