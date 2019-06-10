package com.template.contracts

import com.template.states.StateContract
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class UploadContract : Contract {
    companion object {
        const val ID = "com.template.contracts.UploadContract"
    }

    class Create : CommandData

    override fun verify(tx: LedgerTransaction) {
        requireThat {

        }
    }
}

class SendContract : Contract {
    companion object {
        const val ID = "com.template.contracts.SendContract"
    }

    class Create : CommandData

    override fun verify(tx: LedgerTransaction) {

        requireThat {
        }
    }
}

class MarkContract : Contract {
    companion object {
        const val ID = "com.template.contracts.MarkContract"
    }

    class Create : CommandData

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
    class Change : CommandData
    class Delete : CommandData

    override fun verify(tx: LedgerTransaction) {

        requireThat {
        }
    }
}



