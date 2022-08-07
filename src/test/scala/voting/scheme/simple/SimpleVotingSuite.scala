package voting.scheme.simple

import voting.scheme.definition.BallotProcessingError

import scala.util.Random

class SimpleVotingSuite extends munit.FunSuite:
  test("run voting session") {
    val random         = Random(seed = 42)
    val numberOfVoters = 100
    val votes: Seq[Vote] =
      Seq.range(0, numberOfVoters).map(_ => random.nextBoolean())

    val (sk, pk, bb) = SimpleVoting.setup((), ())
    val ballots: Seq[Ballot] =
      votes.map(v => SimpleVoting.vote(v, pk, ()))
    val maybeUpdatedBb: Either[BallotProcessingError, BulletinBoard] =
      ballots.foldLeft(
        Right(bb): Either[BallotProcessingError, BulletinBoard]
      ) { (accMaybeBb, ballot) =>
        accMaybeBb match
          case l @ Left(_) => l
          case Right(accBb) =>
            SimpleVoting.processBallot(ballot, (), accBb)
      }

    maybeUpdatedBb match
      case Left(e) => fail(e.message)
      case Right(updatedBb) =>
        val expectedResult = votes
          .map(vote => if (vote) 1 else 0)
          .sum
          .toDouble
          ./(numberOfVoters) match
          case x if x > 0.5 => Some(true)
          case x if x < 0.5 => Some(false)
          case _            => None
        val tallyResult = SimpleVoting.tally(updatedBb, pk)
        val result      = SimpleVoting.reveal(bb, tallyResult, sk, ())
        assertEquals(result, expectedResult)
  }
