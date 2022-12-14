import _root_.demo.Cli
import algebra.DomainInt
import crypto.Nat
import crypto.ProofOfKnowledge.ProofOfDisLog
import crypto.Random
import crypto.SizedSeq
import voting.attack.WeakFiatShamir
import voting.scheme.BallotProcessingError
import voting.scheme.Helios
import voting.scheme.Helios._

@main def demo(nTrustees: Int, nVoters: Int, nCandidates: Int) =
  require(nTrustees > 0 && nTrustees <= 7)
  val t            = Nat.fromOrdinal(nTrustees)
  val trusteeNames = Seq("alice", "bob", "carol", "dave", "e", "f", "g").take(nTrustees)
  require(nVoters > 0 && nVoters < Nat.values.length)
  val v          = Nat.fromOrdinal(nVoters)
  val voterNames = (1 to nVoters).map(_.toString)
  require(nCandidates > 0 && nCandidates <= 4)
  val n              = Nat.fromOrdinal(nCandidates)
  val candidateNames = Seq("frank", "victoria", "ernest", "alba").take(nCandidates)
  Cli(t, SizedSeq(t, trusteeNames), v, SizedSeq(v, voterNames), n, SizedSeq(n, candidateNames)).run
