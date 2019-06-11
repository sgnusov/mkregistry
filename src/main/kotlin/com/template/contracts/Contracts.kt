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
            "This doesn't consume any bears." using (tx.inputs.isEmpty())
            for (output in tx.outputs) {
                "This issues a bear." using (output.data is StateContract.BearState)
            }
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
            "This doesn't consume any users." using (tx.inputs.isEmpty())
            "This creates a single user." using (tx.outputs.single().data is StateContract.UserState)
        }
    }
}