## Experimenting with voting schemes

**Disclaimer**: I'm aware of the [Don't Roll Your Own Crypto](https://security.stackexchange.com/questions/18197/why-shouldnt-we-roll-our-own) saying. This is just a pet project to understand how things work.

### Overview
E-voting schemes give guarantees such as ballot privacy (nobody can see what you voted) or universal verifiability (anybody can check that the voting result is correct). This repo contains a simplified implementation of the Helios 2.0 voting system from Adida _et al._

### Motivation
E-voting feels like a quite important topic for our society and is technically interesting.

After reading some papers and watching some videos, one can get an idea of the current state of research. But understanding how these protocols work and why they are secure or not requires a deeper dive. After getting a bit lost since the articles usually point to others which in turn refer to some obscure encryption scheme etc, I thought a bit of hands-on experimenting could be helpful.

### Getting started
```
sbt run <number of trustees> <number of voters> <number of candidates>
```
[![asciicast](https://asciinema.org/a/Rgv70p4IDDvcZ3x5yKEoHlI2B.svg)](https://asciinema.org/a/Rgv70p4IDDvcZ3x5yKEoHlI2B)

### Helios at a glance
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

This scheme preserves the secrecy of each ballot while being publicly verifiable, so everyone is convinced of the integrity of the voting result while learning nothing about the individual votes.

