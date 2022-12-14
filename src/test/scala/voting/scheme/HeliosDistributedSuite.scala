package voting.scheme

import algebra.DomainInt
import crypto.Nat
import crypto.Nat.N3
import crypto.Nat.N4
import crypto.Nat.N9
import crypto.Random
import crypto.SizedSeq
import voting.scheme.BallotProcessingError
import voting.scheme.HeliosDistributed._

class HeliosDistributedSuite extends munit.FunSuite:
  test("voting session is successful") {
    val randomSource = scala.util.Random(seed = 47)
    type V = Nat.N9.type
    val nVoters: V = Nat.N9
    type N = Nat.N3.type
    val nCandidates: N = Nat.N3
    type T = Nat.N4.type
    val nTrustees: T = Nat.N4
    val votes: Seq[Vote[N]] =
      Seq
        .range(0, nVoters.n)
        .map(_ =>
          Helios.Vote {
            (math.abs(randomSource.nextInt()) % nCandidates.n) match
              case 0 => SizedSeq(nCandidates, Seq(true, false, false))
              case 1 => SizedSeq(nCandidates, Seq(false, true, false))
              case 2 => SizedSeq(nCandidates, Seq(false, false, true))
          }
        )

    val domain = DomainInt(107, 53, 75)
    val helios = HeliosDistributed[T, V, N](domain, nCandidates)

    val random             = Random(() => randomSource.nextInt())
    val emptyBulletinBoard = BulletinBoard.empty(nTrustees, nVoters, nCandidates)

    val trusteeKeyPairs = (0 until nTrustees.n).map(i => (i, helios.generateKeyShare(random)))
    val bbWithPubKeyShares = trusteeKeyPairs.foldLeft(emptyBulletinBoard) {
      case (bb, (trustee, (_, pubKeyShare))) =>
        helios.publishKeyShare(TrusteeId(trustee), pubKeyShare, bb)
    }
    assertEquals(
      helios.verifyPublicKeyShares(bbWithPubKeyShares),
      Some(true),
      "public key shares are all valid"
    )

    val pubKey = helios.retrievePublicKey(bbWithPubKeyShares).get

    val bbWithBallots: BulletinBoard[T, V, N] = votes.zipWithIndex.foldLeft(bbWithPubKeyShares) {
      case (bb, (vote, i)) =>
        val ballot = helios.prepareBallot(vote, pubKey, random)
        helios.castBallot(VoterId(i), ballot, bb)
    }
    assertEquals(
      helios.verifyBallots(bbWithBallots),
      Some(true),
      "ballots are all valid"
    )

    val tallyResult = helios.combineBallots(bbWithBallots).get
    val bbWithPartialResults = trusteeKeyPairs.foldLeft(bbWithBallots) {
      case (bb, (i, (secKeyShare, _))) =>
        val partialResult = helios.revealPartialResult(tallyResult, secKeyShare, random)
        helios.publishPartialResult(TrusteeId(i), partialResult, bb)
    }
    assertEquals(
      helios.verifyPartialResults(bbWithPartialResults),
      Some(true),
      "partials results are all valid"
    )

    val finalResult = helios.combinePartialResults(bbWithPartialResults).get
    val expectedResult: SizedSeq[N, Int] = votes
      .map(_.candidates)
      .map(_.map(v => if (v) 1 else 0))
      .reduce((v1, v2) => v1.zip(v2).map(_ + _))
    assertEquals(finalResult.candidates, expectedResult, "voting result is correct")
  }
