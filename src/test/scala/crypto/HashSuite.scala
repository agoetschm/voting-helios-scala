package crypto

import algebra.DomainInt

import scala.math.BigInt
import scala.math.pow
import scala.math.sqrt
import scala.util.Random

class HashSuite extends munit.FunSuite:
  test("distribution is close enough to uniform") {
    val p      = 107
    val q      = 53
    val domain = DomainInt(p, q, 75)
    val hash   = Hash.onDomain(domain)

    val n = 100000
    val hashed = Range(0, n)
      .map { _ =>
        val seq = Range(0, 10)
          .map(_ => Random.nextInt(p - 2) + 1) // no 0
          .map(domain.base(_))
        hash(seq)
      }
    val counts = hashed
      .groupBy(_.z)
      .mapValues(_.size)

    assertEquals(counts.size, q)

    val average  = n.toDouble / q
    val variance = counts.values.map(count => pow(count.toDouble - average, 2)).sum / q
    val expectedVar =
      n * (1.0 / q) * (1 - 1.0 / q) // variance for binomial distribution: n * p * (1-p)

    val errorMargin = 0.3
    assert((variance - expectedVar) / n <= errorMargin)
  }
