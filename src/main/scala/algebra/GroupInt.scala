package algebra

import algebra.GroupInt.Element

import scala.annotation.tailrec

final case class GroupInt(p: Int)
    extends Group[Int, FieldInt.Element, FieldInt, GroupInt.Element, GroupInt]:
  // TODO check that p is prime
  override val order: Int = p - 1
  override def apply(z: Int): GroupInt.Element =
    val mod = AlgebraIntImpl.nonNegativeModulo(z, p)
    require(mod != 0, s"integer [$mod] is equal to 0")
    GroupInt.Element(mod, this)

object GroupInt:
  import AlgebraIntImpl.*
  case class Element private[algebra] (z: Int, group: GroupInt)
      extends Group.Element[Int, FieldInt.Element, FieldInt, Element]:
    override def *(b: Element): Element = group(mult(z, b.z))
    override def inv: Element           = group(inverse(z, group.p))
    override def ^(e: FieldInt.Element): Element =
      group(modularExponentation(z, e.z, group.p))
