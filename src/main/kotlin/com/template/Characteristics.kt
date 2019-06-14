package com.template.characteristics

import java.util.Random
import javax.persistence.Entity
import net.corda.core.serialization.CordaSerializable
import java.lang.Math.abs


val random = Random()

@Entity
@CordaSerializable
data class Characteristics(val color: Int,
                           val hair: Int,
                           val lips: Int) {
    fun mix(other: Characteristics): Characteristics {
        return Characteristics(
            (color + other.color) / 2,
            (hair + other.hair) / 2,
            (lips + other.lips) / 2
        )
    }

    companion object CharacteristicsInitializer {
        fun random(): Characteristics {
            val color = abs(random.nextInt()) % 256
            val hair = abs(random.nextInt()) % 256
            val lips = abs(random.nextInt()) % 256
            return Characteristics(color=color, hair=hair, lips=lips)
        }
    }
}