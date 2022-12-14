package voting.scheme

import algebra.DomainInt
import algebra.FieldInt
import algebra.GroupInt
import crypto.ElGamal
import crypto.ElGamalPoK
import crypto.Hash
import crypto.Nat
import crypto.ProofOfKnowledge
import crypto.ProofOfKnowledge.ProofOfDisLog
import crypto.ProofOfKnowledge.ProofOfLog
import crypto.Random
import crypto.SizedSeq

import HeliosDistributed.*

/** Implementation of Helios with distributed key generation and decryption among T trustees. Refer
  * to Cortier2013 for details.
  *
  * @tparam T
  *   number of trustees
  * @tparam V
  *   number of voters
  * @tparam N
  *   number of candidates in the election
  */
class HeliosDistributed[T <: Nat, V <: Nat, N <: Nat](domain: DomainInt, numberOfCandidates: N)
    extends DistributedVotingScheme[
      Random[Int],
      SecretKeyShare,
      PublicKeyShare,
      PublicKey,
      BulletinBoard[T, V, N],
      Vote[N],
      Ballot[N],
      TallyResult[N],
      PartialResult[N],
      VotingResult[N],
      TrusteeId,
      VoterId
    ]:
  val underlying         = new Helios[N]
  private val encryption = ElGamal.onInt
  private val pok        = ElGamalPoK.onInt[N]

  override val generateKeyShare =
    (random: Random[Int]) =>
      val ((_, h), x) = encryption.gen(domain, random.on(domain))
      val proofOfGen =
        pok.proveGen((domain, h), (domain, x), random.on(domain), Hash.onDomain(domain))
      (SecretKeyShare(x), PublicKeyShare(h, proofOfGen))

  override val publishKeyShare =
    (trusteeId: TrusteeId, pubKeyShare: PublicKeyShare, bb: BulletinBoard[T, V, N]) =>
      bb.copy(
        publicKeyShares = bb.publicKeyShares.zipWithIndex.map((share, i) =>
          if (i == trusteeId.id) Some(pubKeyShare)
          else share
        )
      )

  override val verifyPublicKeyShares: BulletinBoard[T, V, N] => Option[Boolean] =
    (bb: BulletinBoard[T, V, N]) =>
      bb.publicKeyShares.foldLeft(Some(true): Option[Boolean]) {
        case (None, _) | (_, None) => None
        case (Some(arePreviousSharesValid), Some(share)) =>
          Some(
            arePreviousSharesValid &&
              underlying.verifyPublicKey(Helios.PublicKey(domain, share.h, share.proof))
          )
      }

  override val retrievePublicKey: BulletinBoard[T, V, N] => Option[PublicKey] =
    (bb: BulletinBoard[T, V, N]) =>
      bb.publicKeyShares
        .foldLeft(Some(domain.base(1)): Option[GroupInt.Element]) {
          case (None, _) | (_, None) => None
          case (Some(hAcc), Some(share)) =>
            Some(hAcc * share.h)
        }
        .map(PublicKey(_))

  override val prepareBallot: (Vote[N], PublicKey, Random[Int]) => Ballot[N] =
    (vote: Vote[N], pubKey: PublicKey, random: Random[Int]) =>
      underlying.vote(vote, pubKey.underlying(domain), random)
  override val castBallot: (VoterId, Ballot[N], BulletinBoard[T, V, N]) => BulletinBoard[T, V, N] =
    (voterId: VoterId, newBallot: Ballot[N], bb: BulletinBoard[T, V, N]) =>
      bb.copy(
        ballots = bb.ballots.zipWithIndex.map((ballot, i) =>
          if (i == voterId.id) Some(newBallot)
          else ballot
        )
      )
  override val verifyBallots: BulletinBoard[T, V, N] => Option[Boolean] =
    (bb: BulletinBoard[T, V, N]) =>
      retrievePublicKey(bb).flatMap(pubKey =>
        bb.ballots.foldLeft(Some(true): Option[Boolean]) {
          case (None, _) | (_, None) => None
          case (Some(arePreviousBallotsValid), Some(ballot)) =>
            Some(
              arePreviousBallotsValid &&
                underlying.verifyBallot(ballot, pubKey.underlying(domain))
            )
        }
      )

  override val combineBallots: BulletinBoard[T, V, N] => Option[TallyResult[N]] =
    (bb: BulletinBoard[T, V, N]) =>
      bb.ballots.foldLeft(
        Some(
          TallyResult(SizedSeq.fill(numberOfCandidates, (domain.base(1), domain.base(1))))
        ): Option[TallyResult[N]]
      ) {
        case (None, _) | (_, None) => None
        case (Some(TallyResult(accCandidates)), Some(Helios.Ballot(candidates, _))) =>
          Some(TallyResult(accCandidates.zip(candidates).map(encryption.combine(_, _))))
      }
  override val revealPartialResult
      : (TallyResult[N], SecretKeyShare, Random[Int]) => PartialResult[N] =
    (tallyResult: TallyResult[N], secKeyShare: SecretKeyShare, random: Random[Int]) =>
      val partialDecryption: SizedSeq[N, GroupInt.Element] = tallyResult.candidates
        .map((c1, c2) => c1 ^ secKeyShare.x)
      val proof = partialDecryption
        .zip(tallyResult.candidates)
        .map { case (k, (c1, _)) =>
          ProofOfKnowledge.proveLog(
            domain = domain,
            base = c1,
            h = k,
            x = secKeyShare.x,
            random.on(domain),
            Hash.onDomain(domain)
          )
        }
      PartialResult(partialDecryption, proof)

  override val publishPartialResult
      : (TrusteeId, PartialResult[N], BulletinBoard[T, V, N]) => BulletinBoard[T, V, N] =
    (trusteeId: TrusteeId, newPartialResult: PartialResult[N], bb: BulletinBoard[T, V, N]) =>
      bb.copy(
        partialResults = bb.partialResults.zipWithIndex.map((partialResult, i) =>
          if (i == trusteeId.id) Some(newPartialResult)
          else partialResult
        )
      )

  override val verifyPartialResults: BulletinBoard[T, V, N] => Option[Boolean] =
    (bb: BulletinBoard[T, V, N]) =>
      combineBallots(bb).flatMap(tallyResult =>
        bb.partialResults.foldLeft(Some(true): Option[Boolean]) {
          case (None, _) | (_, None) => None
          case (Some(arePreviousPartialsValid), Some(PartialResult(candidates, proofs))) =>
            Some(
              arePreviousPartialsValid &&
                tallyResult.candidates
                  .zip(candidates.zip(proofs))
                  .forall { case ((c1, _), (k, proof)) =>
                    ProofOfKnowledge.verifyLog(
                      proof,
                      domain,
                      base = c1,
                      h = k,
                      Hash.onDomain(domain)
                    )
                  }
            )
        }
      )

  override val combinePartialResults: BulletinBoard[T, V, N] => Option[VotingResult[N]] =
    (bb: BulletinBoard[T, V, N]) =>
      val numberOfVoters = bb.ballots.size
      val logTable: Map[GroupInt.Element, FieldInt.Element] =
        Range(0, numberOfVoters.n + 1)
          .map(domain.exponent(_))
          .map(a => (domain.generator(a), a))
          .toMap

      for {
        tallyResult <- combineBallots(bb)
        combinedPartials <- bb.partialResults.foldLeft(
          Some(
            SizedSeq.fill(numberOfCandidates, domain.base(1))
          ): Option[SizedSeq[N, GroupInt.Element]]
        ) {
          case (None, _) | (_, None) => None
          case (Some(acc), Some(PartialResult(candidates, _))) =>
            Some(acc.zip(candidates).map(_ * _))
        }
        decrypted = combinedPartials
          .zip(tallyResult.candidates)
          .map { case (k, (_, c2)) => c2 / k }
          .map(logTable(_))
      } yield VotingResult(decrypted.map(_.z))

