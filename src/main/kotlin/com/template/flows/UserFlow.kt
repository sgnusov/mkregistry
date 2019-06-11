package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.sun.org.apache.xpath.internal.operations.Bool
import com.template.contracts.BearContract
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
                         val password: String) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val userState = StateContract.UserState(login, password,
                    ourIdentity)

            val txCommand = Command(UserContract.Create(), userState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(userState, "com.template.contracts.UserContract")
                    .addCommand(txCommand)
            txBuilder.verify(serviceHub)

            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            return subFlow(FinalityFlow(signedTx))
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
}