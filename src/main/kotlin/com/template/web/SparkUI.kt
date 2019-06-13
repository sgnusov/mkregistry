package com.template.web

import com.template.states.StateContract
import spark.ModelAndView
import spark.Service
import spark.Request
import spark.Spark.*
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import spark.template.freemarker.FreeMarkerEngine
import java.util.HashMap
import com.template.flows.UserFlows.UserCreateFlow
import com.template.flows.BearFlows.BearIssueFlow
import com.template.flows.BearFlows.BearPresentFlow
import com.template.flows.BearFlows.BearMixFlow
import com.template.schemas.UserSchemaV1
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.*
import net.corda.core.utilities.getOrThrow
import java.util.Random
import java.security.MessageDigest

data class Session(val login: String, val partyAddress: String)

object SparkUI {
    private val sessions: MutableMap<String, Session> = HashMap()
    private val partyProxies: MutableMap<String, CordaRPCOps> = HashMap<String, CordaRPCOps>()

    @JvmStatic
    fun main(args: Array<String>) {
        val random = Random()

        val freeMarkerEngine = SparkUI.initFreemarker(this::class.java)
        val http = Service.ignite().port(args.getOrNull(0)?.toInt() ?: 1357)
        http.staticFileLocation("/spark")

        val userListProxy = SparkUI.setConnection(args.getOrNull(1) ?: "localhost:10005")

        http.get("/") { req, _ ->
            val session = req.cookie("session")
            if (session == null || sessions[session] == null) {
                val model = hashMapOf("error" to "")
                freeMarkerEngine.render(ModelAndView(model, "SparkLogin.ftl"))
            } else {
                val login = sessions[session]!!.login
                val partyProxy = SparkUI.getPartyProxy(sessions.get(session)!!.partyAddress)
                val model: HashMap<String, Any> = hashMapOf("login" to login, "error" to "")
                // Load & add bears to model
                model["bears"] = partyProxy.vaultQuery(StateContract.BearState::class.java).states.filter { it: StateAndRef<StateContract.BearState> ->
                    (it.state.data.ownerLogin == login)
                }.map { it: StateAndRef<StateContract.BearState> -> it.state.data }
                freeMarkerEngine.render(ModelAndView(model, "SparkHome.ftl"))
            }
        }

        http.get("/register") { req, _ ->
            val model = hashMapOf("error" to "")
            freeMarkerEngine.render(ModelAndView(model, "SparkRegister.ftl"))
        }
        http.post("/register") { req, res ->
            val login = req.queryParamsValues("login").single()
            val password = req.queryParamsValues("password").single()
            val nonce = req.queryParamsValues("nonce").single()
            val partyAddress = req.queryParamsValues("party").single()
            val nonceInt = nonce.toInt(radix=16)

            val loginHash = MessageDigest.getInstance("SHA-256").digest(login.toByteArray())
            val challange = ByteArray(36)
            for (i in 0..31) {
                challange[i] = loginHash[i]
            }
            challange[32] = ((nonceInt shr 0) and 0xFF).toByte()
            challange[33] = ((nonceInt shr 8) and 0xFF).toByte()
            challange[34] = ((nonceInt shr 16) and 0xFF).toByte()
            challange[35] = ((nonceInt shr 24) and 0xFF).toByte()
            val digest = MessageDigest.getInstance("SHA-256").digest(challange)
            if (digest[0] > 0 || digest[1] > 1) {
                val model = hashMapOf("error" to "The challange is invalid.")
                freeMarkerEngine.render(ModelAndView(model, "SparkRegister.ftl"))
            } else {
                // Check that the user hasn't been registered yet
                val user = userListProxy.vaultQuery(StateContract.UserState::class.java).states.filter { it: StateAndRef<StateContract.UserState> ->
                    (it.state.data.login == login)
                }.singleOrNull()

                if (user != null) {
                    val model = hashMapOf("error" to "This login is already taken.")
                    freeMarkerEngine.render(ModelAndView(model, "SparkRegister.ftl"))
                } else {
                    val partyProxy = this.getPartyProxy(partyAddress)
                    try {
                        partyProxy.startFlow(::UserCreateFlow, login, password, partyAddress).returnValue.getOrThrow()
                    } catch (e: Exception) {
                        return@post e.toString()
                    }

                    // Check user count
                    val userCount = userListProxy.vaultQuery(StateContract.UserState::class.java).states.size
                    if (userCount == 1) {
                        val partyProxy = SparkUI.getPartyProxy(partyAddress)
                        partyProxy.startFlow(::BearIssueFlow, login).returnValue.getOrThrow()
                    }
                    res.redirect("/")
                }
            }
        }

        http.post("/login") { req, res ->
            val login = req.queryParamsValues("login").single()
            val password = req.queryParamsValues("password").single()
            val user = userListProxy.vaultQuery(StateContract.UserState::class.java).states.filter { it: StateAndRef<StateContract.UserState> ->
                (it.state.data.login == login) and (it.state.data.password == password)
            }.singleOrNull()

            if (user == null) {
                val model = hashMapOf("error" to "Incorrect login or password.")
                freeMarkerEngine.render(ModelAndView(model, "SparkLogin.ftl"))
            } else {
                val session = (random.nextInt() % 899999 + 100000).toString()
                sessions[session] = Session(login, user.state.data.partyAddress)
                res.cookie("session", session)
                res.redirect("/")
            }
        }

        http.post("/logout") { req, res ->
            val session = req.cookie("session")
            sessions.remove(session)
            res.removeCookie("session")
            res.redirect("/")
        }


        // Bear data
        http.get("/api/bears") { req, res ->
            val login = sessions[req.cookie("session")]!!.login
            val partyProxy = SparkUI.getPartyProxy(sessions[req.cookie("session")]!!.partyAddress)
            if (login == null) {
                res.redirect("/")
            } else {
                val bears = partyProxy.vaultQuery(StateContract.BearState::class.java).states.filter { it: StateAndRef<StateContract.BearState> ->
                    (it.state.data.ownerLogin == login)
                }
                var result = "Your bears: <br>"
                for (bear in bears) {
                    result += bear.state.data.color.toString() + "<br>"
                }
                return@get result
            }
        }

        http.post("/api/present") { req, res ->
            val login = sessions[req.cookie("session")]!!.login
            val partyProxy = SparkUI.getPartyProxy(sessions[req.cookie("session")]!!.partyAddress)
            val color = req.queryParamsValues("color").single().toInt()
            val receiverLogin = req.queryParamsValues("receiver").single()
            // Check that we have this bear
            val bear = partyProxy.vaultQuery(StateContract.BearState::class.java).states.filter { it: StateAndRef<StateContract.BearState> ->
                (it.state.data.ownerLogin == login && it.state.data.color == color)
            }[0]
            // Initiate BearPresentFlow
            partyProxy.startFlow(::BearPresentFlow, login, receiverLogin, color).returnValue.getOrThrow()
            res.redirect("/")
        }

        http.post("/api/mix") { req, res ->
            val login = sessions[req.cookie("session")]!!.login
            val partyProxy = SparkUI.getPartyProxy(sessions[req.cookie("session")]!!.partyAddress)
            val color1 = req.queryParamsValues("color1").single().toInt()
            val color2 = req.queryParamsValues("color2").single().toInt()
            // Check that we have this bears
            val bear1 = partyProxy.vaultQuery(StateContract.BearState::class.java).states.filter { it: StateAndRef<StateContract.BearState> ->
                (it.state.data.ownerLogin == login && it.state.data.color == color1)
            }[0]
            val bear2 = partyProxy.vaultQuery(StateContract.BearState::class.java).states.filter { it: StateAndRef<StateContract.BearState> ->
                (it.state.data.ownerLogin == login && it.state.data.color == color2)
            }[0]
            // Initiate BearPresentFlow
            val result = partyProxy.startFlow(::BearMixFlow, login, color1, color2).returnValue.getOrThrow()
            val newBear = result.tx.outputStates[0] as StateContract.BearState

            val model = hashMapOf("color" to newBear.color)
            freeMarkerEngine.render(ModelAndView(model, "SparkNewBear.ftl"))
        }
    }

    fun initFreemarker(resourceLoaderClass: Class<*>): FreeMarkerEngine
    {
        val freeMarkerEngine = FreeMarkerEngine()
        val freeMarkerConfiguration = Configuration()
        freeMarkerConfiguration.setTemplateLoader(ClassTemplateLoader(resourceLoaderClass, "/templates/"))
        freeMarkerEngine.setConfiguration(freeMarkerConfiguration)
        return freeMarkerEngine
    }

    fun setConnection(hostAndPort: String, username: String = "user1", password: String = "test"): CordaRPCOps
    {
        val nodeAddress = NetworkHostAndPort.parse(hostAndPort)
        val rpcConnection = CordaRPCClient(nodeAddress).start(username, password)

        return rpcConnection.proxy
    }

    fun getPartyProxy(hostAndPort: String): CordaRPCOps
    {
        val proxy = this.partyProxies[hostAndPort] ?: SparkUI.setConnection(hostAndPort)
        this.partyProxies[hostAndPort] = proxy
        return proxy
    }
}