object HeliosDistributed:
  case class TrusteeId(id: Int)
  case class VoterId(id: Int)
  case class SecretKeyShare(x: FieldInt.Element)
  case class PublicKeyShare(
      h: GroupInt.Element,
      proof: ProofOfLog[Int, FieldInt.Element, FieldInt, GroupInt.Element]
  )
  case class PublicKey(h: GroupInt.Element):
    def underlying(domain: DomainInt) =
      Helios.PublicKey(
        domain,
        h,
        ProofOfLog(domain.base(1), domain.exponent(0), domain.exponent(0)) // fake
      )

  case class BulletinBoard[T <: Nat, V <: Nat, N <: Nat](
      publicKeyShares: SizedSeq[T, Option[PublicKeyShare]],
      ballots: SizedSeq[V, Option[Ballot[N]]],
      partialResults: SizedSeq[T, Option[PartialResult[N]]]
  )
  object BulletinBoard:
    def empty[T <: Nat, V <: Nat, N <: Nat](t: T, v: V, n: N) =
      BulletinBoard[T, V, N](
        publicKeyShares = SizedSeq.fill(t, None),
        ballots = SizedSeq.fill(v, None),
        partialResults = SizedSeq.fill(t, None)
      )

  type Vote[N <: Nat]   = Helios.Vote[N]
  type Ballot[N <: Nat] = Helios.Ballot[N]

  case class TallyResult[N <: Nat](candidates: SizedSeq[N, (GroupInt.Element, GroupInt.Element)])

  case class PartialResult[N <: Nat](
      candidates: SizedSeq[N, GroupInt.Element],
      proofs: SizedSeq[N, ProofOfLog[Int, FieldInt.Element, FieldInt, GroupInt.Element]]
  )

  case class VotingResult[N <: Nat](candidates: SizedSeq[N, Int])
