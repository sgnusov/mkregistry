package com.template.states

import com.template.schemas.UserSchemaV1
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
    class UploadState(val hash: SecureHash,
                      val lender: Party,
                      val borrower: Party) : ContractState {
        override val participants get() = listOf(lender, borrower)


        /*override fun generateMappedObject(schema: MappedSchema): PersistentState {
            return when (schema) {
                is UploadSchemaV1 -> UploadSchemaV1.PersistentIOU(
                        this.lender.name.toString(),
                        this.borrower.name.toString(),
                        this.hash
                )
                else -> throw IllegalArgumentException("Unrecognised schema $schema")
            }
        }

        override fun supportedSchemas(): Iterable<MappedSchema> = listOf(UploadSchemaV1)*/
    }

    class SendState(val hash: SecureHash,
                    val name: String,
                    val description: String,
                    val lender: Party,
                    val borrower: Party) : ContractState {
        override val participants get() = listOf(lender, borrower)
    }

    class MarkState(val hash: SecureHash,
                    val name: String,
                    val mark: Int,
                    val lender: Party,
                    val borrower: Party) : ContractState {
        override val participants get() = listOf(lender, borrower)
    }

    class UserState(val login: String,
                    val password: String,
                    val adminRight: Boolean,
                    val uploadRight: Boolean,
                    val markRight: Boolean,
                    val sendRight: Boolean,
                    val lender: Party,
                    val borrower: Party): ContractState, QueryableState {
        override val participants get() = listOf(lender, borrower)

        override fun generateMappedObject(schema: MappedSchema): PersistentState {
            return when (schema) {
                is UserSchemaV1 -> UserSchemaV1.PersistentUser(
                        this.lender.name.toString(),
                        this.borrower.name.toString(),
                        this.login,
                        this.password,
                        this.adminRight,
                        this.uploadRight,
                        this.markRight,
                        this.sendRight
                )
                else -> throw IllegalArgumentException("Unrecognised schema $schema")
            }
        }

        override fun supportedSchemas(): Iterable<MappedSchema> = listOf(UserSchemaV1)
    }
}