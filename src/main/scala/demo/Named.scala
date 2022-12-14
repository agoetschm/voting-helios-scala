package demo

import crypto.Nat
import crypto.SizedSeq
import voting.scheme.HeliosDistributed.SecretKeyShare
import voting.scheme.HeliosDistributed.TrusteeId
import voting.scheme.HeliosDistributed.VoterId

trait Named:
  def name: String

object Named:
  sealed trait Agent extends Named
  final case class Voter[N <: Nat](name: String, id: VoterId, vote: Option[SizedSeq[N, Boolean]])
      extends Agent
  final case class Trustee(name: String, id: TrusteeId, secKeyShare: Option[SecretKeyShare])
      extends Agent

  final case class Candidate(name: String, id: Int) extends Named
