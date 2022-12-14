package demo

import algebra.DomainInt
import algebra.FieldInt
import algebra.GroupInt
import crypto.Nat
import crypto.ProofOfKnowledge.ProofOfLog
import crypto.Random
import crypto.SizedSeq
import demo.CommandTree.CommandBranch
import demo.CommandTree.CommandLeaf
import demo.CommandTree.CommandNode
import demo.CommandTree.ParsedToken
import demo.CommandTree.Token
import demo.Error.ExecutionError
import demo.Error.ParsingError
import demo.Named.Candidate
import demo.Named.Trustee
import demo.Named.Voter
import voting.scheme.Helios
import voting.scheme.HeliosDistributed
import voting.scheme.HeliosDistributed.*

import java.io.File
import java.io.PrintWriter
import scala.annotation.tailrec
import scala.io.StdIn.readLine

import annotation.tailrec

class Cli[T <: Nat, V <: Nat, N <: Nat](
    nTrustees: T,
    trusteeNames: SizedSeq[T, String],
    nVoters: V,
    voterNames: SizedSeq[V, String],
    nCandidates: N,
    candidateName: SizedSeq[N, String]
):
  val domain       = DomainInt(107, 53, 75)
  val helios       = HeliosDistributed[T, V, N](domain, nCandidates)
  val randomSource = scala.util.Random()
  val random       = Random(() => randomSource.nextInt())

  def run =
    val initialState = State(
      domain,
      State.Phase.Initial,
      BulletinBoard.empty(nTrustees, nVoters, nCandidates),
      trustees = trusteeNames.zipWithIndex.map((name, id) => Trustee(name, TrusteeId(id), None)),
      voters = voterNames.zipWithIndex.map((name, id) => Voter(name, VoterId(id), None)),
      candidates = candidateName.zipWithIndex.map((name, id) => Candidate(name, id))
    )
    println()
    println(
      s"running demo voting process with ${nTrustees.n} trustees, ${nVoters.n} voters and ${nCandidates.n} candidates"
    )
    println("type 'help' for indications")
    println()
    val finalState = iter(initialState)
    helios.combinePartialResults(finalState.bulletinBoard) match
      case Some(result) =>
        println()
        println(s"final result: (${result.candidates.underlying.map(c => f"$c%2d").mkString(" ")})")
        println()
      case None =>
        println("no final result")

  @tailrec private def iter(state: State[T, V, N]): State[T, V, N] =
    Printer.toFile(state, "state.txt")
    if (state.phase == State.Phase.Finished) state
    else
      print("> ")
      val line        = readLine().strip()
      val commandTree = possibleCommands(state)
      (for {
        command   <- Parser.parse(state, line, commandTree)
        nextState <- executeCommand(command, state, commandTree)
      } yield nextState) match
        case Left(ParsingError(parsedTokens, remainingTokens, possibleNodes, message)) =>
          if !(parsedTokens.isEmpty && (remainingTokens.isEmpty || remainingTokens.forall(
              _.isEmpty
            )))
          then
            println(s"failed to parse${message.map(m => s": $m").getOrElse("")}")
            remainingTokens match
              case Seq() =>
                ()
              case failedToken +: rest =>
                println(s"valid start        : ${parsedTokens.map(_.show).mkString(" ")}")
                println(s"invalid word       : $failedToken")
            println(s"possible next words: ${possibleNodes.map(_.token.show).mkString(", ")}")
            Printer.showHelp(state, commandTree)
          iter(state)
        case Left(ExecutionError(message)) =>
          println(s"execution error: $message")
          iter(state)
        case Right(nextState) =>
          iter(nextState.copy(phase = nextPhase(nextState)))

  private def possibleCommands(state: State[T, V, N]): CommandTree =
    val trusteeInput = Token.InputToken(state.trustees.underlying, "trustee")
    val voterInput   = Token.InputToken(state.voters.underlying, "voter")
    val commands = state.phase match
      case State.Phase.Initial =>
        Seq(
          CommandBranch(
            trusteeInput,
            children = Seq(CommandLeaf(Token.AddKeyShare), CommandLeaf(Token.Show))
          ),
          CommandBranch(voterInput, children = Seq(CommandLeaf(Token.Show)))
        )
      case State.Phase.Voting =>
        val candidateInput = Token.InputToken(state.candidates.underlying, "candidate")
        Seq(
          CommandBranch(
            voterInput,
            Seq(
              CommandBranch(Token.Vote, Seq(CommandLeaf(candidateInput))),
              CommandLeaf(Token.Show)
            )
          ),
          CommandBranch(
            trusteeInput,
            children = Seq(CommandLeaf(Token.Show))
          )
        )
      case State.Phase.Tallying =>
        Seq(
          CommandBranch(
            trusteeInput,
            children = Seq(CommandLeaf(Token.PartialReveal), CommandLeaf(Token.Show))
          ),
          CommandBranch(voterInput, children = Seq(CommandLeaf(Token.Show)))
        )
      case State.Phase.Finished => Seq()
    CommandTree(commands ++ Seq(CommandLeaf(Token.Show), CommandLeaf(Token.Help)))

  private def nextPhase(state: State[T, V, N]): State.Phase =
    import State.Phase
    val (shouldSwitch, next): (Boolean, Phase) = state.phase match
      case Phase.Initial => (state.bulletinBoard.publicKeyShares.forall(_.isDefined), Phase.Voting)
      case Phase.Voting  => (state.bulletinBoard.ballots.forall(_.isDefined), Phase.Tallying)
      case Phase.Tallying =>
        (state.bulletinBoard.partialResults.forall(_.isDefined), Phase.Finished)
      case Phase.Finished => (false, Phase.Finished)
    if (shouldSwitch) next
    else state.phase

  private def executeCommand(
      command: ExecutableCommand,
      state: State[T, V, N],
      commandTree: CommandTree
  ): Either[ExecutionError, State[T, V, N]] =
    import ExecutableCommand.*
    command match
      case AddKeyShare(trusteeId) =>
        val (secKeyShare, pubKeyShare) = helios.generateKeyShare(random)
        val nextState = state.copy(
          bulletinBoard = helios.publishKeyShare(trusteeId, pubKeyShare, state.bulletinBoard),
          trustees = state.trustees.map {
            case trustee if trustee.id == trusteeId => trustee.copy(secKeyShare = Some(secKeyShare))
            case t                                  => t
          }
        )
        println("public key share added")
        Right(nextState)
      case Vote(voterId, candidateId) =>
        val candidatesBitsString =
          SizedSeq
            .fill(nCandidates, ())
            .zipWithIndex
            .map((_, i) => if (i == candidateId) true else false)
        val maybePubKey = helios.retrievePublicKey(state.bulletinBoard)
        maybePubKey.fold(
          Left(ExecutionError("missing public key"))
        ) { pubKey =>
          val ballot = helios.prepareBallot(Helios.Vote(candidatesBitsString), pubKey, random)
          val nextBb = helios.castBallot(voterId, ballot, state.bulletinBoard)
          val nextState = state.copy(
            bulletinBoard = nextBb,
            voters = state.voters.map {
              case voter if voter.id == voterId => voter.copy(vote = Some(candidatesBitsString))
              case v                            => v
            }
          )
          println("ballot cast")
          Right(nextState)
        }
      case PartialReveal(trusteeId) =>
        (for {
          tallyResult <- helios.combineBallots(state.bulletinBoard)
          trustee     <- state.trustees.underlying.find(_.id == trusteeId)
          secKeyShare <- trustee.secKeyShare
          partialResult = helios.revealPartialResult(tallyResult, secKeyShare, random)
          updatedBB     = helios.publishPartialResult(trusteeId, partialResult, state.bulletinBoard)
        } yield {
          updatedBB
        }) match
          case Some(updatedBB) =>
            println("revealed partial result")
            Right(
              state.copy(
                bulletinBoard = updatedBB
              )
            )
          case None => Left(ExecutionError("failed to reveal partial result"))
      case ShowTrustee(trustee) =>
        println(Printer.trusteeToString(trustee))
        Right(state)
      case ShowVoter(voter) =>
        println(Printer.voterToString(voter))
        Right(state)
      case Show =>
        println(Printer.stateToString(state))
        Right(state)
      case Help =>
        Printer.showHelp(state, commandTree)
        Right(state)
