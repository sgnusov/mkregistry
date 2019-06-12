package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.sun.org.apache.xpath.internal.operations.Bool
import com.template.contracts.UserContract
import com.template.states.StateContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.keys
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import kotlin.coroutines.experimental.suspendCoroutine
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory
import java.util.Base64

object UserFlows
{
    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class UserCreateFlow(val login: String,
                         val password: String,
                         val partyAddress: String) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            // Get userlist party
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!

            val partyKey = Base64.getEncoder().encodeToString(
                KeyFactory.getInstance("EC")
                    .getKeySpec(ourIdentity.owningKey, X509EncodedKeySpec::class.java)
                    .encoded
            )

            val userState = StateContract.UserState(login, password,
                    partyAddress, partyKey, ourIdentity, userListParty)

            val txCommand = Command(UserContract.Create(), userState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(userState, "com.template.contracts.UserContract")
                    .addCommand(txCommand)
            txBuilder.verify(serviceHub)

            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)
            val userListPartyFlow = initiateFlow(userListParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(userListPartyFlow)))

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
}