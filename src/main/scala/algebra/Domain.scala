package algebra

object Field:
  trait Element[Z: Integral, E <: Element[Z, E]]:
    val field: Field[Z, E, ?]
    val z: Z
    def *(b: E): E
    def +(b: E): E
    def neg: E
    def -(b: E): E = this.+(b.neg)

trait Field[Z: Integral, E <: Field.Element[Z, E], F <: Field[Z, E, F]]:
  val order: Z
  def apply(z: Z): E

object Group:
  trait Element[
      Z: Integral,
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Element[Z, E, F, B]
  ]:
    val group: Group[Z, E, F, B, ?]
    val z: Z
    def *(b: B): B
    def inv: B
    def ^(e: E): B
    def /(b: B): B = *(b.inv)
trait Group[
    Z: Integral,
    E <: Field.Element[Z, E],
    F <: Field[Z, E, F],
    B <: Group.Element[Z, E, F, B],
    G <: Group[Z, E, F, B, G]
]:
  val order: Z
  def apply(z: Z): B

trait Generator[
    Z: Integral,
    E <: Field.Element[Z, E],
    F <: Field[Z, E, F],
    B <: Group.Element[Z, E, F, B],
    G <: Group[Z, E, F, B, G]
]:
  def apply(e: E): B
  def g: B

trait Domain[
    Z: Integral,
    E <: Field.Element[Z, E],
    F <: Field[Z, E, F],
    B <: Group.Element[Z, E, F, B],
    G <: Group[Z, E, F, B, G],
    Gen <: Generator[Z, E, F, B, G]
]:
  val base: G
  val exponent: F
  val generator: Gen
