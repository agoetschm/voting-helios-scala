package crypto

import algebra.Domain
import algebra.DomainInt
import algebra.Field
import algebra.FieldInt
import algebra.Generator
import algebra.GeneratorInt
import algebra.Group
import algebra.GroupInt

class ElGamal[
    Z: Integral,
    E <: Field.Element[Z, E],
    F <: Field[Z, E, F],
    B <: Group.Element[Z, E, F, B],
    G <: Group[Z, E, F, B, G],
    Gen <: Generator[Z, E, F, B, G],
    D <: Domain[Z, E, F, B, G, Gen]
] extends EncryptionScheme[B, (B, B), (D, B), E, D, E]
    with HomomorphicEncryption[(B, B)]:

  override val gen =
    (domain: D, random: (() => E)) =>
      val secKey: E = random()
      val h: B      = domain.generator(secKey)
      val pubKey    = (domain, h)
      (pubKey, secKey)

  override val enc =
    (m: B, pubKey: (D, B), random: () => E) =>
      val (domain, h) = pubKey
      val r: E        = random()
      val shared      = h ^ r
      (domain.generator(r), (shared * m))

  override val combine =
    (c1: (B, B), c2: (B, B)) =>
      val (a1, b1) = c1
      val (a2, b2) = c2
      (a1 * a2, b1 * b2)

  override val dec =
    (c: (B, B), sk: E) =>
      val shared = c._1 ^ sk
      c._2 * shared.inv

object ElGamal:
  val onInt = new ElGamal[
    Int,
    FieldInt.Element,
    FieldInt,
    GroupInt.Element,
    GroupInt,
    GeneratorInt,
    DomainInt
  ]
