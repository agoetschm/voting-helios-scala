package demo

import demo.CommandTree.CommandNode
import demo.CommandTree.ParsedToken

sealed trait Error
object Error:
  final case class ParsingError(
      parsedTokens: Seq[ParsedToken[_]],
      remainingTokens: Seq[String],
      possibleNodes: Seq[CommandNode],
      message: Option[String] = None
  ) extends Error
  final case class ExecutionError(message: String) extends Error
