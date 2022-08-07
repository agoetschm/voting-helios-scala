## Experimenting with voting schemes

**Disclaimer**: I'm aware of the [Don't Roll Your Own Crypto](https://security.stackexchange.com/questions/18197/why-shouldnt-we-roll-our-own) saying. This is just a pet project to understand how things work.

### Overview
 E-voting schemes give guarantees such as ballot privacy (nobody can see what you voted) or universal verifiability (anybody can check that the voting result is correct). This repo contains a simplified implementation of the Helios 2.0 voting system [Adida2009].

#### Motivation
E-voting feels like a quite important topic for our society and is technically interesting.

After reading some papers and watching some videos one can get an idea of the current state of research. But understanding how these protocols work and why they are secure or not requires a deeper dive. After getting a bit lost since the articles usually point to others which in turn refer to some obscure encryption scheme etc, I thought a bit of hands-on experimenting could be helpful.

#### Getting started
```
sbt run
```
The demo runs a mock election with 3 candidates and 10 voters. It then shows the content of the encrypted ballots and the corresponding proofs. A duplicated but valid ballot is also included, forged by a malicious voter using the re-randomization of the proof of encryption, which is based on a weak Fiat-Shamir transformation [Bernhard2012].

#### Helios at a glance
Voters cast encrypted ballots on a public bulletin board. The ballots can then be tallied without being individually decrypted, and the final result is revealed by decrypting the combination of ballots. Each step is paired with a corresponding proof of knowledge to convince an observer that it was properly executed.

```
                      Public bulletin board
 
                   +--------------------------+
                   |                          |
            cast   |  +--------------------+  |   tally
Vote 1  -----------+->| Encrypted ballot 1 +--+-------+
                   |  +--------------------+  |       |
                   |                          |       |
                   |  +--------------------+  |       |
Vote 2  -----------+->| Encrypted ballot 2 +--+-------+
                   |  +--------------------+  |       |
                   |                          |       |
 ...    -----------+->         ...         ---+-------+
                   |                          |       |
                   |  +--------------------+  |       |    +------------------+   reveal
Vote n  -----------+->| Encrypted ballot n +--+-------+--->| Encrypted result +------------> Result
                   |  +--------------------+  |            +------------------+
                   |                          |
                   +--------------------------+
  
      |                |                                                    |                |         
      +----------------+                                                    +----------------+
      Proof of encryption                                                   Proof of decryption
```

This scheme preserves the secrecy of each ballot while being publicly verifiable, so everyone is convinced of the integrity of the voting result while learning nothing about the individual votes. Well without taking the existing wearknesses in account...

### Implementation
The aim of this project is for the implementation to be as clear and simple as possible, in order to show how the different part of the scheme are put together. The strong typing of Scala helps to give expressive signatures to each function and make their role explicit.

The code is split in three main packages: `algebra`, `crypto` and `voting`. 

The `algebra` package is the lowest level. It contains the logic about cyclic groups and finite fields on which the encryption is based. Next comes the `crypto` package, where ElGamal and the proofs of knowledge are implemented. The `VotingScheme` trait and its implementation `Helios` then live in the `voting` package.

Note that the first two package aim to be "data type agnostic". Every class has a type parameter `Z: Integral` which can be any type representing an integer - such as `Int`, `Long` or `BigInt` - in order to handle different sizes of numbers.

### Helios


#### Encryption
- a vote is a sequence of 0's and 1's but at most one 1, indicating for which cantidate the vote is
  - for example: `0 1 0` is a vote for the second candidate
- each element of a vote is encrypted using ElGamal to form a ballot
- the ballots are tallied by multiplying the elements corresponding to the same candidate
- because ElGamal is homomorphic, the resulting product is the encryption of the sum of the votes
  - ElGamal.enc(a) * ElGamal.enc(b) = ElGamal.enc(a + b)
- the voting organizer who holds the key can reveal the result

Note that the current implementation does not handle mutliple trustees and distributed encryption (yet!): there is only one private key to reveal the voting result.
#### Proofs of knowledge
TODO

#### A possible attack
the consequence: duplicating ballot -> breach privacy if a ballot is duplicated enough time to make the effect observable on the result of the voting process

### References

- **[Adida2009]** B Adida, O De Marneffe, O Pereira, J Quisquater. **Electing a university president using open-audit voting: Analysis of real-world use of Helios**. 2009.
- **[Bernhard2011]** D Bernhard, V Cortier, O Pereira, B Smyth, B Warinschi. **Adapting Helios for provable ballot privacy**. 2011.
- **[Bernhard2012]** D Bernhard, O Pereira, B Warinschi. **How not to prove yourself: Pitfalls of the fiat-shamir heuristic and applications to helios**. 2012.
- **[Cortier2013]** V Cortier, B Smyth. **Attacking and fixing Helios: An analysis of ballot secrecy**. 2013.
