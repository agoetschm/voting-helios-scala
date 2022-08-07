package voting.scheme.simple

import voting.scheme.definition.BallotProcessingError
import voting.scheme.definition.VotingScheme

type Vote          = Boolean
type Ballot        = Boolean
type BulletinBoard = Seq[Ballot]
type TallyResult   = Option[Boolean]
type VotingResult  = Option[Boolean]
object SimpleVoting
    extends VotingScheme[
      Unit,
      Unit,
      Vote,
      Ballot,
      BulletinBoard,
      TallyResult,
      VotingResult,
      Unit,
      Unit
    ]:
  override val setup: (Unit, Unit) => (Unit, Unit, BulletinBoard) =
    (_, _) => ((), (), Seq.empty)

  override val vote: (Vote, Unit, Unit) => Ballot =
    (vote: Vote, _, _) => vote

  override val processBallot
      : (Ballot, Unit, BulletinBoard) => Either[BallotProcessingError, BulletinBoard] =
    (ballot: Ballot, _, bulletinBoard: BulletinBoard) => Right(bulletinBoard :+ ballot)

  override val tally: (BulletinBoard, Unit) => TallyResult =
    (bulletinBoard: BulletinBoard, _) =>
      bulletinBoard.partition(_ == true) match
        case (t, f) if t.size > f.size => Some(true)
        case (t, f) if t.size < f.size => Some(false)
        case _                         => None

  override val reveal: (BulletinBoard, TallyResult, Unit, Unit) => VotingResult =
    (_, tallyResult: TallyResult, _, _) => tallyResult
