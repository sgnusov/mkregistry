package com.template.schemas

import net.corda.core.crypto.SecureHash
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for BearState.
 */
object BearSchema

/**
 * An BearState schema.
 */
object BearSchemaV1 : MappedSchema(
        schemaFamily = BearSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBear::class.java)
) {
    @Entity
    @Table(name = "iou_states")
    class PersistentBear(
            @Column(name = "issuer")
            var issuerName: String,

            @Column(name = "color")
            var color: Int,

            @Column(name = "owner")
            var ownerLogin: String,

            @Column(name = "active")
            var active: Boolean
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", 0, "", true)
    }
}

/**
 * The family of schemas for BearXchangeState.
 */
object BearExchangeSchema

/**
 * An BearsXchenge schema.
 */
object BearsExchangeSchemaV1 : MappedSchema(
        schemaFamily = BearExchangeSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBearsExchange::class.java)
) {
    @Entity
    @Table(name = "iou_states")
    class PersistentBearsExchange(
            @Column(name = "initializerLogin")
            var initializerLogin: String,

            @Column(name = "initializer")
            var initializer: String,

            @Column(name = "initializerBearColor")
            var initializerBearColor: Int,

            @Column(name = "receiverLogin")
            var receiverLogin: String,

            @Column(name = "receiver")
            var receiver: String,

            @Column(name = "receiverBearColor")
            var receiverBearColor: Int,

            @Column(name = "accepted")
            var accepted: Boolean
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", 0, "", "", 0, false)
    }
}

/**
 * The family of schemas for UserState.
 */
object UserSchema

/**
 * A UserState schema.
 */
object UserSchemaV1 : MappedSchema(
        schemaFamily = UserSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentUser::class.java)
) {
    @Entity
    @Table(name = "iou_states")
    class PersistentUser(
            @Column(name = "registerer")
            var registererName: String,

            @Column(name = "userlist")
            var userlistName: String,

            @Column(name = "login")
            var login: String,

            @Column(name = "password")
            var password: String,

            @Column(name = "party")
            var partyAddress: String,

            @Column(name = "partyKey")
            var partyKey: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this(
                "",
                "",
                "",
                "",
                "",
                ""
        )
    }
}