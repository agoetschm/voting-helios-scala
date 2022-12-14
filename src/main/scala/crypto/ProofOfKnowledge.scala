package crypto

import algebra.Domain
import algebra.Field
import algebra.Generator
import algebra.Group

import math.Integral.Implicits.infixIntegralOps

trait ProofOfKnowledge

object ProofOfKnowledge:
  // prove knowledge of x such that log_g h = x
  // https://www.zkdocs.com/docs/zkdocs/zero-knowledge-protocols/schnorr/
  def proveLog[
      Z: Integral,
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Group.Element[Z, E, F, B],
      G <: Group[Z, E, F, B, G],
      Gen <: Generator[Z, E, F, B, G],
      D <: Domain[Z, E, F, B, G, Gen]
  ](domain: D, base: B, h: B, x: E, random: () => E, hash: Seq[B] => E): ProofOfLog[Z, E, F, B] =
    val r         = random()
    val commit    = base ^ r
    val challenge = hash(Seq(h, commit))
    val solution  = r + (x * challenge)
    ProofOfLog(commit, challenge, solution)

  def verifyLog[
      Z: Integral,
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Group.Element[Z, E, F, B],
      G <: Group[Z, E, F, B, G],
      Gen <: Generator[Z, E, F, B, G],
      D <: Domain[Z, E, F, B, G, Gen]
  ](pol: ProofOfLog[Z, E, F, B], domain: D, base: B, h: B, hash: Seq[B] => E): Boolean =
    (pol.challenge == hash(Seq(h, pol.commit))) &&
      (base ^ pol.solution) == (pol.commit * (h ^ pol.challenge))

  // proof that m is either 0 or 1
  // is implemented with "Disjunctive proof of equality between discrete logs" cf Cortier2013
  def proofOfDisjunctiveLogEquality[
      Z: Integral,
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Group.Element[Z, E, F, B],
      G <: Group[Z, E, F, B, G],
      Gen <: Generator[Z, E, F, B, G]
  ](
      exponent: F,
      g: Gen,
      a: B,
      b: B,
      h: B,
      r: E,
      m: E,
      random: () => E,
      hash: Seq[B] => E
  ): ProofOfDisLog[Z, E, F, B] =
    val messageDomain: Set[E] =
      Set(exponent(Integral[Z].zero), exponent(Integral[Z].one))
    val rounds = for (i <- (messageDomain - m)) yield {
      val ci = random()
      val si = random()
      val ai = g(si) / (a ^ ci)
      val bi = (h ^ si) / ((b / g(i)) ^ ci)
      Round(i, (ai, bi), ci, si)
    }
    val w  = random()
    val aM = g(w)
    val bM = h ^ w

    val commits = rounds
      .map(round => (round.i, (round.commit._1, round.commit._2)))
      .union(Set((m, (aM, bM))))
      .toSeq
      .sortBy(_._1.z)
      .map(_._2)
    val hashCommit = hash(commits.map(ab => Seq(ab._1, ab._2)).flatten)

    val cM: E = hashCommit - rounds.map(_.challenge).reduce(_ + _)
    val sM    = w + (r * cM)
    val completedRounds: SizedSeq[Nat.N2.type, Round[Z, E, F, B]] =
      SizedSeq(
        Nat.N2,
        (rounds + Round(m, (aM, bM), cM, sM)).toSeq.sortBy(_.i.z)
      )
    ProofOfDisLog(completedRounds)

  def verifyDisjunctiveLogEquality[
      Z: Integral,
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Group.Element[Z, E, F, B],
      G <: Group[Z, E, F, B, G],
      Gen <: Generator[Z, E, F, B, G]
  ](
      field: F,
      g: Gen,
      a: B,
      b: B,
      h: B,
      poe: ProofOfDisLog[Z, E, F, B],
      hash: Seq[B] => E
  ): Boolean =
    poe.rounds
      .map { case Round(i, (ai, bi), ci, si) =>
        g(si) == ai * (a ^ ci) &&
        (h ^ si) == bi * ((b / g(i)) ^ ci)
      }
      .reduce(_ && _) &&
      hash(
        poe.rounds.underlying.map(_.commit).map(ab => Seq(ab._1, ab._2)).flatten
      ) ==
      poe.rounds.underlying.map(_.challenge).reduce(_ + _)

  case class ProofOfLog[
      Z: Integral,
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Group.Element[Z, E, F, B]
  ](commit: B, challenge: E, solution: E)
  case class Round[
      Z: Integral,
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Group.Element[Z, E, F, B]
  ](
      i: E,
      commit: (B, B),
      challenge: E,
      solution: E
  )
  case class ProofOfDisLog[
      Z: Integral,
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Group.Element[Z, E, F, B]
  ](
      rounds: SizedSeq[Nat.N2.type, Round[Z, E, F, B]]
  ) extends ProofOfKnowledge
