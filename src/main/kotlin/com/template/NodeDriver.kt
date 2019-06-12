package com.template

import com.template.web.SparkUI
import com.template.states.StateContract.UserState
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User

/**
 * Allows you to run your nodes through an IDE (as opposed to using deployNodes). Do not use in a production
 * environment.
 */
object NodeDriver {
    @JvmStatic
    fun main(args: Array<String>) {
        val rpcUsers = listOf(User("user1", "test", permissions = setOf("ALL")))


        driver(DriverParameters(startNodesInProcess = true, waitForAllNodesToFinish = true)) {
            startNode(providedName = CordaX500Name("UserList", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
            ///*
            startNode(providedName = CordaX500Name("PartyA", "London", "GB"), rpcUsers = rpcUsers).getOrThrow()
            startNode(providedName = CordaX500Name("PartyB", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
            startNode(providedName = CordaX500Name("PartyC", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
            startNode(providedName = CordaX500Name("PartyD", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
            startNode(providedName = CordaX500Name("PartyE", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
            startNode(providedName = CordaX500Name("PartyF", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
            //*/

            //*/

            SparkUI.main(arrayOf("1234", "localhost:10005"))

            ///*
            // SparkUpload.main(arrayOf("2345", "localhost:10009", "localhost:10005"))
            // SparkSend.main(arrayOf("3456", "localhost:10013", "localhost:10005"))
            // SparkMark.main(arrayOf(
            //         "4567", "localhost:10017",
            //         "5678", "localhost:10021",
            //         "6789", "localhost:10025",
            //         "localhost:10005"))
            // SparkViewResults.main(arrayOf("7890", "localhost:10029", "localhost:10005"))
        }

    }
}
