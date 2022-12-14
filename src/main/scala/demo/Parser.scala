package demo

import demo.CommandTree.CommandBranch
import demo.CommandTree.CommandLeaf
import demo.CommandTree.CommandNode
import demo.CommandTree.ParsedToken
import demo.CommandTree.Token
import demo.Error.ParsingError
import demo.Named.Candidate
import demo.Named.Trustee
import demo.Named.Voter

object Parser:
  def parse(
      state: State[_, _, _],
      line: String,
      commandTree: CommandTree
  ): Either[ParsingError, ExecutableCommand] =
    val tokens       = line.split(" ")
    val parsedTokens = parse0(Seq(), tokens, commandTree.trunk)
    parsedTokens.flatMap {
      case Seq(Token.ParsedInputToken(trustee: Trustee), Token.AddKeyShare) =>
        Right(ExecutableCommand.AddKeyShare(trustee.id))
      case Seq(
            Token.ParsedInputToken(voter: Voter[_]),
            Token.Vote,
            Token.ParsedInputToken(candidate: Candidate)
          ) =>
        Right(ExecutableCommand.Vote(voter.id, candidate.id))
      case Seq(Token.ParsedInputToken(trustee: Trustee), Token.PartialReveal) =>
        Right(ExecutableCommand.PartialReveal(trustee.id))
      case Seq(Token.ParsedInputToken(trustee: Trustee), Token.Show) =>
        Right(ExecutableCommand.ShowTrustee(trustee))
      case Seq(Token.ParsedInputToken(voter: Voter[_]), Token.Show) =>
        Right(ExecutableCommand.ShowVoter(voter))
      case Seq(Token.Show) => Right(ExecutableCommand.Show)
      case Seq(Token.Help) => Right(ExecutableCommand.Help)
      case _               => throw Exception("should not happen")
    }

  private def parse0(
      acc: Seq[ParsedToken[_]],
      remainingTokens: Seq[String],
      possibleNodes: Seq[CommandNode]
  ): Either[ParsingError, Seq[ParsedToken[_]]] =
    (remainingTokens, possibleNodes) match
      case (Seq(), Seq()) =>
        Right(acc)
      case (Seq(), _) =>
        Left(ParsingError(acc, Seq(), possibleNodes, Some("incomplete command")))
      case (token +: tail, _) =>
        val matchingToken: Option[(ParsedToken[_], CommandNode)] =
          possibleNodes.foldLeft(None: Option[(ParsedToken[_], CommandNode)]) {
            case (matching @ Some(_), _) => matching
            case (None, node) =>
              node.token.matches(token).map((_, node))
          }
        matchingToken match
          case None =>
            Left(ParsingError(acc, remainingTokens, possibleNodes))
          case Some((parsedToken, node: CommandLeaf)) =>
            parse0(acc :+ parsedToken, tail, Nil)
          case Some((parsedToken, node: CommandBranch)) =>
            parse0(acc :+ parsedToken, tail, node.children)
