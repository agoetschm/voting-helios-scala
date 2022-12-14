package demo

import algebra.DomainInt
import crypto.Nat
import crypto.SizedSeq
import demo.Named.Candidate
import demo.Named.Trustee
import demo.Named.Voter
import voting.scheme.HeliosDistributed.BulletinBoard

import State.Phase

final case class State[T <: Nat, V <: Nat, N <: Nat](
    domain: DomainInt,
    phase: Phase,
    bulletinBoard: BulletinBoard[T, V, N],
    trustees: SizedSeq[T, Trustee],
    voters: SizedSeq[V, Voter[N]],
    candidates: SizedSeq[N, Candidate]
)

object State:
  enum Phase:
    case Initial, Voting, Tallying, Finished
