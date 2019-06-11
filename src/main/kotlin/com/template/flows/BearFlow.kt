package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BearIssueContract
import com.template.states.StateContract
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object BearFlows
{
    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearIssueFlow(val color: Int, val login: String) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Ask the userlist party to make sure there is a single user registered
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!
            val userListProxy = CordaRPCClient(NetworkHostAndPort.parse("127.0.0.1:10005")).start("user1", "test").proxy
            val cntUsers = userListProxy.vaultQuery(StateContract.UserState::class.java).states.size
            if (cntUsers != 1) {
                throw FlowException("Need exactly one user to be registered to issue bears, got ${cntUsers}")
            }


            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val nodeName = CordaX500Name("PartyB", "New York", "US")

            // Stage 1.
            // Generate an unsigned transaction.
            val iouState = StateContract.BearState(color, login, ourIdentity)

            val txCommand = Command(BearIssueContract.Issue(), iouState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)

            for(i in 1..100) {
                txBuilder.addOutputState(iouState, "com.template.contracts.BearIssueContract")
            }

            // Stage 2.
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            // Sign the transaction.
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(signedTx))
        }
    }

    @InitiatedBy(BearIssueFlow::class)
    class BearIssueFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This issues a bear." using (output is StateContract.BearState)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}