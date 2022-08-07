package crypto

import algebra.Domain
import algebra.DomainInt
import algebra.Field
import algebra.FieldInt
import algebra.Generator
import algebra.GeneratorInt
import algebra.Group
import algebra.GroupInt
import crypto.Nat.N2
import crypto.ProofOfKnowledge.ProofOfDisLog
import crypto.ProofOfKnowledge.ProofOfLog

class ElGamalPoK[
    N <: Nat, // number of elements in a plaintext
    Z: Integral,
    E <: Field.Element[Z, E],
    F <: Field[Z, E, F],
    B <: Group.Element[Z, E, F, B],
    G <: Group[Z, E, F, B, G],
    Gen <: Generator[Z, E, F, B, G],
    D <: Domain[Z, E, F, B, G, Gen]
]() extends ProvableEncryption[
      SizedSeq[N, (E, E)],
      SizedSeq[N, (B, B)],
      SizedSeq[N, E],
      (D, B),
      (D, E),
      E,
      Seq[B] => E,
      ProofOfLog[Z, E, F, B],
      (
          SizedSeq[N, ProofOfKnowledge.ProofOfDisLog[Z, E, F, B]],
          ProofOfKnowledge.ProofOfDisLog[Z, E, F, B]
      ),
      SizedSeq[N, ProofOfLog[Z, E, F, B]]
    ]:

  override val proveGen =
    (pubKey: (D, B), secKey: (D, E), random: () => E, hash: Seq[B] => E) =>
      val (domain, h) = pubKey
      val (_, x)      = secKey
      ProofOfKnowledge.proveLog(
        domain,
        base = domain.generator.g,
        h,
        x,
        random,
        hash
      )

  override val verifyGen =
    (pubKey: (D, B), proofOfGen: ProofOfLog[Z, E, F, B], hash: Seq[B] => E) =>
      val (domain, h) = pubKey
      ProofOfKnowledge.verifyLog(
        proofOfGen,
        domain,
        domain.generator.g,
        h,
        hash
      )

  override val proveEnc =
    (plaintextsWithRandom, pubKey, ciphertexts, random, hash: Seq[B] => E) =>
      val (domain, h) = pubKey
      val proofSingleEnc = plaintextsWithRandom
        .zip(ciphertexts)
        .map((ptWithRandom, ciphertext) =>
          val (m, r) = ptWithRandom
          val (a, b) = ciphertext
          ProofOfKnowledge.proofOfDisjunctiveLogEquality(
            exponent = domain.exponent,
            g = domain.generator,
            a = a,
            b = b,
            h = h,
            r = r,
            m = m,
            random = random,
            hash
          )
        )
      val proofSumEnc = {
        val (sumPlaintexts, sumR) = plaintextsWithRandom.reduce { case ((pt1, r1), (pt2, r2)) =>
          (pt1 + pt2, r1 + r2)
        }
        val (productA, productB) = ciphertexts.reduce { case ((a1, b1), (a2, b2)) =>
          (a1 * a2, b1 * b2)
        }
        ProofOfKnowledge.proofOfDisjunctiveLogEquality(
          exponent = domain.exponent,
          g = domain.generator,
          a = productA,
          b = productB,
          h = h,
          r = sumR,
          m = sumPlaintexts,
          random = random,
          hash
        )
      }
      (proofSingleEnc, proofSumEnc)

  override val verifyEnc =
    (
        ciphertexts: SizedSeq[N, (B, B)],
        pubKey: (D, B),
        proofOfEnc: (
            SizedSeq[N, ProofOfDisLog[Z, E, F, B]],
            ProofOfDisLog[Z, E, F, B]
        ),
        hash: Seq[B] => E
    ) =>
      val (domain, h)                   = pubKey
      val (proofSingleEnc, proofSumEnc) = proofOfEnc
      val singleCiphertextsValid = ciphertexts
        .zip(proofSingleEnc)
        .map((ciphertext, proofOfElementEncryption) =>
          val (a, b) = ciphertext
          ProofOfKnowledge.verifyDisjunctiveLogEquality(
            domain.exponent,
            domain.generator,
            a,
            b,
            h,
            proofOfElementEncryption,
            hash
          )
        )
        .reduce(_ && _)
      val productCiphertextsValid = {
        val (productA, productB) = ciphertexts.reduce { case ((a1, b1), (a2, b2)) =>
          (a1 * a2, b1 * b2)
        }
        ProofOfKnowledge.verifyDisjunctiveLogEquality(
          domain.exponent,
          domain.generator,
          a = productA,
          b = productB,
          h,
          proofSumEnc,
          hash
        )
      }
      singleCiphertextsValid && productCiphertextsValid

  override val proveDec =
    (
        ciphertext: SizedSeq[N, ((B, B))],
        secKey: (D, E),
        decrypted: SizedSeq[N, E],
        random: () => E,
        hash: Seq[B] => E
    ) =>
      val (domain, x) = secKey
      ciphertext
        .zip(decrypted)
        .map((ct, dt) =>
          val (a, b) = ct
          ProofOfKnowledge.proveLog(
            domain = domain,
            base = a,
            h = b / domain.generator(dt),
            x = x,
            random,
            hash
          )
        )

  override val verifyDec =
    (
        ciphertext: SizedSeq[N, (B, B)],
        decrypted: SizedSeq[N, E],
        pubKey: (D, B),
        proofOfDec: SizedSeq[N, ProofOfLog[Z, E, F, B]],
        hash: Seq[B] => E
    ) =>
      val (domain, _) = pubKey
      proofOfDec
        .zip(ciphertext)
        .zip(decrypted)
        .map { case ((proofOfLog, ct), dt) =>
          val (a, b) = ct
          ProofOfKnowledge.verifyLog(
            proofOfLog,
            domain,
            base = a,
            h = b / domain.generator(dt),
            hash
          )
        }
        .reduce(_ && _)

object ElGamalPoK:
  def onInt[N <: Nat] = new ElGamalPoK[
    N,
    Int,
    FieldInt.Element,
    FieldInt,
    GroupInt.Element,
    GroupInt,
    GeneratorInt,
    DomainInt
  ]
