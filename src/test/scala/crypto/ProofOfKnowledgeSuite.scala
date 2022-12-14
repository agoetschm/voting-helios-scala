package crypto

import algebra.DomainInt
import algebra.GroupInt
import crypto.ProofOfKnowledge.ProofOfDisLog
import crypto.ProofOfKnowledge.ProofOfLog
import crypto.ProofOfKnowledge.Round

class ProofOfKnowledgeSuite extends munit.FunSuite:
  test("proveLog") {
    val domain = DomainInt(107, 53, 75)
    val pok = ProofOfKnowledge.proveLog(
      domain = domain,
      base = domain.base(7),
      h = domain.base(7),
      x = domain.exponent(1),
      random = () => domain.exponent(1),
      hash = _ => domain.exponent(1)
    )
    assertEquals(pok, ProofOfLog(domain.base(7), domain.exponent(1), domain.exponent(2)))
  }

  test("verifyLog") {
    val domain = DomainInt(107, 53, 75)
    val hash   = (_: Seq[GroupInt.Element]) => domain.exponent(44)
    val base   = domain.generator.g
    val x      = domain.exponent(3)
    val h      = base ^ x
    val pok = ProofOfKnowledge.proveLog(
      domain = domain,
      base = base,
      h = h,
      x = x,
      random = () => domain.exponent(42),
      hash = hash
    )
    assert(
      ProofOfKnowledge.verifyLog(
        pok,
        domain = domain,
        base = base,
        h = h,
        hash = hash
      )
    )
    assert(
      ProofOfKnowledge.verifyLog(
        pok,
        domain = domain,
        base = base ^ x, // wrong
        h = h,
        hash = hash
      ) == false
    )
  }

  test("verifyOfDisjunctiveLogEquality") {
    val domain = DomainInt(107, 53, 75)
    val pok = ProofOfKnowledge.proofOfDisjunctiveLogEquality(
      exponent = domain.exponent,
      g = domain.generator,
      a = domain.base(7),
      b = domain.base(7),
      h = domain.base(7),
      r = domain.exponent(1),
      m = domain.exponent(0),
      random = () => domain.exponent(1),
      hash = (_: Seq[GroupInt.Element]) => domain.exponent(1)
    )
    assert(
      ProofOfKnowledge.verifyDisjunctiveLogEquality(
        field = domain.exponent,
        g = domain.generator,
        a = domain.base(7),
        b = domain.base(7),
        h = domain.base(7),
        poe = pok,
        hash = (_: Seq[GroupInt.Element]) => domain.exponent(1)
      )
    )
  }
