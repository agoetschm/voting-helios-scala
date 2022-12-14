package demo

import demo.CommandTree.CommandBranch
import demo.CommandTree.CommandLeaf
import demo.CommandTree.CommandNode

case class CommandTree(trunk: Seq[CommandNode]):
  def show: String =
    trunk
      .map(show0(Seq(), _))
      .mkString("\n")

  private def show0(acc: Seq[String], node: CommandNode): String =
    node match
      case CommandLeaf(token) => s"  ${(acc :+ token.show).mkString(" ")}"
      case CommandBranch(token, children) =>
        children.map(c => show0(acc :+ token.show, c)).mkString("\n")

object CommandTree:
  sealed trait CommandNode:
    val token: Token[_]
  final case class CommandBranch(token: Token[_], children: Seq[CommandNode]) extends CommandNode
  final case class CommandLeaf(token: Token[_])                               extends CommandNode

  sealed trait Token[A]:
    def matches(tkn: String): Option[ParsedToken[A]]
    def show: String
  sealed trait ParsedToken[A]:
    def value: A
    def show: String
  object Token:
    sealed trait ConstantToken(val value: String) extends Token[String] with ParsedToken[String]:
      override def matches(tkn: String): Option[ConstantToken] = Some(this).filter(_.value == tkn)
      override def show: String                                = value
    case object Help          extends ConstantToken("help")
    case object Setup         extends ConstantToken("setup")
    case object Show          extends ConstantToken("show")
    case object AddKeyShare   extends ConstantToken("addKeyShare")
    case object Vote          extends ConstantToken("vote")
    case object PartialReveal extends ConstantToken("partialReveal")

    case class InputToken[A <: Named](possibleValues: Seq[A], name: String) extends Token[A]:
      override def matches(tkn: String): Option[ParsedInputToken[A]] =
        possibleValues.find(_.name == tkn).map(ParsedInputToken.apply)
      override def show: String = s"<$name>"
    case class ParsedInputToken[A <: Named](value: A) extends ParsedToken[A]:
      override def show: String = value.name
