package com.template.web

import com.template.states.StateContract
import spark.ModelAndView
import spark.Service
import java.util.HashMap
import com.template.flows.UserFlows.UserChangeFlow
import com.template.flows.UserFlows.UserCreateFlow
import com.template.flows.UserFlows.UserDeleteFlow
import com.template.schemas.UserSchemaV1
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.*
import net.corda.core.utilities.getOrThrow

object SparkAdmin {

    @JvmStatic
    fun main(args: Array<String>) {
        val freeMarkerEngine = SparkUpload.initFreemarker(this::class.java)
        val http = Service.ignite().port(args.getOrNull(0)?.toInt()?:1357)
        http.staticFileLocation("/spark")

        val proxy = SparkUpload.setConnection(args.getOrNull(1) ?: "localhost:10005")
        val model = HashMap<String, Any>()


        model["registered"] = false
        model["admin"] = false

        http.get("/") { req, _ ->
            freeMarkerEngine.render(ModelAndView(model, "SparkAdmin.ftl"))
        }

        http.post("/register") {req, res ->
            val hasParams = !req.queryParams().isEmpty()

            if (hasParams) {
                val userName = req.queryParamsValues("uname").single()
                val userPassword = req.queryParamsValues("upass").single()
                val user = proxy.vaultQuery(StateContract.UserState::class.java).states.filter{
                    it -> (it.state.data.login == userName)
                }.singleOrNull()

                if (user != null) {
                    return@post "<h1>Username already taken</h1>"
                }



                val results = builder {
                    proxy.vaultQueryBy<StateContract.UserState>(
                            criteria =
                            VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                            .and(VaultCustomQueryCriteria(UserSchemaV1.PersistentUser::adminRight.equal(true)))
                    )
                }

                val hasOrHadAdmin = results.states.size == 1

                proxy.startFlow(::UserCreateFlow, userName, userPassword, !hasOrHadAdmin).returnValue.getOrThrow()



                val newUser = proxy.vaultQuery(StateContract.UserState::class.java).states.filter{
                    it -> (it.state.data.login == userName)
                }.single()

                model["username"] = userName
                model["registered"] = true
                model["admin"] = newUser.state.data.adminRight


                if (model["admin"] == true) {
                    model["userList"] = proxy.vaultQuery(StateContract.UserState::class.java).states
                            .associateBy (
                                {it.state.data.login},
                                {listOf(it.state.data.uploadRight, it.state.data.sendRight, it.state.data.markRight)}
                            )
                }


                res.redirect("/")
            }
            else {
                "<h1>Registration failed</h1>"
            }
        }

        http.post("/login") {req, res ->
            val hasParams = !req.queryParams().isEmpty()

            if (hasParams) {
                val userName = req.queryParamsValues("uname").single()
                val userPassword = req.queryParamsValues("upass").single()
                val user = proxy.vaultQuery(StateContract.UserState::class.java).states.filter{
                    it -> (it.state.data.login == userName) and (it.state.data.password == userPassword)
                }.singleOrNull()

                user?:return@post "<h1>Username or password incorrect</h1>"

                model["username"] = userName
                model["registered"] = true
                model["admin"] = user.state.data.adminRight

                if (model["admin"] == true) {
                    model["userList"] = proxy.vaultQuery(StateContract.UserState::class.java).states
                            .associateBy (
                                    {it.state.data.login},
                                    {listOf(it.state.data.uploadRight, it.state.data.sendRight, it.state.data.markRight)}
                            )
                }


                res.redirect("/")
            }
            else {
                "<h1>Login failed</h1>"
            }
        }

        http.post("/logout") {req, res ->
            logout(model)


            res.redirect("/")
        }

        http.post("/change_rules") { req, res->
            val userName = req.queryParamsValues("username").single()

            val canUpload = "1" in req.queryParams()
            val canSend = "2" in req.queryParams()
            val canMark = "3" in req.queryParams()

            proxy.startFlow(::UserChangeFlow, userName, canUpload, canSend, canMark).returnValue.getOrThrow()


            if (model["admin"] == true) {
                model["userList"] = proxy.vaultQuery(StateContract.UserState::class.java).states
                        .associateBy (
                                {it.state.data.login},
                                {listOf(it.state.data.uploadRight, it.state.data.sendRight, it.state.data.markRight)}
                        )
            }


            res.redirect("/")
        }

        http.post("/delete_user") { req, res->
            val userName = req.queryParamsValues("username").single()

            proxy.startFlow(::UserDeleteFlow, userName).returnValue.getOrThrow()

            if (model["username"] == userName)
            {
                logout(model)
            }
            else if (model["admin"] == true) {
                model["userList"] = proxy.vaultQuery(StateContract.UserState::class.java).states
                        .associateBy (
                                {it.state.data.login},
                                {listOf(it.state.data.uploadRight, it.state.data.sendRight, it.state.data.markRight)}
                        )
            }


            res.redirect("/")
        }
    }

    fun logout(model: HashMap<String, Any>)
    {
        model["username"] = ""
        model["registered"] = false
        model["admin"] = false
    }

}