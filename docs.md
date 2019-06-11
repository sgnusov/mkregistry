# Abstract


## Required nodes

1. Notary
2. Network nodes
3. Userlist party

## Flows & contracts

### UserRegisterFlow(val login: String, val salt: String, val pass_hash: String, val party: String)

*Handled by userlist party.*

Issues command with UserRegisterContract and UserState as output.

#### UserRegisterContract

The output state is UserState, no old username is reused; pass_hash starts with 000.


### BearMixFlow(val login: String, val a_id: Int, val b_id: Int, val c_id: Int)

*Handled by all nodes.*

Mixes bears A and B to get a new C bear; generates a "random" D bear. Optionally (depending on *all* function inputs), another "random" E bear is generated.

Issues command with BearMixContract; the old A's and B's BearStates are made historic, the new C's BearState and a new random D's/E's BearState are outputted.

#### BearMixContract

The two input states and the two/three output states are BearStates; the C, D and E bears are generated correctly.


### BearPresentFlow(val login_from: String, val id: Int, val login_to: String)

*Handled by all nodes.*

First, id_to = `max(ids) + 1` of "login_to" user is received by sending a request to "login_to" party. Then, issue a command with BearPresentContract, and change bear's owner and id.

#### BearPresentContract

The input and output state are BearStates; the input bear characteristics match the output bear.


### BalanceInitFlow(val login: String)

*Handled by userlist party.*

Creates 10 "random" (hardcoded in fact) Bears for "login" user. This is internally implemented as a bear output and a BearIssueContract, signed by userlist party. The request is sent to network nodes.

#### BearIssueContract

The output states are all BearStates.


## User registration

1. Find nonce to solve PoW (on client)
2. Choose party
3. Initiate UserRegisterFlow(login, salt, salted password hash, party) with userlist party
4. Get confirmation or report an error
5. UserRegisterFlow might initiate BalanceInitFlow(login) as a subflow if it is the first user registered
6. Get confirmation or report an error
7. Redirect the user to bear manager party

## Bear mixture

1. Get login; get a_id and b_id of old bears (on client)
2. Generate c_id via `max(ids) + 1` (on client)
3. Send bear mix request to a network node
4. Wait till the request is handled; if it is, output the mixture result

## Charity

1. Get login and id; query destination login aka login_to (on client)
2. Initiate BearPresentFlow(login, id, login_to) with a network node
3. Get confirmation or report an error