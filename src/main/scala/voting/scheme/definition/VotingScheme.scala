package voting.scheme.definition

/** @tparam X
  *   secret info
  * @tparam Y
  *   public info
  * @tparam V
  *   vote
  * @tparam B
  *   ballot
  * @tparam BB
  *   bulletin board
  * @tparam TR
  *   tally result
  * @tparam VR
  *   voting result
  * @tparam D
  *   domain
  * @tparam R
  *   randomness
  */
trait VotingScheme[X, Y, V, B, BB, TR, VR, D, R]:
  val setup: (D, R) => (X, Y, BB)
  val vote: (V, Y, R) => B
  val cast: (B, Y, BB) => Either[BallotProcessingError, BB]
  val tally: (BB, Y) => TR
  val reveal: (BB, TR, X, R) => VR

trait BallotProcessingError:
  def message: String
