package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BearContract
import com.template.states.StateContract
import com.template.schemas.BearSchemaV1
import com.template.schemas.UserSchemaV1
import com.template.characteristics.Characteristics
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.*
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.node.services.vault.builder
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.node.StatesToRecord
import net.corda.core.utilities.unwrap
import sun.java2d.StateTrackable
import java.lang.Math
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory
import java.security.MessageDigest
import java.util.Base64

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

            // Stage 1.
            // Generate an unsigned transaction.
            val txCommand = Command(BearContract.Issue(), listOf(ourIdentity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)

            for (i in 1..100) {
                val chars = Characteristics.random()
                val iouState = StateContract.BearState(chars, login, "", ourIdentity)
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
    class BearPresentFlow(val login: String, val receiverLogin: String, val chars: Characteristics) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Get receiver party
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!
            val userListProxy = CordaRPCClient(NetworkHostAndPort.parse("127.0.0.1:10005")).start("user1", "test").proxy
            val receiver = userListProxy.vaultQuery(StateContract.UserState::class.java).states.filter { it: StateAndRef<StateContract.UserState> ->
                (it.state.data.login == receiverLogin)
            }.singleOrNull()
            receiver ?: throw FlowException("The receiver is not registered")

            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            // Generate an unsigned transaction.
            val identity = Party(
                receiver.state.data.participants[0].name,
                KeyFactory.getInstance("EdDSA")
                    .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(receiver.state.data.partyKey)))
            )
            val inputState = builder {
                serviceHub.vaultService.queryBy(
                        StateContract.BearState::class.java,
                        criteria =
                        VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                                .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(login)))
                                .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::chars.equal(chars)))
                )
            }.states[0]
            val outputState = StateContract.BearState(chars, receiverLogin, "", identity)
            val txCommand = Command(BearContract.Present(), listOf(ourIdentity.owningKey, identity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)

            txBuilder.addInputState(inputState)
            txBuilder.addOutputState(outputState, "com.template.contracts.BearContract")

            // Stage 2.
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)
            val receiverFlow = initiateFlow(identity)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(receiverFlow)))

            // Stage 4.
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(BearPresentFlow::class)
    class BearPresentFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val input = ledgerTx.inputs.single().state.data
                    val output = ledgerTx.outputs.single().data
                    "This consumes a bear." using (input is StateContract.BearState)
                    "This issues a bear." using (output is StateContract.BearState)
                    "The characteristics aren't changed." using (
                        (input as StateContract.BearState).chars ==
                        (output as StateContract.BearState).chars
                    )
                }
            }

            return subFlow(signTransactionFlow)
        }
    }



    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearMixFlow(val login: String, val chars1: Characteristics, val chars2: Characteristics) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Ask the userlist party to make sure there is a single user registered
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!
            val userListProxy = CordaRPCClient(NetworkHostAndPort.parse("127.0.0.1:10005")).start("user1", "test").proxy
            val users = builder {
                userListProxy.vaultQueryByCriteria<StateContract.UserState>(
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                        .and(QueryCriteria.VaultCustomQueryCriteria(UserSchemaV1.PersistentUser::login.equal(login))),
                        StateContract.UserState::class.java
                )
            }.states.singleOrNull()
            if (users == null) {
                throw FlowException("No such user")
            }

            val bears1 = builder {
                serviceHub.vaultService.queryBy(
                    StateContract.BearState::class.java,
                    QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(QueryCriteria.VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(login)))
                    .and(QueryCriteria.VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::chars.equal(chars1)))
                )
            }.states

            var bear1: StateAndRef<StateContract.BearState>? = null
            if (bears1.isNotEmpty()) {
                bear1 = bears1[0]
            }

            val bears2 = builder {
                serviceHub.vaultService.queryBy(
                        StateContract.BearState::class.java,
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                                .and(QueryCriteria.VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(login)))
                                .and(QueryCriteria.VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::chars.equal(chars2)))
                )
            }.states

            var bear2: StateAndRef<StateContract.BearState>? = null
            if (bears2.isNotEmpty()) {
                bear2 = bears2[0]
            }

            if (bear1 == null || bear2 == null) {
                throw FlowException("User does not have these bears")
            }

            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            // Generate an unsigned transaction.
            val txCommand = Command(BearContract.Mix(), listOf(ourIdentity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                .addInputState(bear1)
                .addInputState(bear2)
                .addCommand(txCommand)

            val bearState = StateContract.BearState(chars1.mix(chars2), login, "", ourIdentity)
            txBuilder.addOutputState(bearState, "com.template.contracts.BearContract")

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

    @InitiatedBy(BearMixFlow::class)
    class BearMixFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    "This issues a bear." using (ledgerTx.outputs.single().data is StateContract.BearState)
                    val output = ledgerTx.outputs.single().data as  StateContract.BearState
                    "This consumes 2 inputs." using (ledgerTx.inputs.size == 2)
                    "This consumes bears." using (ledgerTx.inputs.all { it.state.data is StateContract.BearState })
                    val bear1 = ledgerTx.inputs[0].state.data as StateContract.BearState
                    val bear2 = ledgerTx.inputs[1].state.data as StateContract.BearState
                    "This mixes correctly." using (output.chars == bear1.chars.mix(bear2.chars))
                }
            }

            return subFlow(signTransactionFlow)
        }
    }



    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearKeyChangeFlow(val login: String,
                            val chars: Characteristics,
                            val newKeyHash: String) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Ask the userlist party to make sure there is a single user registered
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!
            val userListProxy = CordaRPCClient(NetworkHostAndPort.parse("127.0.0.1:10005")).start("user1", "test").proxy

            // Check that there are no bears with same key
            val isUnique = builder {
                serviceHub.vaultService.queryBy(
                    StateContract.BearState::class.java,
                    criteria =
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(login)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::keyHash.equal(newKeyHash)))
                )
            }.states.isEmpty()
            if (!isUnique) {
                throw FlowException("Key must be unique.")
            }

            val inputState = builder {
                serviceHub.vaultService.queryBy(
                    StateContract.BearState::class.java,
                    criteria =
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(login)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::chars.equal(chars)))
                )
            }.states[0]

            val outputState = StateContract.BearState(
                inputState.state.data.chars,
                inputState.state.data.ownerLogin,
                newKeyHash,
                inputState.state.data.issuer
            )

            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            // Generate an unsigned transaction.
            val txCommand = Command(BearContract.SwapInitialize(), listOf(ourIdentity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)
                    .addInputState(inputState)
                    .addOutputState(outputState, "com.template.contracts.BearContract")

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

    @InitiatedBy(BearKeyChangeFlow::class)
    class BearKeyChangeFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub)
                    val input = ledgerTx.inputs.single().state.data
                    val output = ledgerTx.outputs.single().data
                    "This consumes a bear." using (input is StateContract.BearState)
                    "This issues a bear." using (output is StateContract.BearState)
                    "The owner isn't changed." using (
                        (input as StateContract.BearState).ownerLogin ==
                        (output as StateContract.BearState).ownerLogin
                    )
                    "The characteristics aren't changed." using (
                        input.chars == output.chars
                    )
                }
            }

            return subFlow(signTransactionFlow)
        }
    }



    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearSwapFlow(val login: String, val friendLogin: String, val chars: Characteristics, val key: String) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Get friend party
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!
            val userListProxy = CordaRPCClient(NetworkHostAndPort.parse("127.0.0.1:10005")).start("user1", "test").proxy
            val friend = userListProxy.vaultQuery(StateContract.UserState::class.java).states.filter { it: StateAndRef<StateContract.UserState> ->
                (it.state.data.login == friendLogin)
            }.singleOrNull()
            friend ?: throw FlowException("The friend is not registered")

            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            // Generate an unsigned transaction.
            val identity = Party(
                friend.state.data.participants[0].name,
                KeyFactory.getInstance("EdDSA")
                    .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(friend.state.data.partyKey)))
            )
            val identityProxy = CordaRPCClient(NetworkHostAndPort.parse(friend.state.data.partyAddress)).start("user1", "test").proxy

            val keyHash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(key.toByteArray()))
            val inputUserBear = builder {
                serviceHub.vaultService.queryBy(
                    StateContract.BearState::class.java,
                    criteria =
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(login)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::chars.equal(chars)))
                )
            }.states[0]

            // Load friend's bear
            val friendFlow = initiateFlow(identity)
            friendFlow.send(friendLogin)
            friendFlow.send(keyHash)
            subFlow(ReceiveTransactionFlow(friendFlow, true, StatesToRecord.ALL_VISIBLE))

            val inputFriendBear = builder {
                identityProxy.vaultQueryByCriteria<StateContract.BearState>(
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(friendLogin)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::keyHash.equal(keyHash))),
                    StateContract.BearState::class.java
                )
            }.states[0]
            val outputUserBear = StateContract.BearState(inputFriendBear.state.data.chars, login, "", ourIdentity)
            val outputFriendBear = StateContract.BearState(inputUserBear.state.data.chars, friendLogin, "", identity)

            val txCommand = Command(BearContract.Present(), listOf(ourIdentity.owningKey, identity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                .addCommand(txCommand)
            txBuilder.addInputState(inputUserBear)
            txBuilder.addInputState(inputFriendBear)
            txBuilder.addOutputState(outputUserBear, "com.template.contracts.BearContract")
            txBuilder.addOutputState(outputFriendBear, "com.template.contracts.BearContract")

            // Stage 2.
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(friendFlow)))

            // Stage 3.
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(BearSwapFlow::class)
    class BearSwapFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val login = otherPartySession.receive<String>().unwrap { it }
            val keyHash = otherPartySession.receive<String>().unwrap { it }

            val states = builder {
                serviceHub.vaultService.queryBy(
                        StateContract.BearState::class.java,
                        criteria =
                        VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                                .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(login)))
                                .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::keyHash.equal(keyHash)))
                )
            }.states
            val txhash = states[0].ref.txhash
            val stx = serviceHub.validatedTransactions.getTransaction(txhash)!!
            subFlow(SendTransactionFlow(otherPartySession, stx))

            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    "This consumes two items." using (ledgerTx.inputs.size == 2)
                    "This issues two items." using (ledgerTx.outputs.size == 2)
                    "This consumes bears." using (ledgerTx.inputs.all { it.state.data is StateContract.BearState })
                    "This issues bears." using (ledgerTx.outputs.all { it.data is StateContract.BearState })
                    val inputUserBear = ledgerTx.inputs[0].state.data as StateContract.BearState
                    val inputFriendBear = ledgerTx.inputs[1].state.data as StateContract.BearState
                    val outputUserBear = ledgerTx.outputs[0].data as StateContract.BearState
                    val outputFriendBear = ledgerTx.outputs[1].data as StateContract.BearState
                    "Login sets are equal." using (setOf(inputUserBear.ownerLogin, inputFriendBear.ownerLogin) == setOf(outputUserBear.ownerLogin, outputFriendBear.ownerLogin))
                    "The keys are reset." using (outputUserBear.keyHash.isEmpty() && outputFriendBear.keyHash.isEmpty())
                    "The characteristics match." using (
                        (inputUserBear.chars == outputFriendBear.chars && inputFriendBear.chars == outputUserBear.chars) ||
                        (inputUserBear.chars == outputUserBear.chars && inputFriendBear.chars == outputFriendBear.chars)
                    )
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}