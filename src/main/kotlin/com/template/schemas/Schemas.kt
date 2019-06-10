package com.template.schemas

import net.corda.core.crypto.SecureHash
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/*/**
 * The family of schemas for UploadState.
 */
object UploadSchema

/**
 * An UploadState schema.
 */
object UploadSchemaV1 : MappedSchema(
        schemaFamily = UploadSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentIOU::class.java)
) {
    @Entity
    @Table(name = "iou_states")
    class PersistentIOU(
            @Column(name = "lender")
            var lenderName: String,

            @Column(name = "borrower")
            var borrowerName: String,

            @Column(name = "hash")
            var hash: SecureHash
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", SecureHash.zeroHash)
    }
}*/

/**
 * The family of schemas for UploadState.
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
            @Column(name = "lender")
            var lenderName: String,

            @Column(name = "borrower")
            var borrowerName: String,

            @Column(name = "login")
            var login: String,

            @Column(name = "password")
            var password: String,

            @Column(name = "adminRight")
            var adminRight: Boolean,

            @Column(name = "uploadRight")
            var uploadRight: Boolean,

            @Column(name = "sendRight")
            var sendRight: Boolean,

            @Column(name = "markRight")
            var markRight: Boolean
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this(
                "",
                "",
                "",
                "",
                false,
                false,
                false,
                false
        )
    }
}