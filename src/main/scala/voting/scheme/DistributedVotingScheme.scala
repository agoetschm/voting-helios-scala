package voting.scheme

/** Voting scheme with multiple trustees handling the tallying. This scheme interface is more agent
  * oriented and each function is executable by either a voter, a trustee (voting process admin) or
  * anyone.
  */
trait DistributedVotingScheme[
    Random,
    SecretKeyShare,
    PublicKeyShare,
    PublicKey,
    BulletinBoard,
    Vote,
    Ballot,
    TallyResult,
    PartialResult,
    VotingResult,
    TrusteeId,
    VoterId
]:
  // voter
  val generateKeyShare: Random => (SecretKeyShare, PublicKeyShare)
  val publishKeyShare: (TrusteeId, PublicKeyShare, BulletinBoard) => BulletinBoard
  val revealPartialResult: (TallyResult, SecretKeyShare, Random) => PartialResult
  val publishPartialResult: (TrusteeId, PartialResult, BulletinBoard) => BulletinBoard
  // trustee
  val retrievePublicKey: BulletinBoard => Option[PublicKey]
  val prepareBallot: (Vote, PublicKey, Random) => Ballot
  val castBallot: (VoterId, Ballot, BulletinBoard) => BulletinBoard
  // anyone
  val verifyPublicKeyShares: BulletinBoard => Option[Boolean]
  val verifyBallots: BulletinBoard => Option[Boolean]
  val combineBallots: BulletinBoard => Option[TallyResult]
  val verifyPartialResults: BulletinBoard => Option[Boolean]
  val combinePartialResults: BulletinBoard => Option[VotingResult]
