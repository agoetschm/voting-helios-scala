package algebra

final case class GeneratorInt private[algebra] (g: GroupInt.Element)
    extends Generator[
      Int,
      FieldInt.Element,
      FieldInt,
      GroupInt.Element,
      GroupInt
    ]:

  override def apply(e: FieldInt.Element): GroupInt.Element = g ^ e

object GeneratorInt:
  def apply(g: GroupInt.Element, field: FieldInt): GeneratorInt =
    // check it generates the multiplicative group in the field or order q
    val generator = GeneratorInt(g)
    val exponent  = FieldInt(g.group.p)
    val orderOfG =
      Range(0, g.group.p).map(i => generator(exponent(i))).toSet.size
    require(
      orderOfG == field.order,
      s"the order [$orderOfG] of g=[$g] is not equal to the order [${field.order}] of the subgroup Zq*"
    )
    generator
