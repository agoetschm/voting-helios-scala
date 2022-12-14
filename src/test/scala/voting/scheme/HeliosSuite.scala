package voting.scheme

import algebra.DomainInt
import crypto.Nat
import crypto.Random
import crypto.SizedSeq
import voting.scheme.BallotProcessingError

import Helios._

class HeliosSuite extends munit.FunSuite:
  test("voting session is successful") {
    val randomSource       = scala.util.Random(seed = 42)
    val numberOfVoters     = 40
    val numberOfCandidates = 3
    type N = Nat.N3.type
    val votes: Seq[Vote[N]] =
      Seq
        .range(0, numberOfVoters)
        .map(_ =>
          Vote {
            (math.abs(randomSource.nextInt()) % numberOfCandidates) match
              case 0 => SizedSeq(Nat.N3, Seq(true, false, false))
              case 1 => SizedSeq(Nat.N3, Seq(false, true, false))
              case 2 => SizedSeq(Nat.N3, Seq(false, false, true))
          }
        )

    val domain       = DomainInt(107, 53, 75)
    val helios       = Helios[N]()
    val random       = Random(() => randomSource.nextInt())
    val (sk, pk, bb) = helios.setup(domain, random)
    assert(helios.verifyPublicKey(pk))

    val ballots: Seq[Ballot[N]] = votes.map(v => helios.vote(v, pk, random))
    assert(ballots.forall(ballot => helios.verifyBallot(ballot, pk)))

    val maybeUpdatedBb: Either[BallotProcessingError, BulletinBoard[N]] =
      ballots.foldLeft(
        Right(bb): Either[BallotProcessingError, BulletinBoard[N]]
      ) { (accMaybeBb, ballot) =>
        accMaybeBb match
          case l @ Left(_) => l
          case Right(accBb) =>
            helios.cast(ballot, pk, accBb)
      }

    maybeUpdatedBb match
      case Left(e) => fail(e.message)
      case Right(updatedBb) =>
        val expectedResult: SizedSeq[N, Int] = votes
          .map(_.candidates)
          .map(_.map(v => if (v) 1 else 0))
          .reduce((v1, v2) => v1.zip(v2).map(_ + _))
        val tallyResult = helios.tally(updatedBb, pk)
        val result      = helios.reveal(updatedBb, tallyResult, sk, random)
        assertEquals(result.candidates, expectedResult)
        assert(helios.verifyResult(tallyResult, result, pk))
  }
