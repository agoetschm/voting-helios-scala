package crypto

import algebra.Domain
import algebra.Field
import algebra.Generator
import algebra.Group

class Random[Z: Integral](val source: () => Z):
  def apply(): Z = source()
  def on[
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Group.Element[Z, E, F, B],
      G <: Group[Z, E, F, B, G],
      Gen <: Generator[Z, E, F, B, G],
      D <: Domain[Z, E, F, B, G, Gen]
  ](domain: D): () => E =
    () => domain.exponent(source())
