package algebra

final case class DomainInt(
    exponent: FieldInt,
    base: GroupInt,
    generator: GeneratorInt
) extends Domain[
      Int,
      FieldInt.Element,
      FieldInt,
      GroupInt.Element,
      GroupInt,
      GeneratorInt
    ]

object DomainInt:
  def apply(p: Int, q: Int, g: Int): DomainInt =
    val field     = FieldInt(q)
    val group     = GroupInt(p)
    val generator = GeneratorInt(group(g), field)
    DomainInt(exponent = field, base = group, generator = generator)
