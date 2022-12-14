package demo

import demo.Named.Candidate
import demo.Named.Trustee
import demo.Named.Voter
import voting.scheme.Helios.Ballot
import voting.scheme.HeliosDistributed.PartialResult
import voting.scheme.HeliosDistributed.PublicKeyShare

import java.io.File
import java.io.PrintWriter

object Printer:
  def showHelp(state: State[_, _, _], commandTree: CommandTree): Unit =
    println(s"possible commands:")
    println(s"${commandTree.show}")
    println(s"trustees:")
    state.trustees.underlying.foreach(trustee => println(s"  ${trustee.name}"))
    println(s"voters:")
    state.voters.underlying.foreach(voter => println(s"  ${voter.name}"))
    println(s"candidates:")
    state.candidates.underlying.foreach(candidate => println(s"  ${candidate.name}"))

  def trusteeToString(trustee: Trustee): String =
    Seq(
      s"secret key share: ${trustee.secKeyShare.map(sks => f"${sks.x.z}%2d").getOrElse("")}"
    ).mkString("\n")

  def voterToString(voter: Voter[_]): String =
    Seq(
      s"vote: ${voter.vote.map(_.underlying.map(c => if (c) "1" else "0").mkString(" ")).getOrElse("")}"
    ).mkString("\n")

  def stateToString(state: State[_, _, _]): String =
    Seq(
      s"domain            : p=${state.domain.base.p} q=${state.domain.exponent.q} g=${state.domain.generator.g.z}",
      s"candidates        : ${state.candidates.underlying.map(_.name).mkString(", ")}",
      s"phase             : ${state.phase.toString}",
      s"public key shares",
      state.bulletinBoard.publicKeyShares.underlying
        .zip(state.trustees.underlying)
        .map((pubKeyShare, trustee) =>
          f"${trustee.name}%18s: ${pubKeyShare.map(s => f"${s.h.z}%3d\n${" " * 20}proof: " + pubKeyShareProofToString(s)).getOrElse("")}"
        )
        .mkString("\n"),
      s"ballots",
      state.bulletinBoard.ballots.underlying
        .zip(state.voters.underlying)
        .map((ballot, voter) =>
          f"${voter.name}%18s: ${ballot
              .map(b => s"${ballotToString(b)}\n${" " * 20}proof: ${ballotProofToString(b)}")
              .getOrElse("")}"
        )
        .mkString("\n"),
      s"partial results",
      state.bulletinBoard.partialResults.underlying
        .zip(state.trustees.underlying)
        .map((partialResult, trustee) =>
          f"${trustee.name}%18s: ${partialResult
              .map(p => s"${partialToString(p)}\n${" " * 20}proof: ${partialProofToString(p)}")
              .getOrElse("")}"
        )
        .mkString("\n")
    ).mkString("\n")

  def toFile(state: State[_, _, _], filename: String): Unit =
    val writer = new PrintWriter(new File(filename))
    writer.printf(stateToString(state))
    writer.close()

  private def pubKeyShareProofToString(pks: PublicKeyShare): String =
    f"(${pks.proof.commit.z}%3d ${pks.proof.challenge.z}%2d ${pks.proof.solution.z}%2d)"
  private def ballotToString(b: Ballot[_]): String =
    b.candidates.underlying.map((a, b) => f"(${a.z}%3d,${b.z}%3d)").mkString("  ")
  private def ballotProofToString(b: Ballot[_]): String =
    val bitProofs =
      b.proofOfEncryption._1.underlying
        .map(
          _.rounds
            .map(r =>
              f"(${r.commit._1.z}%3d ${r.commit._2.z}%3d ${r.challenge.z}%2d ${r.solution.z}%2d)"
            )
            .underlying
            .mkString(" ")
        )
        .mkString(" | ")
    val sumProof = b.proofOfEncryption._2.rounds.underlying
      .map(r => f"(${r.commit._1.z}%3d ${r.commit._2.z}%3d ${r.challenge.z}%2d ${r.solution.z}%2d)")
      .mkString(" ")

    s"$bitProofs || $sumProof"
  private def partialToString(p: PartialResult[_]): String =
    s"(${p.candidates.map(c => f"${c.z}%3d").underlying.mkString(",")})"
  private def partialProofToString(p: PartialResult[_]): String =
    s"${p.proofs.underlying.map(pr => f"(${pr.commit.z}%3d ${pr.challenge.z}%2d ${pr.solution.z}%2d)").mkString(" | ")}"
