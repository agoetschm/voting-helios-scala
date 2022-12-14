package demo

import demo.Named.Candidate
import demo.Named.Trustee
import demo.Named.Voter
import voting.scheme.HeliosDistributed.TrusteeId
import voting.scheme.HeliosDistributed.VoterId

sealed trait ExecutableCommand
object ExecutableCommand:
  case object Help                                          extends ExecutableCommand
  case object Show                                          extends ExecutableCommand
  final case class ShowTrustee(trustee: Trustee)            extends ExecutableCommand
  final case class ShowVoter(voter: Voter[_])               extends ExecutableCommand
  final case class AddKeyShare(trusteeId: TrusteeId)        extends ExecutableCommand
  final case class Vote(voterId: VoterId, candidateId: Int) extends ExecutableCommand
  final case class PartialReveal(trusteeId: TrusteeId)      extends ExecutableCommand
