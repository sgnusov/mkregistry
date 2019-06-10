package com.template.web

import com.template.flows.MarkFlows.MarkFlow
import com.template.states.StateContract
import com.template.web.SparkUpload.setConnection
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.CordaRPCOps
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import net.corda.core.utilities.getOrThrow
import spark.ModelAndView
import spark.Service
import spark.template.freemarker.FreeMarkerEngine
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashMap
import java.util.jar.JarInputStream

object SparkMark {

    @JvmStatic
    fun main(args: Array<String>) {
        val freeMarkerEngine = SparkUpload.initFreemarker(this::class.java)

        val admin = setConnection(args.lastOrNull() ?: "localhost:10005")

        for (arg in 0 until args.size/2)
        {
            val http = Service.ignite().port(args.getOrNull(arg*2)?.toInt()?:3456)
            http.staticFileLocation("/spark")

            val proxy = setConnection(args.getOrNull(arg * 2 + 1) ?: "localhost:10013")
            val model = HashMap<String, Any>()
            model["registered"] = false

            http.post("/login") {req, res ->

                val hasParams = !req.queryParams().isEmpty()

                if (hasParams) {
                    val userName = req.queryParamsValues("uname").single()
                    val userPassword = req.queryParamsValues("upass").single()

                    val user = admin.vaultQuery(StateContract.UserState::class.java).states.filter{
                        it -> (it.state.data.login == userName) and (it.state.data.password == userPassword)
                    }.singleOrNull()

                    user?:return@post "<h1>Username or password incorrect/h1>"

                    model["username"] = userName
                    model["registered"] = true
                    model["right"] = user.state.data.markRight


                    res.redirect("/")
                }
                else {
                    "<h1>Login failed</h1>"
                }
            }

            http.post("/logout") {req, res ->
                SparkAdmin.logout(model)

                res.redirect("/")

            }

            http.get("/"
            ) { req, _ ->

                val vaultData = proxy.vaultQuery(StateContract.SendState::class.java)
                val hasParams = !req.queryParams().isEmpty()

                val dataArray = vaultData.states.associateBy({it.state.data.hash}, {it.state.data.description})
                val arr = vaultData.states.groupBy ({it.state.data.hash}, {it.state.data.description})



                val status = MutableList(dataArray.keys.size, {_ -> ""})
                model["hashArray"] = dataArray.keys

                if (hasParams) {
                    val attachmentHash = SecureHash.parse(req.queryParamsValues("hash")[0])

                    val attachmentDownloadInputStream = proxy.openAttachment(attachmentHash)
                    val inpStr =  JarInputStream(attachmentDownloadInputStream)

                    inpStr.use { jar ->
                        while (true) {
                            val nje = jar.nextEntry ?: break
                            if (nje.isDirectory) {
                                continue
                            }
                            val dir = Paths.get(System.getProperty("user.home"))
                            val destFile = dir.toAbsolutePath().resolve(nje.name)

                            Files.newOutputStream(destFile).use {
                                jar.copyTo(it)
                            }
                        }
                    }

                    val input = dataArray[attachmentHash]

                    val dir = Paths.get(System.getProperty("user.home"))
                    val destFile = dir.toAbsolutePath().resolve("test.txt")

                    Files.newOutputStream(destFile).use {
                        it.write(input!!.toByteArray())
                    }

                    status[dataArray.keys.indexOf(attachmentHash)] = "Скачано"
                }

                model["status"] = status

                freeMarkerEngine.render(ModelAndView(model, "SparkMark.ftl"))
            }

            http.post("/") { req, res ->
                val mark = req.queryParamsValues("mark")[0]
                val hash = SecureHash.parse(req.queryParamsValues("hash")[0])


                val vaultData = proxy.vaultQuery(StateContract.SendState::class.java)
                val dataArray = vaultData.states.associateBy({it->it.state.data.hash}, { it->it.state.data.name})
                val hashArray = dataArray.keys

                proxy.startFlow(::MarkFlow, hash, dataArray[hash]!!, mark.toInt())
                     .returnValue.getOrThrow()

                model["hashArray"] = proxy.vaultQuery(StateContract.SendState::class.java).states.map { it -> it.state.data.hash }
                model["status"] = MutableList(dataArray.keys.size, {_ -> ""})


                res.redirect("/")
            }
        }
    }
}