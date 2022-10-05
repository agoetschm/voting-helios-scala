package voting.scheme.helios

import algebra.DomainInt
import algebra.FieldInt
import algebra.GeneratorInt
import algebra.GroupInt
import crypto.ElGamal
import crypto.ElGamalPoK
import crypto.Hash
import crypto.Nat
import crypto.ProofOfKnowledge.ProofOfDisLog
import crypto.ProofOfKnowledge.ProofOfLog
import crypto.Random
import crypto.SizedSeq
import voting.scheme.definition.BallotProcessingError
import voting.scheme.definition.VotingScheme
import voting.scheme.helios.Helios.InvalidBallot

import math.BigInt.int2bigInt
import Helios._

/** @tparam N
  *   number of candidates in the election
  */
class Helios[N <: Nat]
    extends VotingScheme[
      SecretKey,
      PublicKey,
      Vote[N],
      Ballot[N],
      BulletinBoard[N],
      TallyResult[N],
      VotingResult[N],
      DomainInt,
      Random[Int]
    ]:

  private val encryption = ElGamal.onInt
  private val pok        = ElGamalPoK.onInt[N]

  override val setup =
    (domain: DomainInt, random: Random[Int]) =>
      val ((_, h), x) = encryption.gen(domain, random.on(domain))
      val proofOfGen =
        pok.proveGen((domain, h), (domain, x), random.on(domain), Hash.onDomain(domain))
      (SecretKey(domain, x), PublicKey(domain, h, proofOfGen), BulletinBoard(Seq.empty))

  def verifyPublicKey =
    (pubKey: PublicKey) =>
      pok.verifyGen((pubKey.domain, pubKey.h), pubKey.proofOfGen, Hash.onDomain(pubKey.domain))

  override val vote: (Vote[N], PublicKey, Random[Int]) => Ballot[N] =
    (vote: Vote[N], pubKey: PublicKey, random: Random[Int]) =>
      val domain = pubKey.domain
      val voteInDomain = vote.candidates
        .map(v => if (v) 1 else 0)
        .map(pubKey.domain.exponent(_))
      // r is required to calculate the proof of encryption,
      // so it has to be defined here
      val voteWithRandom: SizedSeq[N, (FieldInt.Element, FieldInt.Element)] =
        voteInDomain.map((_, random.on(domain)()))
      val encryptedVote =
        voteWithRandom.map((pt, r) =>
          encryption.enc(domain.generator(pt), (domain, pubKey.h), () => r)
        )
      val proofOfEncryption =
        pok.proveEnc(
          voteWithRandom,
          (domain, pubKey.h),
          encryptedVote,
          random.on(domain),
          Hash.onDomain(domain)
        )
      Ballot(encryptedVote, proofOfEncryption)

  val verifyBallot =
    (ballot: Ballot[N], pubKey: PublicKey) =>
      pok.verifyEnc(
        ballot.candidates,
        (pubKey.domain, pubKey.h),
        ballot.proofOfEncryption,
        Hash.onDomain(pubKey.domain)
      )

  override val cast =
    (ballot: Ballot[N], pubKey: PublicKey, bb: BulletinBoard[N]) =>
      // TODO check for duplicates
      if (verifyBallot(ballot, pubKey))
        Right(bb.copy(ballots = bb.ballots :+ ballot))
      else
        Left(InvalidBallot("proof verification failed"))

  override val tally =
    (bb: BulletinBoard[N], pubKey: PublicKey) =>
      require(
        bb.ballots.size < pubKey.domain.exponent.order,
        "the number of ballots cannot exceed the order of the exponent field"
      )
      val summedBallots = bb.ballots
        .map(_.candidates)
        .reduce((b1, b2) =>
          b1.zip(b2)
            .map(encryption.combine(_, _))
        )
      TallyResult(summedBallots)

  override val reveal =
    (bb: BulletinBoard[N], tallyResult: TallyResult[N], secKey: SecretKey, random: Random[Int]) =>
      val domain = secKey.domain
      val n      = bb.ballots.size
      val logTable: Map[GroupInt.Element, FieldInt.Element] =
        Range(0, n)
          .map(domain.exponent(_))
          .map(a => (domain.generator(a), a))
          .toMap

      val decrypted = tallyResult.candidates
        .map(encryptedVote => encryption.dec(encryptedVote, secKey.x))
        .map(logTable(_))
      val proofOfDecryption =
        pok.proveDec(
          tallyResult.candidates,
          (domain, secKey.x),
          decrypted,
          random.on(domain),
          Hash.onDomain(domain)
        )

      VotingResult(decrypted.map(_.z), proofOfDecryption)

  val verifyResult =
    (tallyResult: TallyResult[N], votingResult: VotingResult[N], pubKey: PublicKey) =>
      pok.verifyDec(
        tallyResult.candidates,
        votingResult.candidates.map(v => pubKey.domain.exponent(v)),
        (pubKey.domain, pubKey.h),
        votingResult.proofOfDecryption,
        Hash.onDomain(pubKey.domain)
      )

object Helios:

  case class SecretKey(domain: DomainInt, x: FieldInt.Element)

  case class PublicKey(
      domain: DomainInt,
      h: GroupInt.Element,
      proofOfGen: ProofOfLog[Int, FieldInt.Element, FieldInt, GroupInt.Element]
  )

  case class Vote[N <: Nat](candidates: SizedSeq[N, Boolean])
  case class Ballot[N <: Nat](
      candidates: SizedSeq[N, (GroupInt.Element, GroupInt.Element)],
      proofOfEncryption: (
          SizedSeq[
            N,
            ProofOfDisLog[Int, FieldInt.Element, FieldInt, GroupInt.Element]
          ],
          ProofOfDisLog[Int, FieldInt.Element, FieldInt, GroupInt.Element]
      )
  )

  case class BulletinBoard[N <: Nat](ballots: Seq[Ballot[N]])

  case class TallyResult[N <: Nat](candidates: SizedSeq[N, (GroupInt.Element, GroupInt.Element)])

  case class VotingResult[N <: Nat](
      candidates: SizedSeq[N, Int],
      proofOfDecryption: SizedSeq[N, ProofOfLog[Int, FieldInt.Element, FieldInt, GroupInt.Element]]
  )

  case class InvalidBallot(message: String) extends BallotProcessingError
