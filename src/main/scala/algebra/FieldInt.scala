package algebra

import algebra.Field.Element

final case class FieldInt(q: Int) extends Field[Int, FieldInt.Element, FieldInt]:
  override val order = q
  override def apply(z: Int): FieldInt.Element =
    FieldInt.Element(AlgebraIntImpl.nonNegativeModulo(z, q), this)

object FieldInt:
  import AlgebraIntImpl.*
  case class Element private[algebra] (z: Int, field: FieldInt) extends Field.Element[Int, Element]:
    override def *(b: Element): Element = field(mult(z, b.z))
    override def +(b: Element): Element = field(add(z, b.z))
    override def neg: Element           = field(negation(z))
