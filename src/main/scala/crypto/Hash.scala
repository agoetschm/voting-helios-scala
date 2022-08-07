package crypto

import algebra.Domain
import algebra.Field
import algebra.Generator
import algebra.Group

import scala.reflect.ClassTag

import math.Integral.Implicits.infixIntegralOps

// TODO test
// maybe check distribution and correlation between in/out
// TODO something looks off: a lot of 1's as output
object Hash:
  val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
  def onDomain[
      Z: Integral,
      E <: Field.Element[Z, E],
      F <: Field[Z, E, F],
      B <: Group.Element[Z, E, F, B],
      G <: Group[Z, E, F, B, G],
      Gen <: Generator[Z, E, F, B, G],
      D <: Domain[Z, E, F, B, G, Gen]
  ](domain: D)(using c: ClassTag[Z]): Seq[B] => E = // TODO why is classtag needed?
    (in: Seq[B]) =>
      val inBytes = in.map(_.z.toInt.toByte)
      val digested = messageDigest
        .digest(inBytes.toArray)
        .map(byte => Integral[Z].fromInt(byte.toInt))
        .reduce(_ + _)
      domain.exponent(digested)
