package voting.attack

import algebra.DomainInt
import crypto.Nat
import voting.scheme.helios.Helios.Ballot
import voting.scheme.helios.Helios.PublicKey

object WeakFiatShamir:
  def rerandomize[N <: Nat](
      ballot: Ballot[N],
      domain: DomainInt,
      pubKey: PublicKey
  ): Ballot[N] =
    val u          = domain.exponent(7) // TODO random
    val h          = pubKey.h
    val candidates = ballot.candidates.map((a, b) => (a * domain.generator(u), b * (h ^ u)))
    val proof = ballot.proofOfEncryption.copy(
      _1 = ballot.proofOfEncryption._1.map(poDisLog =>
        poDisLog.copy(
          rounds = poDisLog.rounds.map(round =>
            round.copy(
              solution = round.solution + (round.challenge * u)
            )
          )
        )
      ),
      _2 = ballot.proofOfEncryption._2.copy(
        rounds = ballot.proofOfEncryption._2.rounds.map(round =>
          round.copy(
            solution =
              round.solution + (round.challenge * u * domain.exponent(ballot.candidates.size.n))
          )
        )
      )
    )
    Ballot(candidates, proof)
