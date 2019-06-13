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

            @Column(name = "keyHash")
            var keyHash: String,

            @Column(name = "owner")
            var ownerLogin: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", 0, "", "")
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