package com.template.states

import com.template.schemas.UserSchemaV1
import com.template.schemas.BearSchemaV1
import com.template.characteristics.Characteristics
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
    class BearState(val chars: Characteristics,
                    val ownerLogin: String,
                    val keyHash: String,
                    val issuer: Party) : ContractState, QueryableState {
        override val participants get() = listOf(issuer)

        override fun generateMappedObject(schema: MappedSchema): PersistentState {
            return when (schema) {
                is BearSchemaV1 -> BearSchemaV1.PersistentBear(
                        this.issuer.name.toString(),
                        this.chars,
                        this.keyHash,
                        this.ownerLogin
                )
                else -> throw IllegalArgumentException("Unrecognised schema $schema")
            }
        }

        override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BearSchemaV1)
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