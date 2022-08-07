import algebra.DomainInt
import crypto.Nat
import crypto.ProofOfKnowledge.ProofOfDisLog
import crypto.Random
import crypto.SizedSeq
import voting.attack.WeakFiatShamir
import voting.scheme.definition.BallotProcessingError
import voting.scheme.helios.Helios
import voting.scheme.helios.Helios._

// TODO use rerandomization to duplicate ballot
type N = Nat.N3.type // we have 3 candidates in this election
@main def demo() =
  val randomSource       = scala.util.Random(seed = 42)
  val numberOfVoters     = 10
  val numberOfCandidates = 3

  val votes: Seq[Vote[N]] = randomVotes(randomSource.nextInt, numberOfCandidates, numberOfVoters)
  val expectedResult: SizedSeq[N, Int] = countVotes(votes)
  printVotes(votes, expectedResult)

  val helios       = Helios[N]()
  val domain       = DomainInt(107, 53, 75)
  val random       = Random(() => randomSource.nextInt())
  val (sk, pk, bb) = helios.setup(domain, random)
  printSetup(domain, sk, pk, helios.verifyPublicKey)

  val ballots: Seq[Ballot[N]] =
    val actualBallots = votes.map(v => helios.vote(v, pk, random))
    val fakeBallot    = WeakFiatShamir.rerandomize(actualBallots.last, domain, pk)
    actualBallots :+ fakeBallot
  printBallots(ballots, helios.verifyBallot(_, pk))

  val maybeUpdatedBb: Either[BallotProcessingError, BulletinBoard[N]] =
    ballots.foldLeft(
      Right(bb): Either[BallotProcessingError, BulletinBoard[N]]
    ) { (accMaybeBb, ballot) =>
      accMaybeBb match
        case l @ Left(_) => l
        case Right(accBb) =>
          helios.processBallot(ballot, pk, accBb)
    }

  maybeUpdatedBb match
    case Left(e) => println(e.message)
    case Right(updatedBb) =>
      val tallyResult = helios.tally(updatedBb, pk)
      val result      = helios.reveal(updatedBb, tallyResult, sk, random)
      printResult(tallyResult, result, helios.verifyResult(_, _, pk))

def randomVotes(
    random: () => Int,
    numberOfCandidates: Int,
    numberOfVoters: Int
): Seq[Vote[Nat.N3.type]] =
  Seq
    .range(0, numberOfVoters)
    .map(_ =>
      Vote {
        (math.abs(random()) % numberOfCandidates) match
          case 0 => SizedSeq(Nat.N3, Seq(true, false, false))
          case 1 => SizedSeq(Nat.N3, Seq(false, true, false))
          case 2 => SizedSeq(Nat.N3, Seq(false, false, true))
      }
    )

def countVotes[N <: Nat](votes: Seq[Vote[N]]): SizedSeq[N, Int] =
  votes
    .map(_.candidates)
    .map(_.map(v => if (v) 1 else 0))
    .reduce((v1, v2) => v1.zip(v2).map(_ + _))

def printVotes(votes: Seq[Vote[N]], expectedResult: SizedSeq[N, Int]) =
  println("--------------------------------------------------")
  println("Random votes")
  votes.zipWithIndex.foreach((vote, i) =>
    println(f"${i}%2d:  ${vote.candidates.underlying.map(if (_) 1 else 0).mkString(" ")}")
  )
  println(s"Sum : ${expectedResult.underlying.map(s => f"$s%1d").mkString(" ")}")
def printSetup(
    domain: DomainInt,
    sk: SecretKey,
    pk: PublicKey,
    verification: PublicKey => Boolean
) =
  println("--------------------------------------------------")
  println("Setup")
  println("")
  println(s"Domain    : p=${domain.base.p} q=${domain.exponent.q} g=${domain.generator.g.z}")
  println(s"Secret key: x=${sk.x.z}")
  println(s"Public key: x=${pk.h.z}")
  println()
  println(
    s"Proof of key generation: (${pk.proofOfGen.commit.z}, ${pk.proofOfGen.challenge.z}, ${pk.proofOfGen.solution.z})"
  )
  println(s"Verification           : ${verification(pk)}")
def printBallots(ballots: Seq[Ballot[N]], verify: (Ballot[N]) => Boolean) =
  println("--------------------------------------------------")
  println("Encrypted ballots")
  ballots.zipWithIndex.foreach((ballot, i) =>
    println(
      f"${i}%2d:  ${ballot.candidates.underlying.map((a, b) => f"(${a.z}%3d,${b.z}%3d)").mkString(" ")}"
    )
  )
  println("Proofs of ballot encryption")
  ballots.zipWithIndex.foreach((ballot, i) =>
    println(f"${i}%2d:  ${ballot.proofOfEncryption._1.underlying
        .map(poe => poe.rounds.underlying.map(r => f"(${r.commit._1.z}%3d,${r.commit._2.z}%3d,${r.challenge.z}%2d,${r.solution.z}%2d)").mkString(" "))
        .mkString(" ")} | ${ballot.proofOfEncryption._2.rounds.underlying
        .map(r => f"(${r.commit._1.z}%3d,${r.commit._2.z}%3d,${r.challenge.z}%2d,${r.solution.z}%2d)")
        .mkString(" ")} | verification: ${verify(ballot)}")
  )
def printResult(
    tallyResult: TallyResult[N],
    result: VotingResult[N],
    verify: (TallyResult[N], VotingResult[N]) => Boolean
) =
  println("--------------------------------------------------")
  println("Tally result")
  println(
    f"${tallyResult.candidates.underlying.map((a, b) => f"(${a.z}%3d,${b.z}%3d)").mkString(" ")}"
  )
  println("--------------------------------------------------")
  println("Voting result")
  println(s"Sum : ${result.candidates.underlying.map(s => f"$s%1d").mkString(" ")}")
  println(
    s"Proof of decryption: ${result.proofOfDecryption.underlying
        .map(pod => f"(${pod.commit.z}%3d,${pod.challenge.z}%2d,${pod.solution.z}%2d)")
        .mkString(" ")}"
  )
  println(s"Verification       : ${verify(tallyResult, result)}")
  println("--------------------------------------------------")
