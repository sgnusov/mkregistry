package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BearContract
import com.template.states.StateContract
import com.template.schemas.BearSchemaV1
import com.template.schemas.UserSchemaV1
import com.template.schemas.BearsExchangeSchemaV1
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
import sun.java2d.StateTrackable
import java.util.Random
import java.lang.Math
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory
import java.security.MessageDigest
import java.util.Base64

object BearFlows {
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

            val random = Random()
            for (i in 1..100) {
                var color = Math.abs(random.nextInt() % 256)
                val iouState = StateContract.BearState(color, login, ourIdentity, true)
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
        override fun call(): SignedTransaction {
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
                                .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::color.equal(color)))
                )
            }.states[0]
            val outputState = StateContract.BearState(color, receiverLogin, identity, true)
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
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
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


    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearMixFlow(val login: String, val color1: Int, val color2: Int) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Ask the userlist party to make sure there is a single user registered
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!
            val userListProxy = CordaRPCClient(NetworkHostAndPort.parse("127.0.0.1:10005")).start("user1", "test").proxy
            val user = builder {
                userListProxy.vaultQueryByCriteria<StateContract.UserState>(
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                                .and(QueryCriteria.VaultCustomQueryCriteria(UserSchemaV1.PersistentUser::login.equal(login))),
                        StateContract.UserState::class.java
                )
            }.states.singleOrNull()
            if (user == null) {
                throw FlowException("No such user")
            }

            val bears1 = builder {
                serviceHub.vaultService.queryBy(
                        StateContract.BearState::class.java,
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                                .and(QueryCriteria.VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(login)))
                                .and(QueryCriteria.VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::color.equal(color1)))
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
                                .and(QueryCriteria.VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::color.equal(color2)))
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

            val bearState = StateContract.BearState((color1 + color2) / 2, login, ourIdentity, true)
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
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    "This issues a bear." using (ledgerTx.outputs.single().data is StateContract.BearState)
                    val output = ledgerTx.outputs.single().data as StateContract.BearState
                    "This consumes 2 inputs." using (ledgerTx.inputs.size == 2)
                    "This consumes bears." using (ledgerTx.inputs.all { it.state.data is StateContract.BearState })
                    val bear1 = ledgerTx.inputs[0].state.data as StateContract.BearState
                    val bear2 = ledgerTx.inputs[1].state.data as StateContract.BearState
                    "This mixes correctly." using (output.color == (bear1.color + bear2.color) / 2)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }


    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearsExchangeInitFlow(val initializerLogin: String,
                               val receiverLogin: String,
                               val color: Int) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Ask the userlist for users parties
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!
            val userListProxy = CordaRPCClient(NetworkHostAndPort.parse("127.0.0.1:10005")).start("user1", "test").proxy

            val initializer = builder {
                userListProxy.vaultQueryByCriteria<StateContract.UserState>(
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                                .and(QueryCriteria.VaultCustomQueryCriteria(UserSchemaV1.PersistentUser::login.equal(initializerLogin))),
                        StateContract.UserState::class.java
                )
            }.states.singleOrNull()

            if (initializer == null) {
                throw FlowException("No such initializer")
            }

            val receiver = builder {
                userListProxy.vaultQueryByCriteria<StateContract.UserState>(
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                                .and(QueryCriteria.VaultCustomQueryCriteria(UserSchemaV1.PersistentUser::login.equal(receiverLogin))),
                        StateContract.UserState::class.java
                )
            }.states.singleOrNull()

            if (receiver == null) {
                throw FlowException("No such receiver")
            }

            // Generating input states

            val inputState = builder {
                serviceHub.vaultService.queryBy(
                        StateContract.BearState::class.java,
                        criteria =
                        VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                                .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(initializerLogin)))
                                .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::color.equal(color)))
                                .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::active.equal(true)))
                )
            }.states[0]

            val outputBearState = StateContract.BearState(
                    inputState.state.data.color,
                    initializerLogin,
                    initializer.state.data.registerer,
                    false
            )

            val outputBearsExchangeState = StateContract.BearsExchangeState(
                    initializerLogin,
                    initializer.state.data.registerer,
                    inputState.state.data.color,
                    receiverLogin,
                    receiver.state.data.registerer,
                    0,
                    false
            )

            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            // Generate an unsigned transaction.
            val txCommand = Command(BearContract.Exchange(), listOf(ourIdentity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)
                    .addInputState(inputState)
                    .addOutputState(outputBearState, "com.template.contracts.BearContract")
                    .addOutputState(outputBearsExchangeState, "com.template.contracts.BearContract")

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

    @InitiatedBy(BearsExchangeInitFlow::class)
    class BearsExchangeInitFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val input = ledgerTx.inputs.single().state.data
                    val output = ledgerTx.outputs
                    "This consumes a bear." using (input is StateContract.BearState)
                    "This issues two items." using (output.size == 2)
                    "This issues a bear and an exchange state." using (setOf(output.map {it::class}) == setOf(StateContract.BearState::class, StateContract.BearsExchangeState::class))
                    val inputBear = input as StateContract.BearState
                    var bear: StateContract.BearState? = null
                    if (output[0].data::class == StateContract.BearState::class)
                        bear = output[0].data as StateContract.BearState
                    else
                        bear = output[1].data as StateContract.BearState

                    var exchange: StateContract.BearsExchangeState? = null
                    if (output[0].data::class == StateContract.BearsExchangeState::class)
                        exchange = output[0].data as StateContract.BearsExchangeState
                    else
                        exchange = output[1].data as StateContract.BearsExchangeState
                    "This issues correct bear." using (
                        bear.issuer == inputBear.issuer
                        && bear.color == inputBear.color
                        && bear.ownerLogin == inputBear.ownerLogin
                        && bear.active == false
                    )
                    "This issues correct exchange state." using (
                        exchange.issuerLogin == inputBear.ownerLogin
                        && exchange.issuerParty == inputBear.issuer
                        && exchange.issuerBearColor == inputBear.color
                    )
                }
            }

            return subFlow(signTransactionFlow)
        }
    }



    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearsExchangeSuggestFlow(val initializerLogin: String,
                                  val initializerBearColor: Int,
                                  val receiverLogin: String,
                                  val receiverBearColor: Int) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Ask the userlist for users parties
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!
            val userListProxy = CordaRPCClient(NetworkHostAndPort.parse("127.0.0.1:10005")).start("user1", "test").proxy

            val initializer = builder {
                userListProxy.vaultQueryByCriteria<StateContract.UserState>(
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                        .and(QueryCriteria.VaultCustomQueryCriteria(UserSchemaV1.PersistentUser::login.equal(initializerLogin))),
                        StateContract.UserState::class.java
                )
            }.states.singleOrNull()

            if (initializer == null) {
                throw FlowException("No such initializer")
            }

            val receiver = builder {
                userListProxy.vaultQueryByCriteria<StateContract.UserState>(
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                        .and(QueryCriteria.VaultCustomQueryCriteria(UserSchemaV1.PersistentUser::login.equal(receiverLogin))),
                        StateContract.UserState::class.java
                )
            }.states.singleOrNull()

            if (receiver == null) {
                throw FlowException("No such receiver")
            }

            // Generating input states

            val inputBearState = builder {
                serviceHub.vaultService.queryBy(
                    StateContract.BearState::class.java,
                    criteria =
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(receiverLogin)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::color.equal(receiverBearColor)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::active.equal(true)))
                )
            }.states[0]

            val inputBearsExchangeState = builder {
                serviceHub.vaultService.queryBy(
                    StateContract.BearsExchangeState::class.java,
                    criteria =
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::initializerLogin.equal(initializerLogin)))
                    //.and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::initializer.equal(initializer.state.data.registerer.name)))
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::initializerBearColor.equal(initializerBearColor)))
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::receiverLogin.equal(receiverLogin)))
                    //.and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::receiver.equal(receiver.state.data.registerer)))
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::receiverBearColor.equal(0)))
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::accepted.equal(false)))
                )
            }.states.single()

            val outputBearState = StateContract.BearState(
                inputBearState.state.data.color,
                receiverLogin,
                receiver.state.data.registerer,
                false
            )

            val outputBearsExchangeState = StateContract.BearsExchangeState(
                    initializerLogin,
                    initializer.state.data.registerer,
                    initializerBearColor,
                    receiverLogin,
                    receiver.state.data.registerer,
                    receiverBearColor,
                    true
            )

            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            // Generate an unsigned transaction.
            val txCommand = Command(BearContract.Exchange(), listOf(ourIdentity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)
                    .addInputState(inputBearState)
                    .addInputState(inputBearsExchangeState)
                    .addOutputState(outputBearState, "com.template.contracts.BearContract")
                    .addOutputState(outputBearsExchangeState, "com.template.contracts.BearContract")

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

    @InitiatedBy(BearsExchangeSuggestFlow::class)
    class BearsExchangeSuggestFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val input = ledgerTx.inputs
                    val output = ledgerTx.outputs
                    "This consumes two items." using (output.size == 2)
                    "This consumes a bear and an exchange state." using (
                        setOf(output.map {it::class})
                        == setOf(StateContract.BearState::class, StateContract.BearsExchangeState::class)
                    )
                    "This issues two items." using (output.size == 2)
                    "This issues a bear and an exchange state." using (
                        setOf(output.map {it::class})
                        == setOf(StateContract.BearState::class, StateContract.BearsExchangeState::class)
                    )

                    var inputBear: StateContract.BearState? = null
                    if (input[0].state.data::class == StateContract.BearState::class)
                        inputBear = input[0].state.data as StateContract.BearState
                    else
                        inputBear = input[1].state.data as StateContract.BearState

                    var inputExchange: StateContract.BearsExchangeState? = null
                    if (input[0].state.data::class == StateContract.BearsExchangeState::class)
                        inputExchange = input[0].state.data as StateContract.BearsExchangeState
                    else
                        inputExchange = input[1].state.data as StateContract.BearsExchangeState

                    var outputBear: StateContract.BearState? = null
                    if (output[0].data::class == StateContract.BearState::class)
                        outputBear = output[0].data as StateContract.BearState
                    else
                        outputBear = output[1].data as StateContract.BearState

                    var outputExchange: StateContract.BearsExchangeState? = null
                    if (output[0].data::class == StateContract.BearsExchangeState::class)
                        outputExchange = output[0].data as StateContract.BearsExchangeState
                    else
                        outputExchange = output[1].data as StateContract.BearsExchangeState

                    "This issues correct bear." using (
                        outputBear.issuer == inputBear.issuer
                        && outputBear.color == inputBear.color
                        && outputBear.ownerLogin == inputBear.ownerLogin
                        && outputBear.active == false
                    )
                    "This issues correct exchange state." using (
                        outputExchange.issuerLogin == inputExchange.issuerLogin
                        && outputExchange.issuerParty == inputExchange.issuerParty
                        && outputExchange.issuerBearColor == inputExchange.issuerBearColor
                        && outputExchange.receiverLogin == inputExchange.receiverLogin
                        && outputExchange.receiverParty == inputExchange.receiverParty
                        && outputExchange.receiverBearColor == inputExchange.receiverBearColor
                        && outputExchange.receiverLogin == inputBear.ownerLogin
                        && outputExchange.receiverParty == inputBear.issuer
                        && outputExchange.receiverBearColor == inputBear.color

                    )
                }
            }

            return subFlow(signTransactionFlow)
        }
    }


    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class BearsExchangeAcceptFlow(val initializerLogin: String,
                                  val initializerBearColor: Int,
                                  val receiverLogin: String,
                                  val receiverBearColor: Int) : FlowLogic<SignedTransaction>() {
        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Ask the userlist for users parties
            val userListName = CordaX500Name("UserList", "New York", "US")
            val userListParty = serviceHub.networkMapCache.getPeerByLegalName(userListName)!!
            val userListProxy = CordaRPCClient(NetworkHostAndPort.parse("127.0.0.1:10005")).start("user1", "test").proxy

            val initializer = builder {
                userListProxy.vaultQueryByCriteria<StateContract.UserState>(
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                        .and(QueryCriteria.VaultCustomQueryCriteria(UserSchemaV1.PersistentUser::login.equal(initializerLogin))),
                        StateContract.UserState::class.java
                )
            }.states.singleOrNull()

            if (initializer == null) {
                throw FlowException("No such initializer")
            }

            val receiver = builder {
                userListProxy.vaultQueryByCriteria<StateContract.UserState>(
                        QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                        .and(QueryCriteria.VaultCustomQueryCriteria(UserSchemaV1.PersistentUser::login.equal(receiverLogin))),
                        StateContract.UserState::class.java
                )
            }.states.singleOrNull()

            if (receiver == null) {
                throw FlowException("No such receiver")
            }

            // Generating input states

            val inputBearState1 = builder {
                serviceHub.vaultService.queryBy(
                    StateContract.BearState::class.java,
                    criteria =
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(initializerLogin)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::color.equal(initializerBearColor)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::active.equal(false)))
                    )
            }.states[0]

            val identity = Party(receiver.state.data.participants[0].name,
                                 KeyFactory.getInstance("EdDSA")
                                    .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(receiver.state.data.partyKey))))
            val identityProxy = CordaRPCClient(NetworkHostAndPort.parse(receiver.state.data.partyAddress)).start("user1", "test").proxy

            val inputBearState2 = builder {
                identityProxy.vaultQueryByCriteria<StateContract.BearState>(
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::ownerLogin.equal(receiverLogin)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::color.equal(receiverBearColor)))
                    .and(VaultCustomQueryCriteria(BearSchemaV1.PersistentBear::active.equal(false))),
                    StateContract.BearState::class.java
                )
            }.states[0]

            val inputBearsExchangeState = builder {
                serviceHub.vaultService.queryBy(
                    StateContract.BearsExchangeState::class.java,
                    criteria =
                    VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::initializerLogin.equal(initializerLogin)))
                    //.and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::initializer.equal(initializerParty)))
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::initializerBearColor.equal(initializerBearColor)))
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::receiverLogin.equal(receiverLogin)))
                    //.and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::receiver.equal(receiverParty)))
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::receiverBearColor.equal(receiverBearColor)))
                    .and(VaultCustomQueryCriteria(BearsExchangeSchemaV1.PersistentBearsExchange::accepted.equal(true)))
                )
            }.states.single()

            val outputBearState1 = StateContract.BearState(
                inputBearState2.state.data.color,
                initializerLogin,
                initializer.state.data.registerer,
                true
            )

            val outputBearState2 = StateContract.BearState(
                inputBearState1.state.data.color,
                receiverLogin,
                receiver.state.data.registerer,
                true
            )

            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            // Generate an unsigned transaction.
            val txCommand = Command(BearContract.Exchange(), listOf(ourIdentity.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)
                    .addInputState(inputBearState1)
                    .addInputState(inputBearState2)
                    .addInputState(inputBearsExchangeState)
                    .addOutputState(outputBearState1, "com.template.contracts.BearContract")
                    .addOutputState(outputBearState2, "com.template.contracts.BearContract")

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

    @InitiatedBy(BearsExchangeAcceptFlow::class)
    class BearExchangeAccapetFlowResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val input = ledgerTx.inputs
                    val output = ledgerTx.outputs
                    "This consumes two items." using (input.size == 3)
                    "This consumes a bear and an exchange state." using (
                        setOf(input.map {it::class})
                        == setOf(StateContract.BearState::class,
                            StateContract.BearState::class,
                            StateContract.BearsExchangeState::class)
                    )
                    "This issues two items." using (output.size == 2)
                    "This issues bears" using (setOf(output.map {it::class}) == setOf(StateContract.BearState::class,
                        StateContract.BearState::class))

                    var inputBear1: StateContract.BearState? = null
                    if (input[0].state.data::class == StateContract.BearState::class)
                        inputBear1 = input[0].state.data as StateContract.BearState
                    else if (input[1].state.data::class == StateContract.BearState::class)
                        inputBear1 = input[1].state.data as StateContract.BearState
                    else
                        inputBear1 = input[2].state.data as StateContract.BearState

                    var inputBear2: StateContract.BearState? = null
                    if (input[0].state.data::class == StateContract.BearState::class && input[0].state.data != inputBear1)
                        inputBear2 = input[0].state.data as StateContract.BearState
                    else if (input[1].state.data::class == StateContract.BearState::class && input[1].state.data != inputBear1)
                        inputBear2 = input[1].state.data as StateContract.BearState
                    else
                        inputBear2 = input[2].state.data as StateContract.BearState

                    var inputExchange: StateContract.BearsExchangeState? = null
                    if (input[0].state.data::class == StateContract.BearsExchangeState::class)
                        inputExchange = input[0].state.data as StateContract.BearsExchangeState
                    else if (input[1].state.data::class == StateContract.BearsExchangeState::class)
                        inputExchange = input[1].state.data as StateContract.BearsExchangeState
                    else
                        inputExchange = input[2].state.data as StateContract.BearsExchangeState

                    var outputBear1: StateContract.BearState? = null
                    if (output[0].data::class == StateContract.BearState::class)
                        outputBear1 = output[0].data as StateContract.BearState
                    else if (output[1].data::class == StateContract.BearState::class)
                        outputBear1 = output[1].data as StateContract.BearState
                    else
                        outputBear1 = output[2].data as StateContract.BearState

                    var outputBear2: StateContract.BearState? = null
                    if (output[0].data::class == StateContract.BearState::class && output[0].data != outputBear1)
                        outputBear2 = output[0].data as StateContract.BearState
                    else if (output[1].data::class == StateContract.BearState::class && output[1].data != outputBear1)
                        outputBear2 = output[1].data as StateContract.BearState
                    else
                        outputBear2 = output[2].data as StateContract.BearState

                    if (outputBear1.issuer == inputBear1.issuer) {
                        var temp = outputBear1
                        outputBear1 = outputBear2
                        outputBear2 = temp
                    }

                    "This issues correct bears." using (
                        outputBear1.issuer == inputBear2.issuer
                        && outputBear1.color == inputBear1.color
                        && outputBear1.ownerLogin == inputBear2.ownerLogin
                        && outputBear1.active == true
                        && outputBear2.issuer == inputBear1.issuer
                        && outputBear2.color == inputBear2.color
                        && outputBear2.ownerLogin == inputBear1.ownerLogin
                        && outputBear2.active == true
                    )

                    if (outputBear1.issuer == inputExchange.receiverParty) {
                        var temp = outputBear1;

                    }

                    "This consumes correct exchange state." using (
                        inputExchange.issuerLogin == inputBear1.ownerLogin
                        && inputExchange.issuerParty == inputBear1.issuer
                        && inputExchange.issuerBearColor == inputBear1.color
                        && inputExchange.receiverLogin == inputBear2.ownerLogin
                        && inputExchange.receiverParty == inputBear2.issuer
                        && inputExchange.receiverBearColor == inputBear2.color
                        && inputExchange.accepted == true
                    )
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}