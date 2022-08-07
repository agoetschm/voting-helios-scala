package crypto

import algebra.DomainInt
import algebra.FieldInt
import algebra.GeneratorInt
import algebra.GroupInt

class ElGamalSuite extends munit.FunSuite:
  test("correctness") {
    val domain  = DomainInt(23, 11, 12)
    val elGamal = ElGamal.onInt

    val random   = () => domain.exponent(5)
    val (pk, sk) = elGamal.gen(domain, random)

    val m1 = domain.base(3)
    val c  = elGamal.enc(m1, pk, random)
    val m2 = elGamal.dec(c, sk)

    assertEquals(m1, m2)
  }

  test("homomorphicity") {
    val domain  = DomainInt(23, 11, 12)
    val elGamal = ElGamal.onInt

    val random   = () => domain.exponent(5)
    val (pk, sk) = elGamal.gen(domain, random)

    val m1       = domain.exponent(3)
    val c1       = elGamal.enc(domain.generator(m1), pk, random)
    val m2       = domain.exponent(5)
    val c2       = elGamal.enc(domain.generator(m2), pk, random)
    val combined = elGamal.combine(c1, c2)
    val m3       = elGamal.dec(combined, sk)

    assertEquals(domain.generator(m1 + m2), m3)
  }

  // TODO test that the distribution of ciphertext doesn't follow the one of the plaintext
