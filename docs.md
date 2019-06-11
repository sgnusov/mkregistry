# Abstract


## Required nodes

1. Notary
2. Network nodes
3. Userlist party

## Flows & contracts

### UserCreateFlow(val login: String, val salt: String, val pass_hash: String, val party: String)

*Handled by userlist party.*

Issues command with UserContract and UserState as output.

#### UserContract

The output state is UserState, no old username is reused; pass_hash starts with 000.


### BearMixFlow(val login: String, val a_characteristics: Int, val b_characteristics: Int)

*Handled by all nodes.*

Mixes bears A and B to get a new C bear; generates a "random" D bear. Optionally (depending on *all* function inputs), another "random" E bear is generated.

Issues command with BearMixContract; the old A's and B's BearStates are made historic, the new C's BearState and a new random D's/E's BearState are outputted.

#### BearMixContract

The two input states and the two/three output states are BearStates; the C, D and E bears are generated correctly.


### BearPresentFlow(val login_from: String, val characteristics: Int, val login_to: String)

*Handled by all nodes.*

Issues a command with BearPresentContract, and changes bear's owner.

#### BearPresentContract

The input and output state are BearStates; the input bear characteristics match the output bear.


### BearIssueFlow(val color: Int, val login: String)

*Handled by all nodes.*

Creates 100 bears. The userlist party must contain exactly one user registered.

#### BearIssueContract

The output state is a BearState.


## User registration

1. Find nonce to solve PoW (on client)
2. Choose party
3. Initiate UserCreateFlow(login, salt, salted password hash, party) with userlist party
4. Get confirmation or report an error
5. Initiate BearIssueFlow(random_color, login) if it is the first user registered
6. Get confirmation or report an error
7. Redirect the user to bear manager party

## Bear mixture

1. Get login; get a_characteristics and b_characteristics of old bears (on client)
3. Send bear mix request to a network node
4. Wait till the request is handled; if it is, output the mixture result

## Charity

1. Get login and characteristics; query destination login aka login_to (on client)
2. Initiate BearPresentFlow(login, characteristics, login_to) with a network node
3. Get confirmation or report an error