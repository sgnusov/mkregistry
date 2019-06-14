package com.template.schemas

import net.corda.core.crypto.SecureHash
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import com.template.characteristics.Characteristics
import java.util.*
import javax.persistence.AttributeConverter
import javax.persistence.Convert
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

class CharacteristicsToStringConverter : AttributeConverter<Characteristics, String> {
    override fun convertToDatabaseColumn(chars: Characteristics): String {
        return "color=${chars.color}&hair=${chars.hair}&lips=${chars.lips}"
    }

    override fun convertToEntityAttribute(str: String): Characteristics {
        var color: Int = 0
        var hair: Int = 0
        var lips: Int = 0
        for (line in str.split('&')) {
            val name = line.split('=')[0]
            val value = line.split('=')[1]
            when (name) {
                "color" -> color = value.toInt()
                "hair" -> hair = value.toInt()
                "lips" -> lips = value.toInt()
            }
        }
        return Characteristics(color, hair, lips)
    }
}

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

        @Column(name = "chars")
        @Convert(converter = CharacteristicsToStringConverter::class)
        var chars: Characteristics,

        @Column(name = "keyHash")
        var keyHash: String,

        @Column(name = "owner")
        var ownerLogin: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", Characteristics(0, 0, 0), "", "")
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