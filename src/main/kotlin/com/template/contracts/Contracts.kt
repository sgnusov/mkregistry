package com.template.contracts

import com.template.states.StateContract
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class BearIssueContract : Contract {
    companion object {
        const val ID = "com.template.contracts.BearIssueContract"
    }

    class Issue : CommandData

    override fun verify(tx: LedgerTransaction) {
        requireThat {
        }
    }
}

class UserContract : Contract {
    companion object {
        const val ID = "com.template.contracts.UserContract"
    }

    class Create : CommandData

    override fun verify(tx: LedgerTransaction) {

        requireThat {
        }
    }
}