package com.template.states

import com.template.schemas.UserSchemaV1
import com.template.schemas.BearSchemaV1
import com.template.schemas.BearsExchangeSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*
import javax.management.Query

// *********
// * State *
// *********
object StateContract {
    class BearState(val color: Int,
                    val ownerLogin: String,
                    val issuer: Party,
                    val active: Boolean) : ContractState, QueryableState {
        override val participants get() = listOf(issuer)

        override fun generateMappedObject(schema: MappedSchema): PersistentState {
            return when (schema) {
                is BearSchemaV1 -> BearSchemaV1.PersistentBear(
                        this.issuer.name.toString(),
                        this.color,
                        this.ownerLogin,
                        this.active
                )
                else -> throw IllegalArgumentException("Unrecognised schema $schema")
            }
        }

        override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BearSchemaV1)
    }

    class BearsExchangeState(val issuerLogin: String,
                             val issuerParty: Party,
                             val issuerBearColor: Int,
                             val receiverLogin: String,
                             val receiverParty: Party,
                             val receiverBearColor: Int,
                             var accepted: Boolean) : ContractState, QueryableState {
        override val participants get() = listOf(issuerParty, receiverParty)

        override fun generateMappedObject(schema: MappedSchema): PersistentState {
            return when (schema) {
                is BearsExchangeSchemaV1 -> BearsExchangeSchemaV1.PersistentBearsExchange(
                        this.issuerLogin,
                        this.issuerParty.name.toString(),
                        this.issuerBearColor,
                        this.receiverLogin,
                        this.receiverParty.name.toString(),
                        this.receiverBearColor,
                        this.accepted
                )
                else -> throw IllegalArgumentException("Unrecognised schema $schema")
            }
        }

        override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BearsExchangeSchemaV1)
    }

    class UserState(val login: String,
                    val password: String,
                    val partyAddress: String,
                    val partyKey: String,
                    val registerer: Party,
                    val userlist: Party): ContractState, QueryableState {
        override val participants get() = listOf(registerer, userlist)

        override fun generateMappedObject(schema: MappedSchema): PersistentState {
            return when (schema) {
                is UserSchemaV1 -> UserSchemaV1.PersistentUser(
                        this.registerer.name.toString(),
                        this.userlist.name.toString(),
                        this.login,
                        this.password,
                        this.partyAddress,
                        this.partyKey
                )
                else -> throw IllegalArgumentException("Unrecognised schema $schema")
            }
        }

        override fun supportedSchemas(): Iterable<MappedSchema> = listOf(UserSchemaV1)
    }
}