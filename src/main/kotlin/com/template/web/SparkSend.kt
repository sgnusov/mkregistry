package com.template.web

import com.template.flows.SendFlow.SendInfoFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import com.template.states.StateContract
import com.template.web.SparkUpload.setConnection
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

object SparkSend {

    @JvmStatic
    fun main(args: Array<String>) {
        val freeMarkerEngine = SparkUpload.initFreemarker(this::class.java)


        val http = Service.ignite().port(args.getOrNull(0)?.toInt()?:2345)

        http.staticFileLocation("/spark")

        val admin = setConnection(args.lastOrNull() ?: "localhost:10005")
        val proxy = setConnection(args.getOrNull(1) ?: "localhost:10009")

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

                user?:return@post "<h1>Username or password incorrect</h1>"

                model["username"] = userName
                model["registered"] = true
                model["right"] = user.state.data.sendRight


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


            val vaultData = proxy.vaultQuery(StateContract.UploadState::class.java)
            val hasParams = !req.queryParams().isEmpty()

            val hashArray = vaultData.states.map{it -> it.state.data.hash}
            val status = MutableList(hashArray.size, {_ -> ""})

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

                status[hashArray.indexOf(attachmentHash)] = "Скачано"
            }



            model["status"] = status
            model["hashArray"] = hashArray

            freeMarkerEngine.render(ModelAndView(model, "SparkSend.ftl"))
        }

        http.post("/") { req, res ->
            val name = req.queryParamsValues("name")[0]
            val hash = SecureHash.parse(req.queryParamsValues("hash")[0])
            val description = req.queryParamsValues("desc")[0]

            proxy.startFlow(::SendInfoFlow, hash, name, description)
                 .returnValue.getOrThrow()

            val vaultData = proxy.vaultQuery(StateContract.UploadState::class.java)
            val hashArray = vaultData.states.map{it -> it.state.data.hash}

            model["hashArray"] = hashArray
            model["status"] = List(hashArray.size, {_ -> ""})

            res.redirect("/")
        }
    }
}