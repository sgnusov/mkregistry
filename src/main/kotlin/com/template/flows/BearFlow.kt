package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BearContract
import com.template.states.StateContract
import com.template.schemas.BearSchemaV1
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.Random
import java.lang.Math

object BearFlows
{
    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearIssueFlow(val login: String) : FlowLogic<SignedTransaction>() {
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
            val txCommand = Command(BearContract.Issue(), listOf(ourIdentity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)

            val random = Random()
            for(i in 1..100) {
                var color = Math.abs(random.nextInt() % 256)
                val iouState = StateContract.BearState(color, login, ourIdentity)
                txBuilder.addOutputState(iouState, "com.template.contracts.BearContract")
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



    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearPresentFlow(val login: String, val receiverLogin: String, val color: Int) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            // Generate an unsigned transaction.
            val inputState = builder {
                serviceHub.vaultService.queryBy(
                    StateContract.BearState::class.java,
                    criteria =
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(login)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::color.equal(color)))
                )
            }.states[0]
            val outputState = StateContract.BearState(color, receiverLogin, ourIdentity)
            val txCommand = Command(BearContract.Present(), listOf(ourIdentity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)

            txBuilder.addInputState(inputState)
            txBuilder.addOutputState(outputState, "com.template.contracts.BearContract")

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

    @InitiatedBy(BearPresentFlow::class)
    class BearPresentFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub)
                    val input = ledgerTx.inputs.single().state.data
                    val output = ledgerTx.outputs.single().data
                    "This consumes a bear." using (input is StateContract.BearState)
                    "This issues a bear." using (output is StateContract.BearState)
                    "The characteristics aren't changed." using (
                        (input as StateContract.BearState).color ==
                        (output as StateContract.BearState).color
                    )
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}